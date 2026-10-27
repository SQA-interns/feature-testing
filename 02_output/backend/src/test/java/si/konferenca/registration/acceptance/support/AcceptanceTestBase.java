package si.konferenca.registration.acceptance.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import si.konferenca.registration.RegistrationApplication;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Black-box harness: the backend runs on a random port against PostgreSQL and Mailpit containers
 * and is configured only through the variables of docs/02_specification.md S7.1.
 */
@SpringBootTest(
    classes = RegistrationApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTestBase {

  public static final String ORGANIZER_USERNAME = "organizer";
  public static final String ORGANIZER_PASSWORD = "organizer-test-password-1234";
  public static final List<String> ORGANIZER_EMAILS =
      List.of("org1@konferenca.test", "org2@konferenca.test");
  public static final String CONFERENCE_NAME = "Test Conference 2026";
  public static final String MAIL_FROM = "registration@konferenca.test";

  protected static final JsonMapper JSON = JsonMapper.builder().build();

  @SuppressWarnings("resource")
  protected static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("registration")
          .withUsername("registration")
          .withPassword("registration-test");

  @SuppressWarnings("resource")
  protected static final GenericContainer<?> MAILPIT =
      new GenericContainer<>("axllent/mailpit:v1.31.1")
          .withExposedPorts(1025, 8025)
          .waitingFor(Wait.forHttp("/livez").forPort(8025));

  protected static final Path JSON_COPY_DIR;
  protected static final Path CONFERENCE_CONFIG_FILE;

  static {
    POSTGRES.start();
    MAILPIT.start();
    try {
      JSON_COPY_DIR = Files.createTempDirectory("json-copies");
      CONFERENCE_CONFIG_FILE = TestConferenceConfig.writeDefault();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @LocalServerPort protected int port;

  protected final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  protected final Mailpit mailpit =
      new Mailpit("http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025));

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry registry) {
    baseProperties().forEach((key, value) -> registry.add(key, () -> value));
  }

  /** The configuration every acceptance context starts from, by variable name (S7.1). */
  public static Map<String, String> baseProperties() {
    Map<String, String> p = new LinkedHashMap<>();
    p.put("APP_ENVIRONMENT", "test");
    p.put("DATABASE_URL", POSTGRES.getJdbcUrl());
    p.put("DATABASE_USERNAME", POSTGRES.getUsername());
    p.put("POSTGRES_PASSWORD", POSTGRES.getPassword());
    p.put("SMTP_HOST", MAILPIT.getHost());
    p.put("SMTP_PORT", String.valueOf(MAILPIT.getMappedPort(1025)));
    p.put("SMTP_STARTTLS", "false");
    p.put("MAIL_FROM", MAIL_FROM);
    p.put("CONFERENCE_NAME", CONFERENCE_NAME);
    p.put("CONFERENCE_CONFIG_FILE", CONFERENCE_CONFIG_FILE.toString());
    p.put("JSON_COPY_DIR", JSON_COPY_DIR.toString());
    p.put("RECAPTCHA_TEST_MODE", "true");
    p.put("ORGANIZER_USERNAME", ORGANIZER_USERNAME);
    p.put("ORGANIZER_PASSWORD", ORGANIZER_PASSWORD);
    p.put("ORGANIZER_EMAILS", String.join(",", ORGANIZER_EMAILS));
    p.put("ORGANIZER_HTTPS_ONLY", "true");
    p.put("RATE_LIMIT_FORM_CONFIG_PER_MINUTE", "10000");
    p.put("RATE_LIMIT_REGISTRATION_PER_MINUTE", "10000");
    p.put("RATE_LIMIT_EXPORT_PER_MINUTE", "10000");
    return p;
  }

  @BeforeEach
  void resetState() throws Exception {
    try (Connection c = db();
        PreparedStatement exists =
            c.prepareStatement("SELECT to_regclass('public.registration') IS NOT NULL")) {
      try (ResultSet rs = exists.executeQuery()) {
        rs.next();
        if (rs.getBoolean(1)) {
          c.createStatement().execute("TRUNCATE registration CASCADE");
        }
      }
    }
    try (Stream<Path> files = Files.list(JSON_COPY_DIR)) {
      for (Path f : files.toList()) {
        Files.deleteIfExists(f);
      }
    }
    mailpit.deleteAll();
  }

  // ---- HTTP ----

  protected URI api(String path) {
    return URI.create("http://localhost:" + port + "/api" + path);
  }

  protected HttpResponse<String> getJson(String path) {
    return send(HttpRequest.newBuilder(api(path)).header("Accept", "application/json").GET());
  }

  protected HttpResponse<String> postRegistration(Map<String, Object> body) {
    return send(
        HttpRequest.newBuilder(api("/registrations"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body))));
  }

  protected HttpResponse<byte[]> getExport(HttpRequest.Builder request) {
    try {
      return http.send(request.GET().build(), HttpResponse.BodyHandlers.ofByteArray());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  protected HttpRequest.Builder exportRequest() {
    return HttpRequest.newBuilder(api("/registrations/export"));
  }

  protected static String basicAuth(String user, String password) {
    return "Basic "
        + Base64.getEncoder()
            .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
  }

  protected HttpResponse<String> send(HttpRequest.Builder request) {
    try {
      return http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  protected static JsonNode json(HttpResponse<String> response) {
    return JSON.readTree(response.body());
  }

  /** Field names of the fieldErrors in a 400 ValidationError body. */
  protected static List<String> errorFields(HttpResponse<String> response) {
    List<String> fields = new ArrayList<>();
    JsonNode errors = json(response).path("fieldErrors");
    for (JsonNode e : errors) {
      fields.add(e.path("field").asString());
    }
    return fields;
  }

  // ---- Database (the storage contract docs/02_contracts/database-schema.sql) ----

  protected static Connection db() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  /** Number of registrations; a missing table counts as a failure, not as zero. */
  protected static long registrationCount() {
    return queryLong("SELECT count(*) FROM registration");
  }

  protected static long queryLong(String sql, Object... args) {
    try (Connection c = db();
        PreparedStatement ps = c.prepareStatement(sql)) {
      for (int i = 0; i < args.length; i++) {
        ps.setObject(i + 1, args[i]);
      }
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    } catch (SQLException e) {
      throw new AssertionError("storage query failed: " + e.getMessage(), e);
    }
  }

  protected static List<Map<String, Object>> queryRows(String sql, Object... args) {
    try (Connection c = db();
        PreparedStatement ps = c.prepareStatement(sql)) {
      for (int i = 0; i < args.length; i++) {
        ps.setObject(i + 1, args[i]);
      }
      try (ResultSet rs = ps.executeQuery()) {
        List<Map<String, Object>> rows = new ArrayList<>();
        int columns = rs.getMetaData().getColumnCount();
        while (rs.next()) {
          Map<String, Object> row = new LinkedHashMap<>();
          for (int i = 1; i <= columns; i++) {
            row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
          }
          rows.add(row);
        }
        return rows;
      }
    } catch (SQLException e) {
      throw new AssertionError("storage query failed: " + e.getMessage(), e);
    }
  }

  // ---- Outcomes shared by the criteria ----

  /**
   * "Rejected" as acceptance-criteria.md defines it: an error status with field errors, no database
   * row, no JSON copy and no email.
   */
  protected void assertRejected(HttpResponse<String> response, String expectedField) {
    org.assertj.core.api.Assertions.assertThat(response.statusCode())
        .as("status of a rejected submission, body: %s", response.body())
        .isEqualTo(400);
    org.assertj.core.api.Assertions.assertThat(errorFields(response))
        .as("fields with errors")
        .contains(expectedField);
    assertNothingStoredOrSent();
  }

  protected void assertNothingStoredOrSent() {
    org.assertj.core.api.Assertions.assertThat(registrationCount())
        .as("registrations in the database")
        .isZero();
    org.assertj.core.api.Assertions.assertThat(jsonCopyDirIsEmpty())
        .as("JSON copy directory is empty")
        .isTrue();
    org.assertj.core.api.Assertions.assertThat(mailpit.total()).as("emails sent").isZero();
  }

  // ---- JSON copies ----

  protected static List<Path> jsonCopies() {
    try (Stream<Path> files = Files.list(JSON_COPY_DIR)) {
      return files.filter(f -> f.getFileName().toString().endsWith(".json")).toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected static boolean jsonCopyDirIsEmpty() {
    try (Stream<Path> files = Files.list(JSON_COPY_DIR)) {
      return files.findAny().isEmpty();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
