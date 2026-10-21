package si.konferenca.registration.acceptance.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL and Mailpit containers shared by all acceptance tests, and the backend settings of
 * {@code docs/02_specification.md} section 3 that point the backend at them.
 */
public final class TestStack {

  public static final String ORGANIZER_USERNAME = "organizer";
  public static final String ORGANIZER_PASSWORD = "acceptance-organizer-secret-01";
  public static final List<String> ORGANIZER_EMAILS =
      List.of("org-one@example.org", "org-two@example.org");
  public static final String CONFERENCE_NAME = "Konferenca Ljubljana 2026";
  public static final String DEFAULT_CONFIG = "classpath:acceptance/conference-config.json";

  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:16.15-alpine"))
          .withDatabaseName("registration")
          .withUsername("registration")
          .withPassword("acceptance-db-password");

  private static final GenericContainer<?> MAILPIT =
      new GenericContainer<>(DockerImageName.parse("axllent/mailpit:v1.31.1"))
          .withExposedPorts(1025, 8025)
          .waitingFor(Wait.forHttp("/api/v1/info").forPort(8025));

  private static final Path JSON_COPY_DIR;

  static {
    POSTGRES.start();
    MAILPIT.start();
    try {
      JSON_COPY_DIR = Files.createTempDirectory("acceptance-json-copies");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private TestStack() {}

  /** Registers the default settings, then the overrides (same keys win). */
  public static void register(DynamicPropertyRegistry registry, Map<String, Object> overrides) {
    properties(overrides).forEach((key, value) -> registry.add(key, () -> value));
  }

  public static void register(DynamicPropertyRegistry registry) {
    register(registry, Map.of());
  }

  /** The default settings with the overrides applied. */
  public static Map<String, Object> properties(Map<String, Object> overrides) {
    Map<String, Object> p = new LinkedHashMap<>();
    p.put("spring.datasource.url", POSTGRES.getJdbcUrl());
    p.put("spring.datasource.username", POSTGRES.getUsername());
    p.put("spring.datasource.password", POSTGRES.getPassword());
    p.put("spring.datasource.hikari.maximum-pool-size", "2");
    p.put("spring.mail.host", MAILPIT.getHost());
    p.put("spring.mail.port", String.valueOf(MAILPIT.getMappedPort(1025)));
    p.put("spring.mail.properties.mail.smtp.starttls.enable", "false");
    p.put("spring.mail.properties.mail.smtp.starttls.required", "false");
    p.put("app.mail.from", "registration@konferenca.example");
    p.put("app.conference.name", CONFERENCE_NAME);
    p.put("app.conference.config-location", DEFAULT_CONFIG);
    p.put("app.json-copy.directory", JSON_COPY_DIR.toString());
    p.put("app.captcha.test-mode", "true");
    p.put("app.captcha.site-key", "");
    p.put("app.captcha.secret-key", "");
    p.put("app.organizer.username", ORGANIZER_USERNAME);
    p.put("app.organizer.password", ORGANIZER_PASSWORD);
    p.put("app.organizer.emails", String.join(",", ORGANIZER_EMAILS));
    p.put("app.organizer.https-only", "false");
    p.put("app.rate-limit.per-minute", "100000");
    p.putAll(overrides);
    return p;
  }

  public static Database database() {
    return new Database(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  public static Mailpit mailpit() {
    return new Mailpit("http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025));
  }

  public static JsonCopies jsonCopies() {
    return new JsonCopies(JSON_COPY_DIR);
  }
}
