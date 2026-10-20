package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Probes of the acceptance tests into the systems the backend writes to: PostgreSQL (schema of
 * docs/02_contracts/database-schema.sql), the JSON copy directory and Mailpit (SMTP server). The
 * containers start once per test JVM.
 */
abstract class AcceptanceProbes {

  static final String DB_NAME = "registration";
  static final String DB_USER = "registration";
  static final String DB_PASSWORD = "registration-test";

  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:16.15-alpine"))
          .withDatabaseName(DB_NAME)
          .withUsername(DB_USER)
          .withPassword(DB_PASSWORD);

  static final GenericContainer<?> MAILPIT =
      new GenericContainer<>(DockerImageName.parse("axllent/mailpit:v1.31.1"))
          .withExposedPorts(1025, 8025)
          .waitingFor(Wait.forHttp("/api/v1/info").forPort(8025));

  static final Path JSON_COPY_DIR;

  static {
    POSTGRES.start();
    MAILPIT.start();
    try {
      JSON_COPY_DIR = Files.createTempDirectory("acceptance-json-copies");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static final Duration MAIL_WAIT = Duration.ofSeconds(15);

  final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  final JsonMapper json = JsonMapper.builder().build();

  /** "Rejected" (acceptance-criteria.md): no database row, no JSON copy, no email. */
  void assertNothingStored() throws Exception {
    assertThat(registrationCount()).as("registrations in the database").isZero();
    assertThat(jsonCopyCount()).as("JSON copies").isZero();
    Thread.sleep(500);
    assertThat(messages()).as("emails in Mailpit").isEmpty();
  }

  // ---- database (docs/02_contracts/database-schema.sql) ----

  static Connection db() throws SQLException {
    return DriverManager.getConnection(POSTGRES.getJdbcUrl(), DB_USER, DB_PASSWORD);
  }

  static int registrationCount() throws SQLException {
    try (Connection c = db();
        Statement s = c.createStatement();
        ResultSet rs =
            s.executeQuery(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'registration'")) {
      rs.next();
      if (rs.getInt(1) == 0) {
        return 0;
      }
    }
    try (Connection c = db();
        Statement s = c.createStatement();
        ResultSet rs = s.executeQuery("SELECT count(*) FROM registration")) {
      rs.next();
      return rs.getInt(1);
    }
  }

  /** Column name to value; text columns as String, registered_at as OffsetDateTime. */
  static Map<String, Object> registrationRow(String id) throws SQLException {
    try (Connection c = db();
        PreparedStatement ps =
            c.prepareStatement(
                "SELECT type, first_name, last_name, email, organization, study_institution,"
                    + " study_programme, student_id, registered_at FROM registration"
                    + " WHERE id = CAST(? AS uuid)")) {
      ps.setString(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        assertThat(rs.next()).as("registration row %s", id).isTrue();
        Map<String, Object> row = new LinkedHashMap<>();
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
        row.put("registered_at", rs.getObject("registered_at", OffsetDateTime.class));
        return row;
      }
    }
  }

  static List<String> optionIds(String id) throws SQLException {
    List<String> ids = new ArrayList<>();
    try (Connection c = db();
        PreparedStatement ps =
            c.prepareStatement(
                "SELECT option_id FROM registration_option WHERE registration_id = CAST(? AS uuid)"
                    + " ORDER BY option_id")) {
      ps.setString(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          ids.add(rs.getString(1));
        }
      }
    }
    return ids;
  }

  /** Consent id to the time it was given. */
  static Map<String, OffsetDateTime> consents(String id) throws SQLException {
    Map<String, OffsetDateTime> result = new LinkedHashMap<>();
    try (Connection c = db();
        PreparedStatement ps =
            c.prepareStatement(
                "SELECT consent_id, given_at FROM registration_consent"
                    + " WHERE registration_id = CAST(? AS uuid) ORDER BY consent_id")) {
      ps.setString(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result.put(rs.getString(1), rs.getObject(2, OffsetDateTime.class));
        }
      }
    }
    return result;
  }

  // ---- JSON copies (docs/02_contracts/registration-copy.schema.json) ----

  static Path jsonCopy(String id) {
    return JSON_COPY_DIR.resolve(id + ".json");
  }

  static long jsonCopyCount() throws IOException {
    if (!Files.isDirectory(JSON_COPY_DIR)) {
      return 0;
    }
    try (Stream<Path> files = Files.list(JSON_COPY_DIR)) {
      return files.filter(f -> f.getFileName().toString().endsWith(".json")).count();
    }
  }

  // ---- Mailpit API ----

  static String mailpitUrl(String path) {
    return "http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025) + path;
  }

  HttpResponse<byte[]> mailpit(String method, String path) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(mailpitUrl(path)))
            .timeout(Duration.ofSeconds(30))
            .method(method, HttpRequest.BodyPublishers.noBody())
            .build();
    HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
    assertThat(response.statusCode()).as("Mailpit %s %s", method, path).isEqualTo(200);
    return response;
  }

  /** Summaries of all messages in Mailpit. */
  List<JsonNode> messages() throws Exception {
    JsonNode list = json.readTree(mailpit("GET", "/api/v1/messages?limit=500").body());
    List<JsonNode> result = new ArrayList<>();
    for (JsonNode m : list.path("messages")) {
      result.add(m);
    }
    return result;
  }

  /** Waits until at least {@code count} messages arrived, then lets stragglers arrive too. */
  List<JsonNode> awaitMessages(int count) throws Exception {
    long deadline = System.nanoTime() + MAIL_WAIT.toNanos();
    while (messages().size() < count && System.nanoTime() < deadline) {
      Thread.sleep(200);
    }
    Thread.sleep(500);
    return messages();
  }

  /** Full message: From, To, Cc, Bcc, Subject, Text, HTML, Attachments. */
  JsonNode message(JsonNode summary) throws Exception {
    return json.readTree(mailpit("GET", "/api/v1/message/" + summary.path("ID").asString()).body());
  }

  /** Header name to values. */
  JsonNode headers(JsonNode summary) throws Exception {
    return json.readTree(
        mailpit("GET", "/api/v1/message/" + summary.path("ID").asString() + "/headers").body());
  }

  byte[] attachment(JsonNode fullMessage, JsonNode attachment) throws Exception {
    return mailpit(
            "GET",
            "/api/v1/message/"
                + fullMessage.path("ID").asString()
                + "/part/"
                + attachment.path("PartID").asString())
        .body();
  }

  /** Every address in To, Cc and Bcc of a message, lower case. */
  static List<String> recipients(JsonNode message) {
    List<String> result = new ArrayList<>();
    for (String field : List.of("To", "Cc", "Bcc")) {
      for (JsonNode a : message.path(field)) {
        result.add(a.path("Address").asString().toLowerCase(java.util.Locale.ROOT));
      }
    }
    return result;
  }

  /** Messages whose recipients include {@code address}. */
  static List<JsonNode> messagesTo(List<JsonNode> messages, String address) {
    return messages.stream().filter(m -> recipients(m).contains(address.toLowerCase())).toList();
  }
}
