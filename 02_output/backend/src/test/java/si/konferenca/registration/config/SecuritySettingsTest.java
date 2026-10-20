package si.konferenca.registration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.cors.CorsConfiguration;

/** ProductionGuard (SR-02), OrganizerAccess (SR-06), AppProperties and the CORS source. */
class SecuritySettingsTest {

  static AppProperties properties(
      String environment, boolean testMode, String siteKey, boolean httpsOnly, String origin) {
    return new AppProperties(
        environment,
        "classpath:conference.json",
        "./data",
        new AppProperties.Mail("from@x.org"),
        new AppProperties.Organizer(List.of("o@x.org"), "u", "p", httpsOnly),
        new AppProperties.Captcha(testMode, siteKey, "secret", "https://verify"),
        new AppProperties.Cors(origin),
        new AppProperties.RateLimit(60),
        new AppProperties.Request(16384));
  }

  static MockEnvironment explicitSettings() {
    return new MockEnvironment()
        .withProperty("DB_URL", "jdbc:postgresql://db/x")
        .withProperty("SMTP_HOST", "smtp")
        .withProperty("CONFERENCE_CONFIG_PATH", "/config/c.json");
  }

  @Test
  void productionWithSafeSettingsStarts() {
    AppProperties safe = properties("production", false, "site", true, "");
    assertThat(ProductionGuard.problems(safe, explicitSettings())).isEmpty();
    new ProductionGuard(safe, explicitSettings());
  }

  @Test
  void productionListsEveryUnsafeSetting() {
    AppProperties unsafe = properties("PRODUCTION", true, " ", false, "http://localhost:5173");

    assertThat(ProductionGuard.problems(unsafe, new MockEnvironment()))
        .containsExactly(
            "CAPTCHA_TEST_MODE must be false",
            "RECAPTCHA_SITE_KEY and RECAPTCHA_SECRET_KEY must be set",
            "ORGANIZER_HTTPS_ONLY must be true",
            "ALLOWED_ORIGIN is for local development only",
            "DB_URL must be set explicitly",
            "SMTP_HOST must be set explicitly",
            "CONFERENCE_CONFIG_PATH must be set explicitly");
    assertThatThrownBy(() -> new ProductionGuard(unsafe, new MockEnvironment()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageStartingWith("Refusing to start in production: CAPTCHA_TEST_MODE");
  }

  @Test
  void productionNeedsTheSecretKeyToo() {
    AppProperties properties =
        new AppProperties(
            "production",
            "c",
            "d",
            new AppProperties.Mail("f"),
            new AppProperties.Organizer(null, "u", "p", true),
            new AppProperties.Captcha(false, "site", null, "v"),
            new AppProperties.Cors(null),
            new AppProperties.RateLimit(1),
            new AppProperties.Request(1));

    assertThat(ProductionGuard.problems(properties, explicitSettings()))
        .containsExactly("RECAPTCHA_SITE_KEY and RECAPTCHA_SECRET_KEY must be set");
  }

  @Test
  void localEnvironmentIsNotGuarded() {
    AppProperties local = properties("local", true, "", false, "http://localhost:5173");
    new ProductionGuard(local, new MockEnvironment());
    assertThat(local.production()).isFalse();
  }

  @Test
  void organizerEmailsAreTrimmedAndBlanksDropped() {
    assertThat(
            new AppProperties.Organizer(Arrays.asList(" a@x.org", "", " "), "u", "p", true)
                .emails())
        .containsExactly("a@x.org");
    assertThat(new AppProperties.Organizer(null, "u", "p", true).emails()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"127.0.0.1", "127.8.9.10", "::1", "0:0:0:0:0:0:0:1"})
  void loopbackAddresses(String address) {
    assertThat(OrganizerAccess.isLoopback(address)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"10.0.0.1", "203.0.113.7", "localhost", "fe80::1", "1.2.3.4.5", ""})
  void nonLoopbackAddresses(String address) {
    assertThat(OrganizerAccess.isLoopback(address)).isFalse();
  }

  @Test
  void nullAddressIsNotLoopback() {
    assertThat(OrganizerAccess.isLoopback(null)).isFalse();
  }

  static MockHttpServletRequest request(String address, boolean secure) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/export");
    request.setRemoteAddr(address);
    request.setSecure(secure);
    return request;
  }

  @Test
  void transportRules() {
    OrganizerAccess httpsOnly = new OrganizerAccess(true);
    assertThat(httpsOnly.transportAllowed(request("203.0.113.7", false))).isFalse();
    assertThat(httpsOnly.transportAllowed(request("203.0.113.7", true))).isTrue();
    assertThat(httpsOnly.transportAllowed(request("127.0.0.1", false))).isTrue();
    assertThat(new OrganizerAccess(false).transportAllowed(request("203.0.113.7", false))).isTrue();
  }

  static boolean granted(OrganizerAccess access, Authentication caller, MockHttpServletRequest r) {
    return access.authorize(() -> caller, new RequestAuthorizationContext(r)).isGranted();
  }

  @Test
  void onlyTheOrganizerOnAnAllowedTransportIsGranted() {
    OrganizerAccess access = new OrganizerAccess(true);
    Authentication organizer =
        UsernamePasswordAuthenticationToken.authenticated(
            "u", null, AuthorityUtils.createAuthorityList("ROLE_ORGANIZER"));
    Authentication other =
        UsernamePasswordAuthenticationToken.authenticated(
            "v", null, AuthorityUtils.createAuthorityList("ROLE_OTHER"));
    Authentication anonymous =
        new AnonymousAuthenticationToken(
            "k", "anon", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

    assertThat(granted(access, organizer, request("127.0.0.1", false))).isTrue();
    assertThat(granted(access, organizer, request("203.0.113.7", false))).isFalse();
    assertThat(granted(access, other, request("127.0.0.1", false))).isFalse();
    assertThat(granted(access, anonymous, request("127.0.0.1", false))).isFalse();
    assertThat(granted(access, null, request("127.0.0.1", false))).isFalse();
  }

  @Test
  void corsOnlyForTheConfiguredOrigin() {
    MockHttpServletRequest api = new MockHttpServletRequest("GET", "/api/form-config");

    assertThat(SecurityConfig.corsSource("").getCorsConfiguration(api)).isNull();
    assertThat(SecurityConfig.corsSource(null).getCorsConfiguration(api)).isNull();
    CorsConfiguration cors =
        SecurityConfig.corsSource(" http://localhost:5173 ").getCorsConfiguration(api);
    assertThat(cors).isNotNull();
    assertThat(cors.getAllowedOrigins()).containsExactly("http://localhost:5173");
    assertThat(cors.getAllowedMethods()).containsExactly("GET", "POST");
    assertThat(cors.getAllowedHeaders()).containsExactly("Content-Type", "Authorization");
  }
}
