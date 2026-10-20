package si.konferenca.registration.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.Ordered;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mock.web.MockHttpServletResponse;
import si.konferenca.registration.adapter.captcha.RecaptchaHttpVerifier;
import si.konferenca.registration.adapter.captcha.TestModeCaptchaVerifier;
import si.konferenca.registration.adapter.web.BodySizeLimitFilter;
import si.konferenca.registration.adapter.web.RateLimitFilter;
import si.konferenca.registration.application.FormConfigService;
import si.konferenca.registration.domain.ConferenceConfig;

/** Bean methods whose effect the shared application context does not show per test. */
class ConfigurationBeansTest {

  static AppProperties properties(boolean testMode) {
    return SecuritySettingsTest.properties("local", testMode, "site", true, "");
  }

  @Test
  void filtersAreRegisteredForTheApiAheadOfSecurity() {
    WebFilterConfiguration filters = new WebFilterConfiguration();

    var rate = filters.rateLimitFilter(properties(true), Clock.systemUTC());
    assertThat(rate.getFilter()).isInstanceOf(RateLimitFilter.class);
    assertThat(rate.getUrlPatterns()).containsExactly("/api/*");
    assertThat(rate.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 10);

    var size = filters.bodySizeLimitFilter(properties(true));
    assertThat(size.getFilter()).isInstanceOf(BodySizeLimitFilter.class);
    assertThat(size.getUrlPatterns()).containsExactly("/api/*");
    assertThat(size.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 20);
  }

  @Test
  void captchaVerifierFollowsTheTestModeSetting() {
    RegistrationConfiguration configuration = new RegistrationConfiguration();

    assertThat(configuration.captchaVerifier(properties(true)))
        .isInstanceOf(TestModeCaptchaVerifier.class);
    assertThat(configuration.captchaVerifier(properties(false)))
        .isInstanceOf(RecaptchaHttpVerifier.class);
  }

  static String smtpAuth(JavaMailSender sender) {
    SmartInitializingSingleton hook = new RegistrationConfiguration().smtpAuthentication(sender);
    assertThat(hook).isNotNull();
    hook.afterSingletonsInstantiated();
    return sender instanceof JavaMailSenderImpl impl
        ? impl.getJavaMailProperties().getProperty("mail.smtp.auth")
        : null;
  }

  @Test
  void smtpAuthenticationOnlyWithAnAccount() {
    JavaMailSenderImpl withAccount = new JavaMailSenderImpl();
    withAccount.setUsername("user");
    assertThat(smtpAuth(withAccount)).isEqualTo("true");

    JavaMailSenderImpl blank = new JavaMailSenderImpl();
    blank.setUsername(" ");
    assertThat(smtpAuth(blank)).isNull();
    assertThat(smtpAuth(new JavaMailSenderImpl())).isNull();

    JavaMailSender other = Mockito.mock(JavaMailSender.class);
    smtpAuth(other);
    Mockito.verifyNoInteractions(other);
  }

  @Test
  void refusedOrganizerTransportGetsAJsonMessage() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    SecurityConfig.refuse(response);

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
    assertThat(response.getContentAsString())
        .isEqualTo("{\"message\":\"Organizer access requires HTTPS.\"}");
  }

  @Test
  void formConfigServiceKeepsTestModeOff() {
    ConferenceConfig config = ConferenceConfigLoader.load("classpath:conference.json");
    assertThat(new FormConfigService(config, "site", false).captchaTestMode()).isFalse();
  }

  @Test
  void loaderKeepsOptionNames() {
    ConferenceConfig config =
        ConferenceConfigLoader.parse(
            ("{\"conferenceName\":\"K\",\"options\":[{\"id\":\"a\",\"name\":\"Lunch\","
                    + "\"category\":\"MEAL\"}],\"consents\":[]}")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    assertThat(config.option("a").orElseThrow().name()).isEqualTo("Lunch");
    assertThat(List.of(config.conferenceName())).containsExactly("K");
  }
}
