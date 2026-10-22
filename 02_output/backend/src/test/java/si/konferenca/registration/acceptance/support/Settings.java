package si.konferenca.registration.acceptance.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * The backend's configuration as the acceptance tests set it: the environment settings of {@code
 * docs/02_specification.md} section 9, plus the standard Spring datasource properties.
 */
public final class Settings {

  public static final String ORGANIZER_USERNAME = "organizer";
  public static final String ORGANIZER_PASSWORD = "acceptance-organizer-pw-2026";
  public static final List<String> ORGANIZER_EMAILS =
      List.of("organizer-one@example.org", "organizer-two@example.org");
  public static final String CONFERENCE_NAME = "Acceptance Conference 2026";
  public static final String CAPTCHA_PASS = "test-pass";
  public static final String CONFERENCE_CONFIG = "classpath:acceptance/conference.json";

  private Settings() {}

  /** The settings every acceptance context starts from. */
  public static Map<String, String> defaults(Path jsonCopyDir) {
    Map<String, String> values = new LinkedHashMap<>();
    values.put("spring.datasource.url", Containers.POSTGRES.getJdbcUrl());
    values.put("spring.datasource.username", Containers.POSTGRES.getUsername());
    values.put("spring.datasource.password", Containers.POSTGRES.getPassword());
    values.put("APP_ENVIRONMENT", "test");
    values.put("SMTP_HOST", Containers.MAILPIT.getHost());
    values.put("SMTP_PORT", String.valueOf(Containers.MAILPIT.getMappedPort(Containers.SMTP_PORT)));
    values.put("SMTP_TLS", "false");
    values.put("MAIL_FROM", "registration@acceptance.example.org");
    values.put("CONFERENCE_NAME", CONFERENCE_NAME);
    values.put("CONFERENCE_CONFIG", CONFERENCE_CONFIG);
    values.put("JSON_COPY_DIR", jsonCopyDir.toString());
    values.put("CAPTCHA_TEST_MODE", "true");
    values.put("RECAPTCHA_SITE_KEY", "acceptance-site-key");
    values.put("RECAPTCHA_SECRET_KEY", "acceptance-secret-key");
    values.put("ORGANIZER_USERNAME", ORGANIZER_USERNAME);
    values.put("ORGANIZER_PASSWORD", ORGANIZER_PASSWORD);
    values.put("ORGANIZER_EMAILS", String.join(",", ORGANIZER_EMAILS));
    values.put("ORGANIZER_HTTPS_ONLY", "true");
    values.put("RATE_LIMIT_PER_MINUTE", "100000");
    return values;
  }

  public static void register(DynamicPropertyRegistry registry, Map<String, String> values) {
    values.forEach((name, value) -> registry.add(name, () -> value));
  }

  /** The same settings as command-line arguments, for starting the backend directly. */
  public static String[] asArguments(Map<String, String> values) {
    List<String> arguments = new ArrayList<>();
    values.forEach((name, value) -> arguments.add("--" + name + "=" + value));
    return arguments.toArray(String[]::new);
  }

  public static Path newDirectory(String prefix) {
    try {
      return Files.createTempDirectory(prefix);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
