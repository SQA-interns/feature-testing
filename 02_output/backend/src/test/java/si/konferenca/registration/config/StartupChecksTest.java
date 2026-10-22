package si.konferenca.registration.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StartupChecksTest {

  private static AppSettings settings(
      String environment,
      boolean testMode,
      String siteKey,
      String secretKey,
      boolean httpsOnly,
      String cors,
      String username,
      int rateLimit,
      long maxBytes) {
    return new AppSettings(
        environment,
        "classpath:conference.json",
        "Konferenca",
        "/data/json-copies",
        new AppSettings.Mail("smtp", 587, true, "", "", "from@x.si"),
        new AppSettings.Captcha(testMode, siteKey, secretKey, "https://verify"),
        new AppSettings.Organizer(username, "password-of-16-chars", "o@x.si", httpsOnly),
        cors,
        rateLimit,
        maxBytes);
  }

  private static AppSettings production() {
    return settings("production", false, "site", "secret", true, "", "organizer", 60, 16384);
  }

  @Test
  void safeProductionSettingsPass() {
    assertThatCode(() -> StartupChecks.verify(production())).doesNotThrowAnyException();
    assertThat(production().isProduction()).isTrue();
  }

  @Test
  void localSettingsMayUseTestModeAndPlainHttp() {
    AppSettings local =
        settings("local", true, "", "", false, "http://localhost:5173", "organizer", 60, 1);

    assertThatCode(() -> StartupChecks.verify(local)).doesNotThrowAnyException();
    assertThat(local.isProduction()).isFalse();
  }

  @Test
  void productionRefusesEveryUnsafeSettingAtOnce() {
    AppSettings unsafe =
        settings("production", true, " ", null, false, "http://x", "organizer", 60, 1);

    assertThatThrownBy(() -> StartupChecks.verify(unsafe))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CAPTCHA_TEST_MODE must be off in production")
        .hasMessageContaining("RECAPTCHA_SITE_KEY is required")
        .hasMessageContaining("RECAPTCHA_SECRET_KEY is required")
        .hasMessageContaining("ORGANIZER_HTTPS_ONLY must be on in production")
        .hasMessageContaining("CORS_ALLOWED_ORIGIN is allowed only locally");
  }

  @Test
  void requiredSettingsAndLimitsAreCheckedEverywhere() {
    AppSettings broken =
        new AppSettings(
            "staging",
            "",
            "K",
            null,
            new AppSettings.Mail("smtp", 587, true, "", "", "f@x.si"),
            new AppSettings.Captcha(true, "", "", "https://verify"),
            new AppSettings.Organizer(" ", null, "", false),
            null,
            0,
            0);

    assertThatThrownBy(() -> StartupChecks.verify(broken))
        .hasMessageContaining("APP_ENVIRONMENT must be local, test or production")
        .hasMessageContaining("CONFERENCE_CONFIG is required")
        .hasMessageContaining("JSON_COPY_DIR is required")
        .hasMessageContaining("ORGANIZER_USERNAME is required")
        .hasMessageContaining("ORGANIZER_PASSWORD is required")
        .hasMessageContaining("ORGANIZER_EMAILS is required")
        .hasMessageContaining("RATE_LIMIT_PER_MINUTE must be at least 1")
        .hasMessageContaining("MAX_REQUEST_BYTES must be at least 1")
        .message()
        .doesNotContain("password-of-16-chars");
  }

  @Test
  void settingsNeverPrintSecrets() {
    AppSettings settings = production();

    assertThat(settings.organizer().toString()).isEqualTo("Organizer[httpsOnly=true]");
    assertThat(settings.captcha().toString())
        .doesNotContain("secret", "site")
        .contains("testMode=false");
    assertThat(settings.mail().toString())
        .contains("host=smtp", "port=587")
        .doesNotContain("password");
  }
}
