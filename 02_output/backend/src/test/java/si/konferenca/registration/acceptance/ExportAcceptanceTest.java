package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import si.konferenca.registration.acceptance.support.AcceptanceTest;
import si.konferenca.registration.acceptance.support.Database;
import si.konferenca.registration.acceptance.support.Submissions;
import si.konferenca.registration.acceptance.support.TestStack;
import si.konferenca.registration.acceptance.support.Workbooks;

/** US-008: organizers export the registrations as an Excel workbook; nobody else can. */
@AcceptanceTest
class ExportAcceptanceTest {

  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  @Autowired private MockMvc mvc;

  private final Database db = TestStack.database();

  @DynamicPropertySource
  static void stack(DynamicPropertyRegistry registry) {
    TestStack.register(registry);
  }

  @Test
  void ac_008_01_organizerExportsWorkbookWithOneRowPerRegistration() throws Exception {
    Map<String, Object> external = Submissions.validExternal();
    Map<String, Object> student = Submissions.validStudent();
    String externalId = register(external);
    String studentId = register(student);

    MockHttpServletResponse response =
        mvc.perform(Submissions.export()).andExpect(status().isOk()).andReturn().getResponse();

    assertThat(response.getContentType()).startsWith(XLSX);
    byte[] xlsx = response.getContentAsByteArray();
    assertThat(Workbooks.header(xlsx)).containsExactlyElementsOf(Workbooks.COLUMNS);
    assertThat(Workbooks.records(xlsx)).hasSize((int) db.countAll());

    Map<String, String> e = Workbooks.record(xlsx, externalId);
    assertThat(e)
        .containsEntry("type", "EXTERNAL")
        .containsEntry("firstName", "Ana")
        .containsEntry("lastName", "Novak")
        .containsEntry("email", (String) external.get("email"))
        .containsEntry("organization", "Example d.o.o.")
        .containsEntry("studyInstitution", "")
        .containsEntry("studyProgramme", "")
        .containsEntry("studentId", "")
        .containsEntry("options", "ws-ai, ev-gala")
        .containsEntry("consents", "privacy");
    assertThat(Instant.parse(e.get("registeredAt")))
        .isEqualTo(db.registeredAt(externalId).toInstant());

    Map<String, String> s = Workbooks.record(xlsx, studentId);
    assertThat(s)
        .containsEntry("type", "STUDENT")
        .containsEntry("firstName", "Luka")
        .containsEntry("lastName", "Kranjc")
        .containsEntry("email", (String) student.get("email"))
        .containsEntry("organization", "")
        .containsEntry("studyInstitution", "Univerza v Ljubljani")
        .containsEntry("studyProgramme", "Računalništvo in informatika")
        .containsEntry("studentId", "63210001")
        .containsEntry("options", "ws-data, meal-lunch")
        .containsEntry("consents", "privacy");
    assertThat(s.get("registeredAt")).isNotBlank();
  }

  @Test
  void ac_008_02_exportWithoutCredentialsIsRefusedWithoutData() throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    register(body);

    MockHttpServletResponse response =
        mvc.perform(Submissions.export(null, null))
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse();

    assertNoRegistrationData(response, (String) body.get("email"));
  }

  @Test
  void ac_008_02_exportWithWrongPasswordIsRefusedWithoutData() throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    register(body);

    MockHttpServletResponse response =
        mvc.perform(Submissions.export(TestStack.ORGANIZER_USERNAME, "wrong-password-123456"))
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse();

    assertNoRegistrationData(response, (String) body.get("email"));
  }

  @Test
  void ac_008_02_exportWithWrongUsernameIsRefusedWithoutData() throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    register(body);

    MockHttpServletResponse response =
        mvc.perform(Submissions.export("someone-else", TestStack.ORGANIZER_PASSWORD))
            .andExpect(status().isUnauthorized())
            .andReturn()
            .getResponse();

    assertNoRegistrationData(response, (String) body.get("email"));
  }

  @Test
  void ac_008_03_slovenianCharactersAreUnchangedInTheWorkbook() throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    body.put("firstName", "Žiga Čeh");
    body.put("lastName", "Šuštaršič");
    body.put("organization", "Zavod ČŠŽ, Črnuče");
    String id = register(body);

    byte[] xlsx =
        mvc.perform(Submissions.export())
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

    assertThat(Workbooks.record(xlsx, id))
        .containsEntry("firstName", "Žiga Čeh")
        .containsEntry("lastName", "Šuštaršič")
        .containsEntry("organization", "Zavod ČŠŽ, Črnuče");
  }

  static void assertNoRegistrationData(MockHttpServletResponse response, String email) {
    byte[] content = response.getContentAsByteArray();
    assertThat(Arrays.copyOf(content, Math.min(2, content.length)))
        .isNotEqualTo("PK".getBytes(StandardCharsets.US_ASCII));
    assertThat(new String(content, StandardCharsets.UTF_8)).doesNotContain(email, "Novak");
    assertThat(String.valueOf(response.getContentType())).doesNotStartWith(XLSX);
  }

  private String register(Map<String, Object> body) throws Exception {
    return Submissions.id(
        mvc.perform(Submissions.post(body)).andExpect(status().isCreated()).andReturn());
  }
}
