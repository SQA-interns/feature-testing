package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Submissions;

/** US-008 Registration export, through GET /api/registrations/export. */
class RegistrationExportAcceptanceTest extends AcceptanceTestBase {

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry registry) {
    register(registry, Map.of());
  }

  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private String register(Map<String, Object> body) {
    HttpResponse<String> response = postRegistration(body);
    assertThat(response.statusCode()).as("setup registration: %s", response.body()).isEqualTo(201);
    return json(response).path("id").asString();
  }

  private HttpResponse<byte[]> exportAsOrganizer() {
    return getExport(
        exportRequest().header("Authorization", basicAuth(ORGANIZER_USERNAME, ORGANIZER_PASSWORD)));
  }

  /** Rows of the first sheet as header-name to cell-text maps. */
  private static List<Map<String, String>> rows(byte[] xlsx) throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      Sheet sheet = workbook.getSheetAt(0);
      DataFormatter text = new DataFormatter();
      Row header = sheet.getRow(0);
      List<String> names = new ArrayList<>();
      for (Cell c : header) {
        names.add(text.formatCellValue(c));
      }
      List<Map<String, String>> result = new ArrayList<>();
      for (int r = 1; r <= sheet.getLastRowNum(); r++) {
        Row row = sheet.getRow(r);
        if (row == null) {
          continue;
        }
        Map<String, String> values = new java.util.LinkedHashMap<>();
        for (int c = 0; c < names.size(); c++) {
          Cell cell = row.getCell(c);
          values.put(names.get(c), cell == null ? "" : text.formatCellValue(cell));
        }
        result.add(values);
      }
      return result;
    }
  }

  @Test
  @DisplayName("AC-008-01 organizer gets an xlsx with one row per registration and all its data")
  void ac_008_01_organizerExportsOneRowPerRegistration() throws Exception {
    String externalId = register(Submissions.validExternal());
    String studentId = register(Submissions.validStudent());

    HttpResponse<byte[]> response = exportAsOrganizer();

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(v -> assertThat(v).startsWith(XLSX));
    List<Map<String, String>> rows = rows(response.body());
    assertThat(rows).hasSize(2);

    Map<String, String> external =
        rows.stream().filter(r -> r.containsValue(externalId)).findFirst().orElseThrow();
    assertThat(external.values())
        .contains(
            "EXTERNAL",
            "Ana",
            "Novak",
            "ana.novak@example.org",
            "Example Institute",
            "Workshop: Testing in practice",
            "Conference dinner");
    Map<String, String> student =
        rows.stream().filter(r -> r.containsValue(studentId)).findFirst().orElseThrow();
    assertThat(student.values())
        .contains(
            "STUDENT",
            "Luka",
            "Kranjc",
            "luka.kranjc@student.example.org",
            "University of Ljubljana",
            "Computer Science",
            "63200001",
            "Workshop: Testing in practice",
            "Lunch (vegetarian)");
    for (Map<String, String> row : List.of(external, student)) {
      assertThat(row.get("Registered at")).as("registration time").isNotBlank();
      assertThat(row.get("Consents")).as("consents").contains("privacy");
    }
  }

  static Stream<Arguments> badCredentials() {
    return Stream.of(
        Arguments.of("no credentials", null),
        Arguments.of("wrong password", basicAuth(ORGANIZER_USERNAME, "wrong-password-123456")),
        Arguments.of("wrong username", basicAuth("someone", ORGANIZER_PASSWORD)));
  }

  @ParameterizedTest(name = "AC-008-02 export refused with {0}")
  @MethodSource("badCredentials")
  void ac_008_02_exportWithoutValidCredentialsIsRefused(String variant, String authorization) {
    String id = register(Submissions.validExternal());
    var request = exportRequest();
    if (authorization != null) {
      request.header("Authorization", authorization);
    }

    HttpResponse<byte[]> response = getExport(request);

    assertThat(response.statusCode()).isEqualTo(401);
    String body = new String(response.body(), StandardCharsets.UTF_8);
    assertThat(body).doesNotContain(id, "Ana", "Novak", "ana.novak@example.org");
    assertThat(response.headers().firstValue("Content-Type").orElse("")).doesNotStartWith(XLSX);
  }

  @Test
  @DisplayName("AC-008-03 Slovenian characters appear unchanged in the workbook cells")
  void ac_008_03_slovenianCharactersAreUnchangedInWorkbook() throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    body.put("firstName", "Čedomir Žiga");
    body.put("lastName", "Šuštaršič");
    body.put("organization", "Inštitut za ŽČŠ raziskave, Črnomelj");
    String id = register(body);

    HttpResponse<byte[]> response = exportAsOrganizer();

    assertThat(response.statusCode()).isEqualTo(200);
    Map<String, String> row =
        rows(response.body()).stream().filter(r -> r.containsValue(id)).findFirst().orElseThrow();
    assertThat(row.values())
        .contains("Čedomir Žiga", "Šuštaršič", "Inštitut za ŽČŠ raziskave, Črnomelj");
  }

  @Test
  @DisplayName(
      "AC-008-04 valid credentials over plain HTTP from a non-localhost client are refused")
  void ac_008_04_plainHttpFromRemoteClientIsRefused() {
    String id = register(Submissions.validExternal());
    String credentials = basicAuth(ORGANIZER_USERNAME, ORGANIZER_PASSWORD);

    HttpResponse<byte[]> overHttps =
        getExport(
            exportRequest()
                .header("Authorization", credentials)
                .header("X-Forwarded-For", "203.0.113.10")
                .header("X-Forwarded-Proto", "https"));
    assertThat(overHttps.statusCode())
        .as("control: the same remote client over HTTPS (via the proxy) is served")
        .isEqualTo(200);

    HttpResponse<byte[]> overHttp =
        getExport(
            exportRequest()
                .header("Authorization", credentials)
                .header("X-Forwarded-For", "203.0.113.10")
                .header("X-Forwarded-Proto", "http"));

    assertThat(overHttp.statusCode()).as("refused").isBetween(400, 499);
    String body = new String(overHttp.body(), StandardCharsets.UTF_8);
    assertThat(body).doesNotContain(id, "Ana", "Novak", "ana.novak@example.org");
    assertThat(overHttp.headers().firstValue("Content-Type").orElse("")).doesNotStartWith(XLSX);
  }
}
