package si.konferenca.registration.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import si.konferenca.registration.application.CaptchaSettings;
import si.konferenca.registration.domain.ConferenceConfiguration;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.InvalidConfigurationException;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;

/** ConferenceConfigLoader, StartupChecks, AppSettings and ApplicationBeans. */
class ConfigTest {

  private static AppSettings settings(
      String environment,
      String configFile,
      boolean testMode,
      String siteKey,
      String secretKey,
      boolean httpsOnly,
      String cors) {
    return new AppSettings(
        environment,
        "Conf",
        "from@x.y",
        configFile,
        "dir",
        new AppSettings.Organizer("org", "pw", " a@x.y , ,b@x.y ", httpsOnly),
        new AppSettings.Captcha(testMode, siteKey, secretKey, "https://verify"),
        cors,
        60,
        16384);
  }

  private static AppSettings local() {
    return settings("local", "", true, "", "", false, "");
  }

  private static AppSettings production() {
    return settings("PRODUCTION", "/etc/conf.json", false, "site", "secret", true, "");
  }

  private static ConferenceConfiguration parse(String json) {
    return ConferenceConfigLoader.parse(json.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void parsesOptionsAndConsentsWithDefaults() {
    ConferenceConfiguration config =
        parse(
            """
            {"options": [
              {"id": "a", "name": "A", "category": "MEAL"},
              {"id": "b", "name": "B", "category": "EVENT", "active": false,
               "availableTo": ["STUDENT"]}],
             "consents": [{"id": "p", "text": "T"}, {"id": "q", "text": "U", "mandatory": false}]}
            """);

    ConferenceOption a = config.options().get(0);
    assertThat(a.active()).isTrue();
    assertThat(a.category()).isEqualTo(OptionCategory.MEAL);
    assertThat(a.availableTo()).containsExactlyInAnyOrder(RegistrationType.values());
    ConferenceOption b = config.options().get(1);
    assertThat(b.active()).isFalse();
    assertThat(b.availableTo()).containsExactly(RegistrationType.STUDENT);
    assertThat(config.consents().get(0).mandatory()).isTrue();
    assertThat(config.consents().get(1).mandatory()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "not json",
        "{\"options\": [], \"consents\": [], \"extra\": 1}",
        "{\"options\": [{\"id\": \"a\", \"name\": \"A\", \"category\": \"PARTY\"}], \"consents\": []}",
        "{\"options\": [{\"id\": \"a\", \"name\": \"A\", \"category\": \"MEAL\", \"x\": 1}],"
            + " \"consents\": []}",
        "{\"options\": [{\"id\": \"a\", \"name\": \"A\", \"category\": \"MEAL\","
            + " \"availableTo\": [\"NOBODY\"]}], \"consents\": []}",
        "{\"options\": [], \"consents\": [{\"id\": \"p\", \"text\": \"T\", \"mandatory\": \"x\"}]}"
      })
  void unreadableFilesAreInvalid(String json) {
    assertThatThrownBy(() -> parse(json))
        .isInstanceOf(InvalidConfigurationException.class)
        .hasMessageStartingWith("Invalid conference configuration: unreadable file: ");
  }

  @ParameterizedTest
  @ValueSource(strings = {"null", "{}", "{\"options\": []}", "{\"consents\": []}"})
  void missingListsAreInvalid(String json) {
    assertThatThrownBy(() -> parse(json))
        .hasMessage("Invalid conference configuration: options and consents are required");
  }

  @Test
  void emptyEntriesAreInvalid() {
    assertThatThrownBy(() -> parse("{\"options\": [null], \"consents\": []}"))
        .hasMessage("Invalid conference configuration: empty option entry");
    assertThatThrownBy(() -> parse("{\"options\": [], \"consents\": [null]}"))
        .hasMessage("Invalid conference configuration: empty consent entry");
  }

  @Test
  void beanUsesTheShippedFileWhenNoneIsConfigured() throws Exception {
    ConferenceConfiguration config = new ConferenceConfigLoader().conferenceConfiguration(local());

    assertThat(config.consent("privacy")).isPresent();
    assertThat(config.activeOptions()).isNotEmpty();
  }

  @Test
  void beanReadsTheConfiguredFile(@TempDir Path dir) throws Exception {
    Path file = dir.resolve("c.json");
    Files.writeString(file, "{\"options\": [], \"consents\": [{\"id\": \"z\", \"text\": \"Z\"}]}");

    ConferenceConfiguration config =
        new ConferenceConfigLoader()
            .conferenceConfiguration(settings("local", file.toString(), true, "", "", true, ""));

    assertThat(config.consents()).hasSize(1);
  }

  @Test
  void productionRequiresAConfiguredFile() {
    AppSettings blank = settings("production", " ", false, "s", "k", true, "");
    AppSettings none = settings("production", null, false, "s", "k", true, "");

    for (AppSettings s : new AppSettings[] {blank, none}) {
      assertThatThrownBy(() -> new ConferenceConfigLoader().conferenceConfiguration(s))
          .hasMessage(
              "Invalid conference configuration: CONFERENCE_CONFIG_FILE is required in production");
    }
  }

  @Test
  void localAndCompleteProductionSettingsPass() {
    assertThatCode(() -> StartupChecks.check(local())).doesNotThrowAnyException();
    assertThatCode(() -> StartupChecks.check(production())).doesNotThrowAnyException();
    assertThatCode(() -> new StartupChecks(production()).verify()).doesNotThrowAnyException();
  }

  private static void assertRefused(AppSettings s, String message) {
    assertThatThrownBy(() -> StartupChecks.check(s))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Refusing to start: " + message);
  }

  @Test
  void captchaKeysAreRequiredOutsideTestMode() {
    String message =
        "RECAPTCHA_SITE_KEY and RECAPTCHA_SECRET_KEY are required unless CAPTCHA_TEST_MODE";
    assertRefused(settings("local", "", false, "", "k", false, ""), message);
    assertRefused(settings("local", "", false, "s", " ", false, ""), message);
    assertRefused(settings("local", "", false, null, "k", false, ""), message);
    assertRefused(settings("local", "", false, "s", null, false, ""), message);
  }

  @Test
  void organizerCredentialsAreRequired() {
    for (AppSettings.Organizer organizer :
        new AppSettings.Organizer[] {
          new AppSettings.Organizer(" ", "pw", "", true),
          new AppSettings.Organizer("u", "", "", true),
          new AppSettings.Organizer(null, "pw", "", true),
          new AppSettings.Organizer("u", null, "", true)
        }) {
      AppSettings s =
          new AppSettings(
              "local",
              "C",
              "f",
              "",
              "d",
              organizer,
              new AppSettings.Captcha(true, "", "", "u"),
              "",
              1,
              1);
      assertRefused(s, "ORGANIZER_USERNAME and ORGANIZER_PASSWORD are required");
    }
  }

  @Test
  void productionRefusesTestModePlainHttpAndCors() {
    assertRefused(
        settings("production", "f", true, "s", "k", true, ""),
        "CAPTCHA_TEST_MODE must be off in production");
    assertRefused(
        settings("production", "f", false, "s", "k", false, ""),
        "ORGANIZER_HTTPS_ONLY must be on in production");
    assertRefused(
        settings("production", "f", false, "s", "k", true, "http://localhost:5173"),
        "CORS_ALLOWED_ORIGIN must not be set in production");
    assertThatCode(
            () -> StartupChecks.check(settings("production", "f", false, "s", "k", true, null)))
        .doesNotThrowAnyException();
  }

  @Test
  void limitsMustBePositive() {
    String message = "RATE_LIMIT_PER_MINUTE and REQUEST_MAX_BYTES must be positive";
    AppSettings.Organizer o = new AppSettings.Organizer("u", "p", "", true);
    AppSettings.Captcha c = new AppSettings.Captcha(true, "", "", "u");
    assertRefused(new AppSettings("local", "C", "f", "", "d", o, c, "", 0, 1), message);
    assertRefused(new AppSettings("local", "C", "f", "", "d", o, c, "", 1, 0), message);
    assertThatCode(
            () -> StartupChecks.check(new AppSettings("local", "C", "f", "", "d", o, c, "", 1, 1)))
        .doesNotThrowAnyException();
  }

  @Test
  void settingsHelpers() {
    assertThat(local().organizer().emailList()).containsExactly("a@x.y", "b@x.y");
    assertThat(new AppSettings.Organizer("u", "p", null, true).emailList()).isEmpty();
    assertThat(local().production()).isFalse();
    assertThat(production().production()).isTrue();
    assertThat(production().organizer().toString())
        .isEqualTo("Organizer[username=org, httpsOnly=true]")
        .doesNotContain("pw");
    assertThat(production().captcha().toString())
        .isEqualTo("Captcha[testMode=false, verifyUrl=https://verify]")
        .doesNotContain("secret");
  }

  @Test
  void applicationBeansExposeCaptchaSettingsAndUtcClock() {
    ApplicationBeans beans = new ApplicationBeans();

    assertThat(beans.captchaSettings(production())).isEqualTo(new CaptchaSettings(false, "site"));
    assertThat(beans.captchaSettings(settings("local", "", true, null, null, true, "")))
        .isEqualTo(new CaptchaSettings(true, ""));
    assertThat(beans.clock().getZone()).isEqualTo(java.time.ZoneOffset.UTC);
  }
}
