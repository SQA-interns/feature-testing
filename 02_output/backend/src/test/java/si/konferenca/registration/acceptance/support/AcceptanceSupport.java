package si.konferenca.registration.acceptance.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
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
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Black-box harness for the backend acceptance tests: shares a PostgreSQL and a Mailpit container,
 * talks to the application only through HTTP, and observes the database schema and JSON copy
 * directory fixed by the specification (section 7). Subclasses say where the application runs.
 */
public abstract class AcceptanceSupport {

  public static final String ORGANIZER_USERNAME = "organizer";
  public static final String ORGANIZER_PASSWORD = "acceptance-organizer-password-42";
  public static final String ORGANIZER_EMAIL_1 = "org1@organizers.example";
  public static final String ORGANIZER_EMAIL_2 = "org2@organizers.example";
  public static final String CONFERENCE_NAME = "Acceptance Conference 2026";
  public static final String PRIVACY_TEXT =
      "I agree that my personal data is processed for organizing the conference and that it is"
          + " shared with the conference organizers.";

  /** Options of the test configuration: active ones, one inactive, one only for externals. */
  public static final String TEST_CONFIG =
      """
      {
        "options": [
          {"id": "ws-ai", "name": "AI workshop", "category": "WORKSHOP"},
          {"id": "ws-cloud", "name": "Cloud workshop", "category": "WORKSHOP",
           "availableTo": ["EXTERNAL"]},
          {"id": "ev-gala", "name": "Gala evening", "category": "EVENT",
           "availableTo": ["EXTERNAL", "STUDENT"]},
          {"id": "meal-lunch", "name": "Lunch", "category": "MEAL", "active": true},
          {"id": "meal-veg", "name": "Vegetarian lunch", "category": "MEAL", "active": false},
          {"id": "other-tour", "name": "City tour", "category": "OTHER"}
        ],
        "consents": [
          {"id": "privacy", "text": "%s", "mandatory": true},
          {"id": "photo", "text": "Photos of me may be published.", "mandatory": false}
        ]
      }
      """
          .formatted(PRIVACY_TEXT);

  public static final JsonMapper JSON = JsonMapper.builder().build();

  protected static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:16.15-alpine"))
          .withDatabaseName("registration")
          .withUsername("registration")
          .withPassword("acceptance-db-password");

  @SuppressWarnings("resource")
  protected static final GenericContainer<?> MAILPIT =
      new GenericContainer<>(DockerImageName.parse("axllent/mailpit:v1.31.1"))
          .withExposedPorts(1025, 8025);

  protected static final Path WORK_DIR;
  protected static final Path JSON_COPY_DIR;
  protected static final Path CONFIG_FILE;

  static {
    POSTGRES.start();
    MAILPIT.start();
    try {
      WORK_DIR = Files.createTempDirectory("registration-acceptance");
      JSON_COPY_DIR = Files.createDirectories(WORK_DIR.resolve("json-copies"));
      CONFIG_FILE = WORK_DIR.resolve("conference-config.json");
      Files.writeString(CONFIG_FILE, TEST_CONFIG, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  protected static final Mailpit MAIL =
      new Mailpit("http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025));

  private static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  /** The settings every application instance of the acceptance tests starts with. */
  public static Map<String, String> baseSettings() {
    Map<String, String> settings = new LinkedHashMap<>();
    settings.put("spring.datasource.url", POSTGRES.getJdbcUrl());
    settings.put("spring.datasource.username", POSTGRES.getUsername());
    settings.put("spring.datasource.password", POSTGRES.getPassword());
    settings.put("spring.mail.host", MAILPIT.getHost());
    settings.put("spring.mail.port", String.valueOf(MAILPIT.getMappedPort(1025)));
    settings.put("CAPTCHA_TEST_MODE", "true");
    settings.put("ORGANIZER_USERNAME", ORGANIZER_USERNAME);
    settings.put("ORGANIZER_PASSWORD", ORGANIZER_PASSWORD);
    settings.put("ORGANIZER_EMAILS", ORGANIZER_EMAIL_1 + "," + ORGANIZER_EMAIL_2);
    settings.put("ORGANIZER_HTTPS_ONLY", "false");
    settings.put("RATE_LIMIT_PER_MINUTE", "100000");
    settings.put("JSON_COPY_DIR", JSON_COPY_DIR.toString());
    settings.put("CONFERENCE_CONFIG_FILE", CONFIG_FILE.toString());
    settings.put("CONFERENCE_NAME", CONFERENCE_NAME);
    settings.put("MAIL_FROM", "registration@conference.example");
    return settings;
  }

  // ---------------------------------------------------------------- HTTP

  /** Base URL of the application instance under test. */
  protected abstract String baseUrl();

  public static HttpResponse<byte[]> send(HttpRequest request) {
    try {
      return HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  protected HttpResponse<byte[]> get(String path) {
    return send(HttpRequest.newBuilder(URI.create(baseUrl() + path)).GET().build());
  }

  protected HttpResponse<byte[]> getAsOrganizer(String path) {
    return getWithBasic(path, ORGANIZER_USERNAME, ORGANIZER_PASSWORD);
  }

  protected HttpResponse<byte[]> getWithBasic(String path, String user, String password) {
    return send(
        HttpRequest.newBuilder(URI.create(baseUrl() + path))
            .header("Authorization", basic(user, password))
            .GET()
            .build());
  }

  public static String basic(String user, String password) {
    return "Basic "
        + Base64.getEncoder()
            .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
  }

  protected HttpResponse<byte[]> postRegistration(JsonNode body) {
    return send(
        HttpRequest.newBuilder(URI.create(baseUrl() + "/api/registrations"))
            .header("Content-Type", "application/json; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
            .build());
  }

  /** Posts the submission, asserts HTTP 201 and returns the new registration id. */
  protected String register(JsonNode body) {
    HttpResponse<byte[]> response = postRegistration(body);
    assertThat(response.statusCode()).as(text(response)).isEqualTo(201);
    String id = json(response).path("id").asString();
    assertThat(id).isNotBlank();
    return id;
  }

  public static String text(HttpResponse<byte[]> response) {
    return new String(response.body(), StandardCharsets.UTF_8);
  }

  public static JsonNode json(HttpResponse<byte[]> response) {
    return JSON.readTree(response.body());
  }

  public static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  // ---------------------------------------------------------------- submissions

  /** A random lower-case token of letters only, unique enough to tag test data. */
  public static String token() {
    StringBuilder sb = new StringBuilder("zq");
    for (int i = 0; i < 10; i++) {
      sb.append((char) ('a' + ThreadLocalRandom.current().nextInt(26)));
    }
    return sb.toString();
  }

  public static String uniqueEmail() {
    return "p." + UUID.randomUUID().toString().substring(0, 13) + "@participants.example";
  }

  /** A valid external submission with one option and the mandatory consent. */
  public static ObjectNode validExternal() {
    ObjectNode body = JSON.createObjectNode();
    body.put("type", "EXTERNAL");
    body.put("firstName", "Ana");
    body.put("lastName", "Novak");
    body.put("email", uniqueEmail());
    body.put("organization", "Example d.o.o.");
    body.putNull("studyInstitution");
    body.putNull("studyProgramme");
    body.putNull("studentId");
    body.putArray("options").add("ws-ai");
    body.putArray("consents").add("privacy");
    body.put("captchaToken", "test-pass");
    return body;
  }

  /** A valid student submission with one option and the mandatory consent. */
  public static ObjectNode validStudent() {
    ObjectNode body = JSON.createObjectNode();
    body.put("type", "STUDENT");
    body.put("firstName", "Luka");
    body.put("lastName", "Kranjc");
    body.put("email", uniqueEmail());
    body.putNull("organization");
    body.put("studyInstitution", "University of Ljubljana");
    body.put("studyProgramme", "Computer Science");
    body.put("studentId", "63200001");
    body.putArray("options").add("ws-ai");
    body.putArray("consents").add("privacy");
    body.put("captchaToken", "test-pass");
    return body;
  }

  public static ObjectNode options(ObjectNode body, String... ids) {
    var array = body.putArray("options");
    for (String id : ids) {
      array.add(id);
    }
    return body;
  }

  // ---------------------------------------------------------------- rejection checks

  /** Asserts HTTP 400 with an error entry for the given field. */
  public static void assertFieldError(HttpResponse<byte[]> response, String field) {
    assertThat(response.statusCode()).as(text(response)).isEqualTo(400);
    List<String> fields = new ArrayList<>();
    json(response).path("errors").forEach(e -> fields.add(e.path("field").asString()));
    assertThat(fields).as(text(response)).contains(field);
    json(response)
        .path("errors")
        .forEach(e -> assertThat(e.path("message").asString()).as("message of " + e).isNotBlank());
  }

  /** Snapshot of everything a rejected submission must leave unchanged. */
  public record StoreSnapshot(long rows, long jsonCopies) {}

  public static StoreSnapshot snapshot() {
    return new StoreSnapshot(registrationRowCount(), jsonCopyCount());
  }

  /** Asserts no row, no JSON copy and no email to the given address since the snapshot. */
  public static void assertNothingCreated(StoreSnapshot before, String participantEmail) {
    assertThat(registrationRowCount()).as("registration rows").isEqualTo(before.rows());
    assertThat(jsonCopyCount()).as("JSON copies").isEqualTo(before.jsonCopies());
    sleep(Duration.ofMillis(500));
    assertThat(MAIL.messagesTo(participantEmail)).as("emails to " + participantEmail).isEmpty();
  }

  public static void sleep(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  // ---------------------------------------------------------------- JSON copies

  public static long jsonCopyCount() {
    try (Stream<Path> files = Files.list(JSON_COPY_DIR)) {
      return files.filter(p -> p.getFileName().toString().endsWith(".json")).count();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Path jsonCopy(String id) {
    return JSON_COPY_DIR.resolve(id + ".json");
  }

  // ---------------------------------------------------------------- database (spec section 7)

  public static Connection db() throws SQLException {
    return DriverManager.getConnection(
        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
  }

  public static long registrationRowCount() {
    try (Connection c = db();
        PreparedStatement ps = c.prepareStatement("select count(*) from registration");
        ResultSet rs = ps.executeQuery()) {
      rs.next();
      return rs.getLong(1);
    } catch (SQLException e) {
      // before the schema exists there are no registrations
      return 0;
    }
  }

  /** The registration row as column name to string value, or an empty map. */
  public static Map<String, Object> registrationRow(String id) throws SQLException {
    Map<String, Object> row = new LinkedHashMap<>();
    try (Connection c = db();
        PreparedStatement ps =
            c.prepareStatement(
                "select type, first_name, last_name, email, organization, study_institution,"
                    + " study_programme, student_id, registered_at from registration where id = ?")) {
      ps.setObject(1, UUID.fromString(id));
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          for (String col :
              List.of(
                  "type",
                  "first_name",
                  "last_name",
                  "email",
                  "organization",
                  "study_institution",
                  "study_programme",
                  "student_id")) {
            row.put(col, rs.getString(col));
          }
          row.put("registered_at", rs.getObject("registered_at", java.time.OffsetDateTime.class));
        }
      }
    }
    return row;
  }

  public static List<String> registrationOptions(String id) throws SQLException {
    List<String> ids = new ArrayList<>();
    try (Connection c = db();
        PreparedStatement ps =
            c.prepareStatement(
                "select option_id from registration_option where registration_id = ?"
                    + " order by position")) {
      ps.setObject(1, UUID.fromString(id));
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          ids.add(rs.getString(1));
        }
      }
    }
    return ids;
  }

  /** Consent id to the time it was given. */
  public static Map<String, java.time.OffsetDateTime> registrationConsents(String id)
      throws SQLException {
    Map<String, java.time.OffsetDateTime> consents = new LinkedHashMap<>();
    try (Connection c = db();
        PreparedStatement ps =
            c.prepareStatement(
                "select consent_id, given_at from registration_consent where registration_id = ?")) {
      ps.setObject(1, UUID.fromString(id));
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          consents.put(rs.getString(1), rs.getObject(2, java.time.OffsetDateTime.class));
        }
      }
    }
    return consents;
  }
}
