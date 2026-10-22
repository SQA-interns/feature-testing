package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** US-005 Reliable registration storage (AC-005-01, 02, 04; AC-005-03 in its own class). */
class Us005StorageAcceptanceTest extends AcceptanceTestBase {

  private static final String SLOVENIAN_FIRST = "Čedomir Šžč";
  private static final String SLOVENIAN_LAST = "Žužek-Čšž ŠČŽ";
  private static final String SLOVENIAN_INSTITUTION = "Univerza v Ljubljani, Fakulteta Čšž ŠŽČ";

  @DisplayName("AC-005-01")
  @Test
  void ac_005_01_databaseHoldsTypeFieldsOptionsConsentsWithTimeAndRegistrationTime()
      throws Exception {
    ObjectNode body = options(validExternal(), "ws-ai", "other-tour");
    body.putArray("consents").add("privacy").add("photo");
    OffsetDateTime before = OffsetDateTime.now().minusMinutes(1);

    String id = register(body);

    OffsetDateTime after = OffsetDateTime.now().plusMinutes(1);
    Map<String, Object> row = registrationRow(id);
    assertThat(row).as("registration row " + id).isNotEmpty();
    assertThat(row.get("type")).isEqualTo("EXTERNAL");
    assertThat(row.get("first_name")).isEqualTo("Ana");
    assertThat(row.get("last_name")).isEqualTo("Novak");
    assertThat(row.get("email")).isEqualTo(body.path("email").asString());
    assertThat(row.get("organization")).isEqualTo("Example d.o.o.");
    assertThat(row.get("study_institution")).isNull();
    assertThat(row.get("study_programme")).isNull();
    assertThat(row.get("student_id")).isNull();
    OffsetDateTime registeredAt = (OffsetDateTime) row.get("registered_at");
    assertThat(registeredAt).isBetween(before, after);

    assertThat(registrationOptions(id)).containsExactlyInAnyOrder("ws-ai", "other-tour");
    Map<String, OffsetDateTime> consents = registrationConsents(id);
    assertThat(consents.keySet()).containsExactlyInAnyOrder("privacy", "photo");
    consents.values().forEach(givenAt -> assertThat(givenAt).isBetween(before, after));
  }

  @DisplayName("AC-005-01")
  @Test
  void ac_005_01_databaseHoldsStudentFields() throws Exception {
    ObjectNode body = options(validStudent(), "meal-lunch");

    String id = register(body);

    Map<String, Object> row = registrationRow(id);
    assertThat(row.get("type")).isEqualTo("STUDENT");
    assertThat(row.get("first_name")).isEqualTo("Luka");
    assertThat(row.get("last_name")).isEqualTo("Kranjc");
    assertThat(row.get("study_institution")).isEqualTo("University of Ljubljana");
    assertThat(row.get("study_programme")).isEqualTo("Computer Science");
    assertThat(row.get("student_id")).isEqualTo("63200001");
    assertThat(row.get("organization")).isNull();
    assertThat(row.get("registered_at")).isNotNull();
    assertThat(registrationOptions(id)).containsExactly("meal-lunch");
    assertThat(registrationConsents(id).keySet()).containsExactly("privacy");
  }

  private static List<String> strings(JsonNode array) {
    List<String> values = new ArrayList<>();
    array.forEach(v -> values.add(v.isObject() ? v.path("id").asString() : v.asString()));
    return values;
  }

  @DisplayName("AC-005-02")
  @Test
  void ac_005_02_jsonCopyNamedAfterIdContainsTheAcceptedRegistration() throws Exception {
    ObjectNode body = options(validExternal(), "ws-ai", "ev-gala");
    body.put("firstName", "  Ana  ");

    String id = register(body);

    assertThat(jsonCopy(id)).as("JSON copy " + id + ".json").exists();
    JsonNode copy = JSON.readTree(Files.readString(jsonCopy(id), StandardCharsets.UTF_8));
    assertThat(copy.path("id").asString()).isEqualTo(id);
    assertThat(copy.path("type").asString()).isEqualTo("EXTERNAL");
    assertThat(copy.path("firstName").asString()).isEqualTo("Ana");
    assertThat(copy.path("lastName").asString()).isEqualTo("Novak");
    assertThat(copy.path("email").asString()).isEqualTo(body.path("email").asString());
    assertThat(copy.path("organization").asString()).isEqualTo("Example d.o.o.");
    assertThat(strings(copy.path("options"))).containsExactlyInAnyOrder("ws-ai", "ev-gala");
    assertThat(strings(copy.path("consents"))).containsExactly("privacy");
    assertThat(copy.path("registeredAt").asString()).isNotBlank();
    assertThat(OffsetDateTime.parse(copy.path("registeredAt").asString()))
        .isBetween(OffsetDateTime.now().minus(Duration.ofMinutes(2)), OffsetDateTime.now());
  }

  @DisplayName("AC-005-04")
  @Test
  void ac_005_04_slovenianCharactersAreUnchangedInDatabaseAndJsonCopy() throws Exception {
    ObjectNode body = validStudent();
    body.put("firstName", SLOVENIAN_FIRST);
    body.put("lastName", SLOVENIAN_LAST);
    body.put("studyInstitution", SLOVENIAN_INSTITUTION);

    String id = register(body);

    Map<String, Object> row = registrationRow(id);
    assertThat(row.get("first_name")).isEqualTo(SLOVENIAN_FIRST);
    assertThat(row.get("last_name")).isEqualTo(SLOVENIAN_LAST);
    assertThat(row.get("study_institution")).isEqualTo(SLOVENIAN_INSTITUTION);

    JsonNode copy = JSON.readTree(Files.readString(jsonCopy(id), StandardCharsets.UTF_8));
    assertThat(copy.path("firstName").asString()).isEqualTo(SLOVENIAN_FIRST);
    assertThat(copy.path("lastName").asString()).isEqualTo(SLOVENIAN_LAST);
    assertThat(copy.path("studyInstitution").asString()).isEqualTo(SLOVENIAN_INSTITUTION);
  }

  @DisplayName("AC-005-04")
  @Test
  void ac_005_04_slovenianCharactersInExternalOrganizationAreUnchanged() throws Exception {
    ObjectNode body = validExternal();
    body.put("firstName", "Čč Šš Žž");
    body.put("lastName", "ČŠŽ");
    body.put("organization", SLOVENIAN_INSTITUTION);

    String id = register(body);

    Map<String, Object> row = registrationRow(id);
    assertThat(row.get("first_name")).isEqualTo("Čč Šš Žž");
    assertThat(row.get("last_name")).isEqualTo("ČŠŽ");
    assertThat(row.get("organization")).isEqualTo(SLOVENIAN_INSTITUTION);
    JsonNode copy = JSON.readTree(Files.readString(jsonCopy(id), StandardCharsets.UTF_8));
    assertThat(copy.path("organization").asString()).isEqualTo(SLOVENIAN_INSTITUTION);
  }
}
