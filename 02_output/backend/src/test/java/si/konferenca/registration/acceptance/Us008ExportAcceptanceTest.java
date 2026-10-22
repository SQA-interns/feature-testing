package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import tools.jackson.databind.node.ObjectNode;

/** US-008 Registration export (AC-008-01..03; AC-008-04 in OrganizerHttpsOnlyAcceptanceTest). */
class Us008ExportAcceptanceTest extends AcceptanceTestBase {

  static final List<String> COLUMNS =
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

  static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  /** The first sheet as rows of cell texts; row 0 is the header. */
  static List<List<String>> readWorkbook(byte[] bytes) throws Exception {
    List<List<String>> rows = new ArrayList<>();
    DataFormatter formatter = new DataFormatter();
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      Sheet sheet = workbook.getSheetAt(0);
      for (Row row : sheet) {
        List<String> cells = new ArrayList<>();
        for (int c = 0; c < COLUMNS.size(); c++) {
          Cell cell = row.getCell(c);
          cells.add(cell == null ? "" : formatter.formatCellValue(cell));
        }
        rows.add(cells);
      }
    }
    return rows;
  }

  /** Data rows keyed by the id column, as column name to cell text. */
  static Map<String, Map<String, String>> rowsById(List<List<String>> rows) {
    Map<String, Map<String, String>> byId = new HashMap<>();
    for (List<String> row : rows.subList(1, rows.size())) {
      Map<String, String> cells = new HashMap<>();
      for (int c = 0; c < COLUMNS.size(); c++) {
        cells.put(COLUMNS.get(c), row.get(c));
      }
      byId.put(row.get(0), cells);
    }
    return byId;
  }

  @DisplayName("AC-008-01")
  @Test
  void ac_008_01_organizerGetsWorkbookWithOneRowPerRegistration() throws Exception {
    ObjectNode external = options(validExternal(), "ws-ai", "other-tour");
    external.putArray("consents").add("privacy").add("photo");
    String externalId = register(external);
    ObjectNode student = options(validStudent(), "meal-lunch");
    String studentId = register(student);

    HttpResponse<byte[]> response = getAsOrganizer("/api/export");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith(XLSX);
    List<List<String>> rows = readWorkbook(response.body());
    assertThat(rows.get(0)).containsExactlyElementsOf(COLUMNS);
    Map<String, Map<String, String>> byId = rowsById(rows);
    assertThat(rows.size() - 1).as("one row per registration").isEqualTo(byId.size());
    assertThat((long) byId.size()).isEqualTo(registrationRowCount());

    Map<String, String> e = byId.get(externalId);
    assertThat(e).as("row of " + externalId).isNotNull();
    assertThat(e.get("type")).isEqualTo("EXTERNAL");
    assertThat(e.get("firstName")).isEqualTo("Ana");
    assertThat(e.get("lastName")).isEqualTo("Novak");
    assertThat(e.get("email")).isEqualTo(external.path("email").asString());
    assertThat(e.get("organization")).isEqualTo("Example d.o.o.");
    assertThat(e.get("studyInstitution")).isEmpty();
    assertThat(e.get("studyProgramme")).isEmpty();
    assertThat(e.get("studentId")).isEmpty();
    assertThat(Arrays.asList(e.get("options").split(", ")))
        .containsExactlyInAnyOrder("ws-ai", "other-tour");
    assertThat(Arrays.asList(e.get("consents").split(", ")))
        .containsExactlyInAnyOrder("privacy", "photo");
    assertThat(OffsetDateTime.parse(e.get("registeredAt"))).isNotNull();

    Map<String, String> s = byId.get(studentId);
    assertThat(s).as("row of " + studentId).isNotNull();
    assertThat(s.get("type")).isEqualTo("STUDENT");
    assertThat(s.get("organization")).isEmpty();
    assertThat(s.get("studyInstitution")).isEqualTo("University of Ljubljana");
    assertThat(s.get("studyProgramme")).isEqualTo("Computer Science");
    assertThat(s.get("studentId")).isEqualTo("63200001");
    assertThat(s.get("options")).isEqualTo("meal-lunch");
    assertThat(s.get("consents")).isEqualTo("privacy");
  }

  private void assertNoRegistrationData(HttpResponse<byte[]> response, String email) {
    byte[] body = response.body();
    assertThat(body.length >= 2 && body[0] == 'P' && body[1] == 'K')
        .as("no workbook in the response")
        .isFalse();
    assertThat(text(response)).doesNotContain(email);
  }

  @DisplayName("AC-008-02")
  @Test
  void ac_008_02_exportWithoutOrWithWrongCredentialsIsRefused() {
    ObjectNode body = validExternal();
    String email = body.path("email").asString();
    register(body);

    HttpResponse<byte[]> anonymous = get("/api/export");
    HttpResponse<byte[]> wrongPassword =
        getWithBasic("/api/export", ORGANIZER_USERNAME, ORGANIZER_PASSWORD + "x");
    HttpResponse<byte[]> wrongUser = getWithBasic("/api/export", "someone", ORGANIZER_PASSWORD);

    for (HttpResponse<byte[]> response : List.of(anonymous, wrongPassword, wrongUser)) {
      assertThat(response.statusCode()).isEqualTo(401);
      assertNoRegistrationData(response, email);
    }
    assertThat(getAsOrganizer("/api/export").statusCode()).isEqualTo(200);
  }

  @DisplayName("AC-008-03")
  @Test
  void ac_008_03_slovenianCharactersAreUnchangedInTheWorkbook() throws Exception {
    ObjectNode body = validExternal();
    body.put("firstName", "Čedomir Šžč");
    body.put("lastName", "Žužek-ČŠŽ");
    body.put("organization", "Fakulteta za računalništvo Čšž");
    String id = register(body);

    HttpResponse<byte[]> response = getAsOrganizer("/api/export");

    assertThat(response.statusCode()).isEqualTo(200);
    Map<String, String> row = rowsById(readWorkbook(response.body())).get(id);
    assertThat(row).isNotNull();
    assertThat(row.get("firstName")).isEqualTo("Čedomir Šžč");
    assertThat(row.get("lastName")).isEqualTo("Žužek-ČŠŽ");
    assertThat(row.get("organization")).isEqualTo("Fakulteta za računalništvo Čšž");
  }
}
