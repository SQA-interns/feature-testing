package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static si.konferenca.registration.acceptance.support.Submissions.validExternal;
import static si.konferenca.registration.acceptance.support.Submissions.validStudent;
import static si.konferenca.registration.acceptance.support.Submissions.with;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.HttpApi;
import si.konferenca.registration.acceptance.support.Settings;
import si.konferenca.registration.acceptance.support.StandardAcceptanceTest;

/** US-008 Registration export, through {@code GET /api/export}. */
class Us008ExportAcceptanceTest extends StandardAcceptanceTest {

  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private static final List<String> COLUMNS =
      List.of(
          "id",
          "type",
          "firstName",
          "lastName",
          "email",
          "organization",
          "studyInstitution",
          "studyProgramme",
          "studentId",
          "options",
          "consents",
          "registeredAt");

  @Test
  @DisplayName("AC-008-01 the organizer export is a workbook with one row per registration")
  void ac008_01_exportIsAWorkbookWithOneRowPerRegistration() throws IOException {
    UUID external =
        registerAccepted(
            with(
                validExternal(),
                "options",
                List.of("ws-ai", "ev-dinner"),
                "consents",
                List.of("privacy", "photos")));
    UUID student = registerAccepted(validStudent());

    HttpApi.Response response = api.getAsOrganizer("/api/export");

    assertThat(response.status()).as("response status").isEqualTo(200);
    assertThat(response.header("Content-Type")).startsWith(XLSX);
    List<List<String>> rows = rows(response.body());
    assertThat(rows.getFirst()).isEqualTo(COLUMNS);
    assertThat(rows).hasSize(3);

    List<String> externalRow = rowOf(rows, external);
    assertThat(cell(externalRow, "type")).isEqualTo("EXTERNAL");
    assertThat(cell(externalRow, "firstName")).isEqualTo("Ana");
    assertThat(cell(externalRow, "lastName")).isEqualTo("Novak");
    assertThat(cell(externalRow, "email")).isEqualTo("ana.novak@example.org");
    assertThat(cell(externalRow, "organization")).isEqualTo("Example d.o.o.");
    assertThat(cell(externalRow, "studyInstitution")).isEmpty();
    assertThat(cell(externalRow, "studyProgramme")).isEmpty();
    assertThat(cell(externalRow, "studentId")).isEmpty();
    assertThat(cell(externalRow, "options").split(", "))
        .containsExactlyInAnyOrder("ws-ai", "ev-dinner");
    assertThat(cell(externalRow, "consents").split(", "))
        .containsExactlyInAnyOrder("privacy", "photos");
    assertThat(OffsetDateTime.parse(cell(externalRow, "registeredAt")).toInstant())
        .isCloseTo(db.row(external).registeredAt().toInstant(), within(1, ChronoUnit.SECONDS));

    List<String> studentRow = rowOf(rows, student);
    assertThat(cell(studentRow, "type")).isEqualTo("STUDENT");
    assertThat(cell(studentRow, "organization")).isEmpty();
    assertThat(cell(studentRow, "studyInstitution")).isEqualTo("Univerza v Ljubljani");
    assertThat(cell(studentRow, "studyProgramme")).isEqualTo("Computer Science");
    assertThat(cell(studentRow, "studentId")).isEqualTo("63210001");
    assertThat(cell(studentRow, "options").split(", "))
        .containsExactlyInAnyOrder("ws-ai", "meal-lunch");
    assertThat(cell(studentRow, "consents")).isEqualTo("privacy");
  }

  @Test
  @DisplayName("AC-008-02 the export without or with wrong credentials is refused with 401")
  void ac008_02_exportWithoutValidCredentialsIsRefused() {
    registerAccepted(validExternal());
    assertThat(api.getAsOrganizer("/api/export").status())
        .as("control: valid credentials are accepted")
        .isEqualTo(200);

    List<HttpApi.Response> refused =
        List.of(
            api.get("/api/export"),
            api.get(
                "/api/export",
                "Authorization",
                HttpApi.basic(Settings.ORGANIZER_USERNAME, "wrong-password-123456")),
            api.get(
                "/api/export",
                "Authorization",
                HttpApi.basic("someone-else", Settings.ORGANIZER_PASSWORD)));
    for (HttpApi.Response response : refused) {
      assertThat(response.status()).isEqualTo(401);
      assertNoRegistrationData(response);
    }
  }

  @Test
  @DisplayName("AC-008-03 Slovenian characters appear unchanged in the workbook cells")
  void ac008_03_slovenianCharactersAreUnchanged() throws IOException {
    String lastName = "Šuštaršič-Žužek čšž";
    String organization = "Društvo čebelarjev Šentjur ČŠŽ";
    UUID id =
        registerAccepted(with(validExternal(), "lastName", lastName, "organization", organization));

    List<String> row = rowOf(rows(api.getAsOrganizer("/api/export").body()), id);

    assertThat(cell(row, "lastName")).isEqualTo(lastName);
    assertThat(cell(row, "organization")).isEqualTo(organization);
  }

  @Test
  @DisplayName(
      "AC-008-04 with HTTPS-only on, the export over plain HTTP from a remote client is refused")
  void ac008_04_plainHttpFromRemoteClientIsRefused() {
    registerAccepted(validExternal());

    HttpApi.Response plain = api.getAsOrganizer("/api/export", "X-Forwarded-For", "203.0.113.7");
    HttpApi.Response secure =
        api.getAsOrganizer(
            "/api/export", "X-Forwarded-For", "203.0.113.7", "X-Forwarded-Proto", "https");

    assertThat(plain.status()).as("plain HTTP from a remote client").isBetween(400, 499);
    assertNoRegistrationData(plain);
    assertThat(secure.status()).as("control: the same client over HTTPS").isEqualTo(200);
  }

  private static void assertNoRegistrationData(HttpApi.Response response) {
    assertThat(response.header("Content-Type")).doesNotStartWith(XLSX);
    assertThat(response.text()).doesNotContain("Novak", "ana.novak@example.org");
  }

  private static List<List<String>> rows(byte[] workbook) throws IOException {
    DataFormatter formatter = new DataFormatter();
    List<List<String>> rows = new ArrayList<>();
    try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(workbook))) {
      Sheet sheet = book.getSheetAt(0);
      for (Row row : sheet) {
        List<String> cells = new ArrayList<>();
        for (int i = 0; i < COLUMNS.size(); i++) {
          cells.add(row.getCell(i) == null ? "" : formatter.formatCellValue(row.getCell(i)));
        }
        rows.add(cells);
      }
    }
    return rows;
  }

  private static List<String> rowOf(List<List<String>> rows, UUID id) {
    return rows.stream()
        .filter(r -> r.getFirst().equals(id.toString()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no row for " + id + " in " + Arrays.asList(rows)));
  }

  private static String cell(List<String> row, String column) {
    return row.get(COLUMNS.indexOf(column));
  }
}
