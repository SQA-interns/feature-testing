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
import tools.jackson.databind.JsonNode;

/** US-005 reliable storage: AC-005-01, AC-005-02, AC-005-04 (database row and JSON copy). */
class StorageAcceptanceTest extends DefaultContextAcceptanceTest {

  static final List<String> COPY_FIELDS =
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
  @DisplayName("AC-005-01 the database holds the external registration with options and consents")
  void ac_005_01_externalRegistrationIsStored() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("consents", List.of("privacy", "photos"));
    OffsetDateTime before = OffsetDateTime.now().minusSeconds(5);

    String id = createdId(postRegistration(body));
    OffsetDateTime after = OffsetDateTime.now().plusSeconds(5);

    assertThat(registrationCount()).isEqualTo(1);
    Map<String, Object> row = registrationRow(id);
    assertThat(row.get("type")).isEqualTo("EXTERNAL");
    assertThat(row.get("first_name")).isEqualTo("Ana");
    assertThat(row.get("last_name")).isEqualTo("Novak");
    assertThat(row.get("email")).isEqualTo("ana.novak@example.org");
    assertThat(row.get("organization")).isEqualTo("Example d.o.o.");
    assertThat(row.get("study_institution")).isNull();
    assertThat(row.get("study_programme")).isNull();
    assertThat(row.get("student_id")).isNull();
    OffsetDateTime registeredAt = (OffsetDateTime) row.get("registered_at");
    assertThat(registeredAt).isAfter(before).isBefore(after);
    assertThat(optionIds(id)).containsExactly("ev-gala", "ws-ai");
    Map<String, OffsetDateTime> consents = consents(id);
    assertThat(consents.keySet()).containsExactly("photos", "privacy");
    for (OffsetDateTime givenAt : consents.values()) {
      assertThat(givenAt).isAfter(before).isBefore(after);
    }
  }

  @Test
  @DisplayName("AC-005-01 the database holds the student registration with every field")
  void ac_005_01_studentRegistrationIsStored() throws Exception {
    String id = createdId(postRegistration(validStudent()));

    Map<String, Object> row = registrationRow(id);
    assertThat(row.get("type")).isEqualTo("STUDENT");
    assertThat(row.get("first_name")).isEqualTo("Luka");
    assertThat(row.get("last_name")).isEqualTo("Horvat");
    assertThat(row.get("email")).isEqualTo("luka.horvat@student.example.org");
    assertThat(row.get("organization")).isNull();
    assertThat(row.get("study_institution")).isEqualTo("University of Ljubljana");
    assertThat(row.get("study_programme")).isEqualTo("Computer Science");
    assertThat(row.get("student_id")).isEqualTo("63210001");
    assertThat(row.get("registered_at")).isNotNull();
    assertThat(optionIds(id)).containsExactly("ev-student", "ws-ai");
    assertThat(consents(id).keySet()).containsExactly("privacy");
  }

  @Test
  @DisplayName("AC-005-01 a second registration with the same email is stored separately (OQ-05)")
  void ac_005_01_repeatEmailIsStoredSeparately() throws Exception {
    String first = createdId(postRegistration(validExternal()));
    String second = createdId(postRegistration(validExternal()));

    assertThat(first).isNotEqualTo(second);
    assertThat(registrationCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("AC-005-02 a JSON copy named after the id holds the registration as accepted")
  void ac_005_02_jsonCopyHoldsAcceptedRegistration() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("firstName", "  Ana ");

    String id = createdId(postRegistration(body));

    assertThat(jsonCopy(id)).isRegularFile();
    assertThat(jsonCopyCount()).isEqualTo(1);
    JsonNode copy = json.readTree(Files.readAllBytes(jsonCopy(id)));
    for (String field : COPY_FIELDS) {
      assertThat(copy.has(field)).as("JSON copy field %s", field).isTrue();
    }
    assertThat(copy.path("id").asString()).isEqualTo(id);
    assertThat(copy.path("type").asString()).isEqualTo("EXTERNAL");
    assertThat(copy.path("firstName").asString()).isEqualTo("Ana");
    assertThat(copy.path("lastName").asString()).isEqualTo("Novak");
    assertThat(copy.path("email").asString()).isEqualTo("ana.novak@example.org");
    assertThat(copy.path("organization").asString()).isEqualTo("Example d.o.o.");
    assertThat(copy.path("studyInstitution").isNull()).isTrue();
    assertThat(copy.path("studyProgramme").isNull()).isTrue();
    assertThat(copy.path("studentId").isNull()).isTrue();
    List<String> options = new ArrayList<>();
    copy.path("options").forEach(o -> options.add(o.asString()));
    assertThat(options).containsExactlyInAnyOrder("ws-ai", "ev-gala");
    assertThat(copy.path("consents")).hasSize(1);
    assertThat(copy.path("consents").get(0).path("id").asString()).isEqualTo("privacy");
    OffsetDateTime givenAt =
        OffsetDateTime.parse(copy.path("consents").get(0).path("givenAt").asString());
    OffsetDateTime registeredAt = OffsetDateTime.parse(copy.path("registeredAt").asString());
    OffsetDateTime stored = (OffsetDateTime) registrationRow(id).get("registered_at");
    assertThat(Duration.between(registeredAt, stored).abs())
        .as("registeredAt of the JSON copy vs the database (microsecond rounding allowed)")
        .isLessThan(Duration.ofMillis(1));
    assertThat(givenAt).isNotNull();
  }

  @Test
  @DisplayName("AC-005-04 Slovenian characters are stored unchanged in the database and JSON copy")
  void ac_005_04_slovenianCharactersAreStoredUnchanged() throws Exception {
    Map<String, Object> external = validExternal();
    external.put("firstName", "Žiga Črtomir");
    external.put("lastName", "Šuštaršič-Čeh");
    external.put("organization", "Fakulteta za računalništvo, ČŠŽ čšž d.o.o.");
    Map<String, Object> student = validStudent();
    student.put("firstName", "Špela");
    student.put("lastName", "Žnidaršič");
    student.put("studyInstitution", "Univerza v Mariboru, Škofja Loka Č");

    String externalId = createdId(postRegistration(external));
    String studentId = createdId(postRegistration(student));

    Map<String, Object> row = registrationRow(externalId);
    assertThat(row.get("first_name")).isEqualTo("Žiga Črtomir");
    assertThat(row.get("last_name")).isEqualTo("Šuštaršič-Čeh");
    assertThat(row.get("organization")).isEqualTo("Fakulteta za računalništvo, ČŠŽ čšž d.o.o.");
    assertThat(registrationRow(studentId).get("first_name")).isEqualTo("Špela");
    assertThat(registrationRow(studentId).get("last_name")).isEqualTo("Žnidaršič");
    assertThat(registrationRow(studentId).get("study_institution"))
        .isEqualTo("Univerza v Mariboru, Škofja Loka Č");

    String externalCopy =
        new String(Files.readAllBytes(jsonCopy(externalId)), StandardCharsets.UTF_8);
    JsonNode copy = json.readTree(externalCopy);
    assertThat(copy.path("firstName").asString()).isEqualTo("Žiga Črtomir");
    assertThat(copy.path("lastName").asString()).isEqualTo("Šuštaršič-Čeh");
    assertThat(copy.path("organization").asString())
        .isEqualTo("Fakulteta za računalništvo, ČŠŽ čšž d.o.o.");
    JsonNode studentCopy = json.readTree(Files.readAllBytes(jsonCopy(studentId)));
    assertThat(studentCopy.path("lastName").asString()).isEqualTo("Žnidaršič");
    assertThat(studentCopy.path("studyInstitution").asString())
        .isEqualTo("Univerza v Mariboru, Škofja Loka Č");
  }
}
