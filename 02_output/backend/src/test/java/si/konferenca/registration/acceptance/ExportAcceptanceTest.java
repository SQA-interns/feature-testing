package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** US-008 registration export: AC-008-01 … AC-008-04. */
class ExportAcceptanceTest extends DefaultContextAcceptanceTest {

  static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  static final List<String> HEADERS =
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

  String organizerAuth() {
    return basicAuth(ORGANIZER_USERNAME, ORGANIZER_PASSWORD);
  }

  /** Header name to cell text for each data row of the first sheet, keyed by id. */
  static Map<String, Map<String, String>> rows(byte[] workbook) throws Exception {
    try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(workbook))) {
      Sheet sheet = wb.getSheetAt(0);
      DataFormatter format = new DataFormatter();
      Row header = sheet.getRow(0);
      List<String> names = new ArrayList<>();
      for (int c = 0; c < HEADERS.size(); c++) {
        names.add(format.formatCellValue(header.getCell(c)));
      }
      assertThat(names).isEqualTo(HEADERS);
      Map<String, Map<String, String>> result = new LinkedHashMap<>();
      for (int r = 1; r <= sheet.getLastRowNum(); r++) {
        Row row = sheet.getRow(r);
        if (row == null) {
          continue;
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (int c = 0; c < HEADERS.size(); c++) {
          values.put(HEADERS.get(c), format.formatCellValue(row.getCell(c)));
        }
        result.put(values.get("id"), values);
      }
      return result;
    }
  }

  static List<String> split(String joined) {
    return joined.isEmpty() ? List.of() : Arrays.asList(joined.split(", "));
  }

  @Test
  @DisplayName("AC-008-01 an organizer exports a workbook with one row per registration")
  void ac_008_01_organizerExportsWorkbook() throws Exception {
    Map<String, Object> external = validExternal();
    external.put("consents", List.of("privacy", "photos"));
    String externalId = createdId(postRegistration(external));
    String studentId = createdId(postRegistration(validStudent()));

    HttpResponse<byte[]> response = getBytes("/api/export", "Authorization", organizerAuth());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith(XLSX);
    Map<String, Map<String, String>> rows = rows(response.body());
    assertThat(rows.keySet()).containsExactlyInAnyOrder(externalId, studentId);

    Map<String, String> ext = rows.get(externalId);
    assertThat(ext.get("type")).isEqualTo("EXTERNAL");
    assertThat(ext.get("firstName")).isEqualTo("Ana");
    assertThat(ext.get("lastName")).isEqualTo("Novak");
    assertThat(ext.get("email")).isEqualTo("ana.novak@example.org");
    assertThat(ext.get("organization")).isEqualTo("Example d.o.o.");
    assertThat(ext.get("studyInstitution")).isEmpty();
    assertThat(ext.get("studyProgramme")).isEmpty();
    assertThat(ext.get("studentId")).isEmpty();
    assertThat(split(ext.get("options"))).containsExactlyInAnyOrder("ws-ai", "ev-gala");
    assertThat(split(ext.get("consents"))).containsExactlyInAnyOrder("privacy", "photos");
    assertThat(OffsetDateTime.parse(ext.get("registeredAt"))).isNotNull();

    Map<String, String> stu = rows.get(studentId);
    assertThat(stu.get("type")).isEqualTo("STUDENT");
    assertThat(stu.get("organization")).isEmpty();
    assertThat(stu.get("studyInstitution")).isEqualTo("University of Ljubljana");
    assertThat(stu.get("studyProgramme")).isEqualTo("Computer Science");
    assertThat(stu.get("studentId")).isEqualTo("63210001");
    assertThat(split(stu.get("options"))).containsExactlyInAnyOrder("ws-ai", "ev-student");
    assertThat(split(stu.get("consents"))).containsExactly("privacy");
    assertThat(OffsetDateTime.parse(stu.get("registeredAt"))).isNotNull();
  }

  @Test
  @DisplayName("AC-008-01 with no registrations the workbook holds only the header row")
  void ac_008_01_emptyExportHasHeaderRow() throws Exception {
    HttpResponse<byte[]> response = getBytes("/api/export", "Authorization", organizerAuth());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(rows(response.body())).isEmpty();
  }

  @ParameterizedTest(name = "AC-008-02 export with {0} is refused (401) without data")
  @ValueSource(strings = {"no credentials", "wrong password", "wrong user"})
  void ac_008_02_exportWithoutValidCredentialsIsRefused(String variant) throws Exception {
    createdId(postRegistration(validExternal()));
    String[] headers =
        switch (variant) {
          case "no credentials" -> new String[0];
          case "wrong password" ->
              new String[] {"Authorization", basicAuth(ORGANIZER_USERNAME, "wrong-password")};
          default -> new String[] {"Authorization", basicAuth("intruder", ORGANIZER_PASSWORD)};
        };

    HttpResponse<byte[]> response = getBytes("/api/export", headers);

    assertThat(response.statusCode()).isEqualTo(401);
    assertThat(response.headers().firstValue("Content-Type").orElse("")).doesNotStartWith(XLSX);
    String body = new String(response.body(), StandardCharsets.UTF_8);
    assertThat(body).doesNotContain("ana.novak@example.org").doesNotContain("Novak");
  }

  @Test
  @DisplayName("AC-008-03 Slovenian characters appear unchanged in the workbook cells")
  void ac_008_03_slovenianCharactersAreExportedUnchanged() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("firstName", "Žiga Črtomir");
    body.put("lastName", "Šuštaršič-Čeh");
    body.put("organization", "Fakulteta za računalništvo, ČŠŽ čšž d.o.o.");
    String id = createdId(postRegistration(body));

    HttpResponse<byte[]> response = getBytes("/api/export", "Authorization", organizerAuth());

    assertThat(response.statusCode()).isEqualTo(200);
    Map<String, String> row = rows(response.body()).get(id);
    assertThat(row.get("firstName")).isEqualTo("Žiga Črtomir");
    assertThat(row.get("lastName")).isEqualTo("Šuštaršič-Čeh");
    assertThat(row.get("organization")).isEqualTo("Fakulteta za računalništvo, ČŠŽ čšž d.o.o.");
  }

  @Test
  @DisplayName("AC-008-04 valid credentials over plain HTTP from a non-local client are refused")
  void ac_008_04_plainHttpFromRemoteClientIsRefused() throws Exception {
    createdId(postRegistration(validExternal()));

    // The request reaches the backend through a trusted local proxy that reports the client.
    HttpResponse<byte[]> plain =
        getBytes(
            "/api/export",
            "Authorization",
            organizerAuth(),
            "X-Forwarded-For",
            "203.0.113.7",
            "X-Forwarded-Proto",
            "http");

    assertThat(plain.statusCode()).isBetween(400, 499);
    assertThat(plain.headers().firstValue("Content-Type").orElse("")).doesNotStartWith(XLSX);
    String body = new String(plain.body(), StandardCharsets.UTF_8);
    assertThat(body).doesNotContain("ana.novak@example.org").doesNotContain("Novak");

    // Control: the same client over HTTPS (terminated by the proxy) is served.
    HttpResponse<byte[]> secure =
        getBytes(
            "/api/export",
            "Authorization",
            organizerAuth(),
            "X-Forwarded-For",
            "203.0.113.7",
            "X-Forwarded-Proto",
            "https");
    assertThat(secure.statusCode()).isEqualTo(200);
  }
}
