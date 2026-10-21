package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import si.konferenca.registration.acceptance.support.AcceptanceTest;
import si.konferenca.registration.acceptance.support.Database;
import si.konferenca.registration.acceptance.support.JsonCopies;
import si.konferenca.registration.acceptance.support.Submissions;
import si.konferenca.registration.acceptance.support.TestStack;
import tools.jackson.databind.JsonNode;

/** US-005: every accepted registration is in the database and has its JSON copy. */
@AcceptanceTest
class StorageAcceptanceTest {

  @Autowired private MockMvc mvc;

  private final Database db = TestStack.database();
  private final JsonCopies copies = TestStack.jsonCopies();

  @DynamicPropertySource
  static void stack(DynamicPropertyRegistry registry) {
    TestStack.register(registry);
  }

  @Test
  void ac_005_01_databaseHoldsTheExternalRegistrationWithOptionsConsentsAndTimes()
      throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    body.put("options", List.of("ev-gala", "ws-ai", "other-cert"));
    body.put("consents", List.of("privacy", "photos"));
    OffsetDateTime before = OffsetDateTime.now().minusSeconds(5);

    String id = register(body);

    OffsetDateTime after = OffsetDateTime.now().plusSeconds(5);
    assertThat(db.registration(id))
        .containsEntry("type", "EXTERNAL")
        .containsEntry("first_name", "Ana")
        .containsEntry("last_name", "Novak")
        .containsEntry("email", (String) body.get("email"))
        .containsEntry("organization", "Example d.o.o.");
    assertThat(db.options(id)).containsExactly("ev-gala", "ws-ai", "other-cert");
    Map<String, OffsetDateTime> consents = db.consents(id);
    assertThat(consents).containsOnlyKeys("privacy", "photos");
    consents.values().forEach(t -> assertThat(t).isBetween(before, after));
    assertThat(db.registeredAt(id)).isBetween(before, after);
  }

  @Test
  void ac_005_01_databaseHoldsTheStudentRegistrationFields() throws Exception {
    Map<String, Object> body = Submissions.validStudent();

    String id = register(body);

    assertThat(db.registration(id))
        .containsEntry("type", "STUDENT")
        .containsEntry("first_name", "Luka")
        .containsEntry("last_name", "Kranjc")
        .containsEntry("email", (String) body.get("email"))
        .containsEntry("study_institution", "Univerza v Ljubljani")
        .containsEntry("study_programme", "Računalništvo in informatika")
        .containsEntry("student_id", "63210001");
    assertThat(db.options(id)).containsExactly("ws-data", "meal-lunch");
    assertThat(db.consents(id)).containsOnlyKeys("privacy");
    assertThat(db.registeredAt(id)).isNotNull();
  }

  @Test
  void ac_005_02_jsonCopyNamedAfterTheIdContainsTheAcceptedRegistration() throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    body.put("firstName", "  Ana ");

    String id = register(body);

    assertThat(copies.file(id)).exists();
    JsonNode copy = copies.read(id);
    assertThat(copy.path("id").asString("")).isEqualTo(id);
    assertThat(copy.path("type").asString("")).isEqualTo("EXTERNAL");
    assertThat(copy.path("firstName").asString("")).isEqualTo("Ana");
    assertThat(copy.path("lastName").asString("")).isEqualTo("Novak");
    assertThat(copy.path("email").asString("")).isEqualTo(body.get("email"));
    assertThat(copy.path("organization").asString("")).isEqualTo("Example d.o.o.");
    assertThat(FormConfigAcceptanceTest.strings(copy.path("options")))
        .containsExactly("ws-ai", "ev-gala");
    assertThat(copy.path("consents").values())
        .singleElement()
        .satisfies(
            c -> {
              assertThat(c.path("id").asString("")).isEqualTo("privacy");
              assertThat(c.path("givenAt").asString("")).isNotBlank();
            });
    assertThat(OffsetDateTime.parse(copy.path("registeredAt").asString("")).toInstant())
        .isEqualTo(db.registeredAt(id).toInstant());
  }

  @Test
  void ac_005_04_slovenianCharactersAreStoredUnchangedInDatabaseAndJsonCopy() throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    body.put("firstName", "Žiga Čeh");
    body.put("lastName", "Šuštaršič");
    body.put("organization", "Fakulteta za računalništvo, ČŠŽ čšž");

    String id = register(body);

    assertThat(db.registration(id))
        .containsEntry("first_name", "Žiga Čeh")
        .containsEntry("last_name", "Šuštaršič")
        .containsEntry("organization", "Fakulteta za računalništvo, ČŠŽ čšž");
    JsonNode copy = copies.read(id);
    assertThat(copy.path("firstName").asString("")).isEqualTo("Žiga Čeh");
    assertThat(copy.path("lastName").asString("")).isEqualTo("Šuštaršič");
    assertThat(copy.path("organization").asString(""))
        .isEqualTo("Fakulteta za računalništvo, ČŠŽ čšž");
  }

  @Test
  void ac_005_04_slovenianCharactersOfStudentInstitutionAreStoredUnchanged() throws Exception {
    Map<String, Object> body = Submissions.validStudent();
    body.put("studyInstitution", "Univerza v Mariboru, Žalec Šoštanj Črnomelj");

    String id = register(body);

    assertThat(db.registration(id))
        .containsEntry("study_institution", "Univerza v Mariboru, Žalec Šoštanj Črnomelj");
    assertThat(copies.read(id).path("studyInstitution").asString(""))
        .isEqualTo("Univerza v Mariboru, Žalec Šoštanj Črnomelj");
  }

  private String register(Map<String, Object> body) throws Exception {
    return Submissions.id(
        mvc.perform(Submissions.post(body)).andExpect(status().isCreated()).andReturn());
  }
}
