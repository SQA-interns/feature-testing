package si.konferenca.registration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import si.konferenca.registration.application.FormConfiguration;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.ConferenceConfigException;

class ConfigUnitTest {

  private static AppProperties properties(
      boolean testMode,
      String siteKey,
      String secret,
      String user,
      String password,
      String emails,
      boolean httpsOnly,
      String cors) {
    return new AppProperties(
        new AppProperties.Mail("from@x.si"),
        new AppProperties.Conference("Conf", "classpath:conference-config.json"),
        new AppProperties.JsonCopy("/tmp"),
        new AppProperties.Captcha(testMode, siteKey, secret, "http://verify"),
        new AppProperties.Organizer(user, password, emails, httpsOnly),
        new AppProperties.Cors(cors),
        new AppProperties.RateLimit(30),
        new AppProperties.Request(16384));
  }

  private static AppProperties production() {
    return properties(false, "site", "secret", "org", "pass", "o@x.si", true, "");
  }

  private static List<String> problems(AppProperties p, String... profiles) {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles(profiles);
    try {
      new StartupGuard(p, environment);
      return List.of();
    } catch (IllegalStateException e) {
      return List.of(e.getMessage().substring("Refusing to start: ".length()).split("; "));
    }
  }

  @Test
  void productionSettingsStart() {
    assertThat(problems(production())).isEmpty();
  }

  @Test
  void testModeOnlyInLocalOrTest() {
    AppProperties p = properties(true, "", "", "org", "pass", "o@x.si", true, null);

    assertThat(problems(p))
        .containsExactly("CAPTCHA_TEST_MODE=true is allowed only in the local and test profiles");
    assertThat(problems(p, "local")).isEmpty();
    assertThat(problems(p, "test")).isEmpty();
  }

  @Test
  void keysAreRequiredWithoutTestMode() {
    String message = "RECAPTCHA_SITE_KEY and RECAPTCHA_SECRET_KEY are required without test mode";
    assertThat(problems(properties(false, "", "s", "o", "p", "e@x.si", true, ""), "local"))
        .containsExactly(message);
    assertThat(problems(properties(false, "k", " ", "o", "p", "e@x.si", true, ""), "local"))
        .containsExactly(message);
    assertThat(problems(properties(false, null, null, "o", "p", "e@x.si", true, ""), "test"))
        .containsExactly(message);
  }

  @Test
  void httpsOnlyOffOnlyInLocalOrTest() {
    AppProperties p = properties(false, "k", "s", "o", "p", "e@x.si", false, "");

    assertThat(problems(p))
        .containsExactly(
            "ORGANIZER_HTTPS_ONLY=false is allowed only in the local and test profiles");
    assertThat(problems(p, "test")).isEmpty();
  }

  @Test
  void organizerSettingsAreRequired() {
    String login = "ORGANIZER_USERNAME and ORGANIZER_PASSWORD are required";
    assertThat(problems(properties(false, "k", "s", "", "p", "e@x.si", true, "")))
        .containsExactly(login);
    assertThat(problems(properties(false, "k", "s", "o", null, "e@x.si", true, "")))
        .containsExactly(login);
    assertThat(problems(properties(false, "k", "s", "o", "p", " , ", true, "")))
        .containsExactly("ORGANIZER_EMAILS is required");
  }

  @Test
  void corsOnlyInLocal() {
    AppProperties p =
        properties(false, "k", "s", "o", "p", "e@x.si", true, "http://localhost:5173");

    assertThat(problems(p, "test"))
        .containsExactly("CORS_ALLOWED_ORIGIN is allowed only in the local profile");
    assertThat(problems(p, "local")).isEmpty();
  }

  @Test
  void reportsEveryProblemAtOnce() {
    assertThat(problems(properties(true, "", "", null, null, null, false, "x"))).hasSize(5);
  }

  @Test
  void organizerEmailListIsTrimmedAndSecretsStayOutOfToString() {
    AppProperties.Organizer organizer =
        new AppProperties.Organizer("org", "s3cret", " a@x.si, ,b@x.si ", true);

    assertThat(organizer.emailList()).containsExactly("a@x.si", "b@x.si");
    assertThat(new AppProperties.Organizer("o", "p", null, true).emailList()).isEmpty();
    assertThat(organizer.toString()).isEqualTo("Organizer[username=org, httpsOnly=true]");
    assertThat(new AppProperties.Captcha(false, "site", "hidden", "u").toString())
        .isEqualTo("Captcha[testMode=false, siteKey=site]");
  }

  @Test
  void catalogIsLoadedFromTheConfiguredLocation() {
    ConferenceCatalog catalog =
        new CatalogConfig().conferenceCatalog(production(), new DefaultResourceLoader());

    assertThat(catalog.option("ws-ai")).isPresent();
  }

  @Test
  void catalogLocationMustBeSetAndExist() {
    CatalogConfig config = new CatalogConfig();
    DefaultResourceLoader loader = new DefaultResourceLoader();

    assertThatThrownBy(() -> config.conferenceCatalog(withLocation(""), loader))
        .isInstanceOf(ConferenceConfigException.class)
        .hasMessage("CONFERENCE_CONFIG is not set");
    assertThatThrownBy(() -> config.conferenceCatalog(withLocation(null), loader))
        .hasMessage("CONFERENCE_CONFIG is not set");
    assertThatThrownBy(() -> config.conferenceCatalog(withLocation("classpath:none.json"), loader))
        .hasMessage("Conference configuration classpath:none.json not found");
  }

  @Test
  void unreadableCatalogFails() {
    Resource broken =
        new AbstractResource() {
          @Override
          public boolean exists() {
            return true;
          }

          @Override
          public String getDescription() {
            return "broken";
          }

          @Override
          public InputStream getInputStream() throws IOException {
            throw new IOException("unreadable");
          }
        };
    DefaultResourceLoader loader =
        new DefaultResourceLoader() {
          @Override
          public Resource getResource(String location) {
            return broken;
          }
        };

    assertThatThrownBy(() -> new CatalogConfig().conferenceCatalog(withLocation("x"), loader))
        .isInstanceOf(ConferenceConfigException.class)
        .hasMessage("Conference configuration x is unreadable");
  }

  private static AppProperties withLocation(String location) {
    AppProperties p = production();
    return new AppProperties(
        p.mail(),
        new AppProperties.Conference("Conf", location),
        p.jsonCopy(),
        p.captcha(),
        p.organizer(),
        p.cors(),
        p.rateLimit(),
        p.request());
  }

  @Test
  void formConfigurationTreatsAMissingSiteKeyAsEmpty() {
    ConferenceCatalog catalog = ConferenceCatalog.of(List.of(), List.of());

    assertThat(new FormConfiguration(catalog, null, true).current().captchaSiteKey()).isEmpty();
    assertThat(new FormConfiguration(catalog, "k", false).current())
        .isEqualTo(new FormConfiguration.View(List.of(), List.of(), "k", false));
  }

  @Test
  void httpsOnlyFilterChecksOnlyTheExportAndOnlyWhenOn() throws Exception {
    MockHttpServletRequest export = new MockHttpServletRequest("GET", "/api/export");
    export.setRemoteAddr("203.0.113.1");
    MockHttpServletRequest other = new MockHttpServletRequest("GET", "/api/form-config");
    other.setRemoteAddr("203.0.113.1");

    assertThat(passes(new OrganizerHttpsOnlyFilter(true), export)).isFalse();
    assertThat(passes(new OrganizerHttpsOnlyFilter(true), other)).isTrue();
    assertThat(passes(new OrganizerHttpsOnlyFilter(false), export)).isTrue();
    export.setSecure(true);
    assertThat(passes(new OrganizerHttpsOnlyFilter(true), export)).isTrue();
  }

  @Test
  void httpsOnlyFilterRefusalHasAJsonBody() throws Exception {
    MockHttpServletRequest export = new MockHttpServletRequest("GET", "/api/export");
    export.setRemoteAddr("203.0.113.1");
    MockHttpServletResponse response = new MockHttpServletResponse();

    new OrganizerHttpsOnlyFilter(true).doFilter(export, response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
    assertThat(response.getContentAsString()).isEqualTo(OrganizerHttpsOnlyFilter.BODY);
  }

  @Test
  void loopbackAddressesAreRecognised() {
    assertThat(OrganizerHttpsOnlyFilter.isLoopback("127.0.0.1")).isTrue();
    assertThat(OrganizerHttpsOnlyFilter.isLoopback("127.10.0.3")).isTrue();
    assertThat(OrganizerHttpsOnlyFilter.isLoopback("::1")).isTrue();
    assertThat(OrganizerHttpsOnlyFilter.isLoopback("10.0.0.1")).isFalse();
    assertThat(OrganizerHttpsOnlyFilter.isLoopback("")).isFalse();
    assertThat(OrganizerHttpsOnlyFilter.isLoopback(null)).isFalse();
    assertThatCode(() -> OrganizerHttpsOnlyFilter.isLoopback("no such host.invalid"))
        .doesNotThrowAnyException();
    assertThat(OrganizerHttpsOnlyFilter.isLoopback("no such host.invalid")).isFalse();
  }

  private static boolean passes(OrganizerHttpsOnlyFilter filter, MockHttpServletRequest request)
      throws Exception {
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(request, new MockHttpServletResponse(), chain);
    return chain.getRequest() != null;
  }
}
