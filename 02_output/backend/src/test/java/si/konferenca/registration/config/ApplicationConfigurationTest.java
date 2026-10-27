package si.konferenca.registration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import si.konferenca.registration.application.FormSettings;

class ApplicationConfigurationTest {

  private static AppProperties properties(
      String environment,
      boolean testMode,
      String siteKey,
      String secretKey,
      String username,
      String password,
      String emails) {
    return new AppProperties(
        environment,
        new AppProperties.Conference("Conf", Path.of("conference.json")),
        Path.of("copies"),
        new AppProperties.Recaptcha(testMode, siteKey, secretKey, "http://verify"),
        new AppProperties.Organizer(username, password, emails, true),
        new AppProperties.Mail("smtp", 25, false, "mailuser", "mailpass", "from@x.si"),
        null,
        new AppProperties.RateLimit(1, 2, 3),
        1000);
  }

  private static AppProperties valid(String environment) {
    return properties(environment, false, "site", "secret", "org", "pw", "a@x.si");
  }

  @ParameterizedTest
  @CsvSource({"local", "test", "production"})
  void knownEnvironmentsStart(String environment) {
    assertThatCode(() -> ApplicationConfiguration.checkEnvironment(valid(environment)))
        .doesNotThrowAnyException();
  }

  @Test
  void unknownEnvironmentIsRefused() {
    assertThatThrownBy(() -> ApplicationConfiguration.checkEnvironment(valid("staging")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageStartingWith("APP_ENVIRONMENT must be one of");
  }

  @ParameterizedTest
  @CsvSource(
      nullValues = "null",
      value = {"true,site,secret", "false,' ',secret", "false,null,secret", "false,site,''"})
  void productionNeedsRealRecaptcha(boolean testMode, String siteKey, String secretKey) {
    AppProperties p = properties("production", testMode, siteKey, secretKey, "o", "p", "a@x.si");

    assertThatThrownBy(() -> ApplicationConfiguration.checkEnvironment(p))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("production needs RECAPTCHA_TEST_MODE=false and both reCAPTCHA keys");
    AppProperties local = properties("local", testMode, siteKey, secretKey, "o", "p", "a@x.si");
    assertThatCode(() -> ApplicationConfiguration.checkEnvironment(local))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @CsvSource(
      nullValues = "null",
      value = {"' ',p,a@x.si", "null,p,a@x.si", "o,'',a@x.si", "o,null,a@x.si", "o,p,' , '"})
  void organizerSettingsAreRequired(String username, String password, String emails) {
    AppProperties p = properties("test", true, "", "", username, password, emails);

    assertThatThrownBy(() -> ApplicationConfiguration.checkEnvironment(p))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("ORGANIZER_USERNAME, ORGANIZER_PASSWORD and ORGANIZER_EMAILS are required");
  }

  @Test
  void formSettingsComeFromTheProperties() {
    AppProperties p = properties("test", true, "site", "", "o", "p", "a@x.si");

    assertThat(new ApplicationConfiguration().formSettings(p))
        .isEqualTo(new FormSettings("Conf", true, "site"));
    assertThat(new ApplicationConfiguration().clock().getZone()).isEqualTo(ZoneOffset.UTC);
  }

  @Test
  void organizerRecipientsAreSplitAndTrimmed() {
    AppProperties.Organizer organizer =
        new AppProperties.Organizer("o", "p", " a@x.si ,, b@x.si,", false);

    assertThat(organizer.emailList()).containsExactly("a@x.si", "b@x.si");
  }

  @Test
  void textFormsDoNotShowSecrets() {
    AppProperties p = valid("test");

    assertThat(p.organizer().toString())
        .isEqualTo("Organizer[username=org, httpsOnly=true]")
        .doesNotContain("pw");
    assertThat(p.mail().toString())
        .isEqualTo("Mail[host=smtp, port=25, starttls=false]")
        .doesNotContain("mailpass");
  }
}
