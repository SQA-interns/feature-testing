package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import si.konferenca.registration.RegistrationApplication;
import tools.jackson.databind.JsonNode;

/**
 * Black-box harness for the backend acceptance tests: the application runs on a random port and is
 * reached only over HTTP. Settings use the names of docs/02_specification.md section 4; storage and
 * mail are observed through {@link AcceptanceProbes}.
 */
@SpringBootTest(
    classes = RegistrationApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTestSupport extends AcceptanceProbes {

  static final String ORGANIZER_USERNAME = "organizer";
  static final String ORGANIZER_PASSWORD = "acceptance-organizer-pass-0001";
  static final List<String> ORGANIZER_EMAILS = List.of("org1@example.org", "org2@example.org");
  static final String CONFERENCE_NAME = "Konferenca Test 2026";
  @LocalServerPort int port;

  /** The settings of {@link #registerSettings} as plain values, for starting the app directly. */
  static Map<String, Object> settingsMap(Map<String, String> overrides) {
    Map<String, Object> values = new LinkedHashMap<>();
    registerSettings(
        new DynamicPropertyRegistry() {
          @Override
          public void add(String name, Supplier<Object> valueSupplier) {
            values.put(name, valueSupplier.get());
          }
        },
        overrides);
    return values;
  }

  /** Default settings of the acceptance context; {@code overrides} replace single settings. */
  static void registerSettings(DynamicPropertyRegistry registry, Map<String, String> overrides) {
    Map<String, Supplier<Object>> settings = new LinkedHashMap<>();
    settings.put("APP_ENVIRONMENT", () -> "test");
    settings.put("DB_URL", POSTGRES::getJdbcUrl);
    settings.put("DB_USER", () -> DB_USER);
    settings.put("POSTGRES_PASSWORD", () -> DB_PASSWORD);
    settings.put("SMTP_HOST", MAILPIT::getHost);
    settings.put("SMTP_PORT", () -> String.valueOf(MAILPIT.getMappedPort(1025)));
    settings.put("SMTP_TLS", () -> "false");
    settings.put("MAIL_FROM", () -> "registration@test.local");
    settings.put("ORGANIZER_EMAILS", () -> String.join(",", ORGANIZER_EMAILS));
    settings.put("ORGANIZER_USERNAME", () -> ORGANIZER_USERNAME);
    settings.put("ORGANIZER_PASSWORD", () -> ORGANIZER_PASSWORD);
    settings.put("CONFERENCE_CONFIG_PATH", () -> testConfigPath("conference-test.json"));
    settings.put("JSON_COPY_DIR", () -> JSON_COPY_DIR.toAbsolutePath().toString());
    settings.put("CAPTCHA_TEST_MODE", () -> "true");
    settings.put("RECAPTCHA_SITE_KEY", () -> "");
    settings.put("RECAPTCHA_SECRET_KEY", () -> "");
    settings.put("ORGANIZER_HTTPS_ONLY", () -> "true");
    settings.put("RATE_LIMIT_PER_MINUTE", () -> "100000");
    overrides.forEach((key, value) -> settings.put(key, () -> value));
    settings.forEach(registry::add);
  }

  static String testConfigPath(String fileName) {
    try {
      return Paths.get(AcceptanceTestSupport.class.getResource("/acceptance/" + fileName).toURI())
          .toAbsolutePath()
          .toString();
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  @BeforeEach
  void cleanState() throws Exception {
    try (Connection c = db();
        Statement s = c.createStatement();
        ResultSet rs =
            s.executeQuery(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'registration'")) {
      rs.next();
      if (rs.getInt(1) > 0) {
        s.execute("TRUNCATE registration CASCADE");
      }
    }
    if (!Files.isDirectory(JSON_COPY_DIR)) {
      Files.deleteIfExists(JSON_COPY_DIR);
      Files.createDirectories(JSON_COPY_DIR);
    }
    try (Stream<Path> files = Files.list(JSON_COPY_DIR)) {
      for (Path f : files.toList()) {
        Files.delete(f);
      }
    }
    mailpit("DELETE", "/api/v1/messages");
  }

  // ---- requests ----

  static Map<String, Object> validExternal() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "EXTERNAL");
    body.put("firstName", "Ana");
    body.put("lastName", "Novak");
    body.put("email", "ana.novak@example.org");
    body.put("organization", "Example d.o.o.");
    body.put("studyInstitution", null);
    body.put("studyProgramme", null);
    body.put("studentId", null);
    body.put("options", new ArrayList<>(List.of("ws-ai", "ev-gala")));
    body.put("consents", new ArrayList<>(List.of("privacy")));
    body.put("captchaToken", "test-pass");
    return body;
  }

  static Map<String, Object> validStudent() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "STUDENT");
    body.put("firstName", "Luka");
    body.put("lastName", "Horvat");
    body.put("email", "luka.horvat@student.example.org");
    body.put("organization", null);
    body.put("studyInstitution", "University of Ljubljana");
    body.put("studyProgramme", "Computer Science");
    body.put("studentId", "63210001");
    body.put("options", new ArrayList<>(List.of("ws-ai", "ev-student")));
    body.put("consents", new ArrayList<>(List.of("privacy")));
    body.put("captchaToken", "test-pass");
    return body;
  }

  String url(String path) {
    return "http://127.0.0.1:" + port + path;
  }

  HttpResponse<String> postRegistration(Object body) throws Exception {
    return postJson("/api/registrations", json.writeValueAsString(body));
  }

  HttpResponse<String> postJson(String path, String body) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(url(path)))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/json; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .build();
    return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  HttpResponse<String> get(String path, String... headers) throws Exception {
    return http.send(getRequest(path, headers), HttpResponse.BodyHandlers.ofString());
  }

  HttpResponse<byte[]> getBytes(String path, String... headers) throws Exception {
    return http.send(getRequest(path, headers), HttpResponse.BodyHandlers.ofByteArray());
  }

  private HttpRequest getRequest(String path, String... headers) {
    HttpRequest.Builder b =
        HttpRequest.newBuilder(URI.create(url(path))).timeout(Duration.ofSeconds(60)).GET();
    if (headers.length > 0) {
      b.headers(headers);
    }
    return b.build();
  }

  static String basicAuth(String user, String password) {
    return "Basic "
        + Base64.getEncoder()
            .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
  }

  JsonNode jsonBody(HttpResponse<String> response) {
    return json.readTree(response.body());
  }

  /** Asserts HTTP 201 and returns the new registration id. */
  String createdId(HttpResponse<String> response) {
    assertThat(response.statusCode()).as("status, body: %s", response.body()).isEqualTo(201);
    String id = jsonBody(response).path("id").asString();
    assertThat(id).isNotBlank();
    return id;
  }

  /** Field names of a 400 response body {"errors": [{"field", "message"}]}. */
  List<String> errorFields(HttpResponse<String> response) {
    assertThat(response.statusCode()).as("status, body: %s", response.body()).isEqualTo(400);
    JsonNode errors = jsonBody(response).path("errors");
    assertThat(errors.isArray()).as("errors array in %s", response.body()).isTrue();
    List<String> fields = new ArrayList<>();
    for (JsonNode e : errors) {
      assertThat(e.path("message").asString()).isNotBlank();
      fields.add(e.path("field").asString());
    }
    return fields;
  }
}
