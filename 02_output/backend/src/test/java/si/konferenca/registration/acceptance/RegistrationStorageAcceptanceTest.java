package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Submissions;
import tools.jackson.databind.JsonNode;

/** US-005 Reliable registration storage: database row and raw JSON copy. */
class RegistrationStorageAcceptanceTest extends AcceptanceTestBase {

  private static final List<String> EXTERNAL_COLUMNS =
      List.of("first_name", "last_name", "email", "organization");
  private static final List<String> STUDENT_COLUMNS =
      List.of(
          "first_name", "last_name", "email", "study_institution", "study_programme", "student_id");

  private static Map<String, Object> submission(String type) {
    Map<String, Object> body =
        "EXTERNAL".equals(type) ? Submissions.validExternal() : Submissions.validStudent();
    body.put("consents", new ArrayList<>(List.of("privacy", "photos")));
    return body;
  }

  private static String camel(String column) {
    StringBuilder out = new StringBuilder();
    boolean upper = false;
    for (char c : column.toCharArray()) {
      if (c == '_') {
        upper = true;
      } else {
        out.append(upper ? Character.toUpperCase(c) : c);
        upper = false;
      }
    }
    return out.toString();
  }

  @ParameterizedTest(name = "AC-005-01 {0} registration is stored with all its data")
  @ValueSource(strings = {"EXTERNAL", "STUDENT"})
  void ac_005_01_acceptedRegistrationIsStoredWithAllData(String type) {
    Map<String, Object> body = submission(type);

    HttpResponse<String> response = postRegistration(body);

    assertThat(response.statusCode()).as("body: %s", response.body()).isEqualTo(201);
    UUID id = UUID.fromString(json(response).path("id").asString());
    List<Map<String, Object>> rows = queryRows("SELECT * FROM registration WHERE id = ?", id);
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.get(0);
    assertThat(row.get("type")).isEqualTo(type);
    assertThat(row.get("registered_at")).as("time of registration").isNotNull();
    for (String column : "EXTERNAL".equals(type) ? EXTERNAL_COLUMNS : STUDENT_COLUMNS) {
      assertThat(row.get(column)).as(column).isEqualTo(body.get(camel(column)));
    }

    List<Object> optionIds =
        queryRows(
                "SELECT option_id FROM registration_option WHERE registration_id = ?"
                    + " ORDER BY position",
                id)
            .stream()
            .map(r -> r.get("option_id"))
            .toList();
    assertThat(optionIds).containsExactlyElementsOf(castList(body.get("optionIds")));

    List<Map<String, Object>> consents =
        queryRows(
            "SELECT consent_id, given_at FROM registration_consent WHERE registration_id = ?"
                + " ORDER BY consent_id",
            id);
    assertThat(consents).extracting(c -> c.get("consent_id")).containsExactly("photos", "privacy");
    assertThat(consents).allSatisfy(c -> assertThat(c.get("given_at")).isNotNull());
  }

  @Test
  @DisplayName("AC-005-02 a JSON copy named after the id holds the registration as accepted")
  void ac_005_02_jsonCopyNamedAfterIdContainsTheRegistration() throws Exception {
    Map<String, Object> body = submission("STUDENT");

    HttpResponse<String> response = postRegistration(body);

    assertThat(response.statusCode()).as("body: %s", response.body()).isEqualTo(201);
    String id = json(response).path("id").asString();
    Path copy = JSON_COPY_DIR.resolve(id + ".json");
    assertThat(copy).exists();
    JsonNode stored = JSON.readTree(Files.readString(copy, StandardCharsets.UTF_8));
    assertThat(stored.path("id").asString()).isEqualTo(id);
    assertThat(stored.path("type").asString()).isEqualTo("STUDENT");
    assertThat(stored.path("registeredAt").asString()).isNotBlank();
    JsonNode participant = stored.path("participant");
    for (String field :
        List.of(
            "firstName", "lastName", "email", "studyInstitution", "studyProgramme", "studentId")) {
      assertThat(participant.path(field).asString()).as(field).isEqualTo(body.get(field));
    }
    List<Object> optionIds = new ArrayList<>();
    stored.path("options").forEach(o -> optionIds.add(o.path("id").asString()));
    assertThat(optionIds).containsExactlyElementsOf(castList(body.get("optionIds")));
    List<String> consentIds = new ArrayList<>();
    stored.path("consents").forEach(c -> consentIds.add(c.path("id").asString()));
    assertThat(consentIds).containsExactlyInAnyOrder("privacy", "photos");
    assertThat(stored.has("recaptchaToken")).as("no captcha token in the copy").isFalse();
  }

  @Test
  @DisplayName("AC-005-03 when the JSON copy cannot be written the API errors and no row remains")
  void ac_005_03_failedJsonCopyLeavesNoRegistration() throws Exception {
    Set<PosixFilePermission> original = Files.getPosixFilePermissions(JSON_COPY_DIR);
    Files.setPosixFilePermissions(JSON_COPY_DIR, PosixFilePermissions.fromString("r-xr-xr-x"));
    try {
      assertThat(Files.isWritable(JSON_COPY_DIR))
          .as("precondition: the JSON copy directory is not writable (tests must not run as root)")
          .isFalse();

      HttpResponse<String> response = postRegistration(Submissions.validExternal());

      assertThat(response.statusCode()).as("body: %s", response.body()).isBetween(500, 599);
      assertThat(registrationCount()).as("registrations in the database").isZero();
      assertThat(jsonCopies()).as("JSON copies").isEmpty();
    } finally {
      Files.setPosixFilePermissions(JSON_COPY_DIR, original);
    }
  }

  @ParameterizedTest(name = "AC-005-04 {0} with Slovenian characters is stored unchanged")
  @ValueSource(strings = {"EXTERNAL", "STUDENT"})
  void ac_005_04_slovenianCharactersAreStoredUnchanged(String type) throws Exception {
    Map<String, Object> body = submission(type);
    body.put("firstName", "Čedomir Žiga");
    body.put("lastName", "Šuštaršič");
    if ("EXTERNAL".equals(type)) {
      body.put("organization", "Inštitut za ŽČŠ raziskave, Črnomelj");
    } else {
      body.put("studyInstitution", "Univerza v Ljubljani, Fakulteta za šport");
    }

    HttpResponse<String> response = postRegistration(body);

    assertThat(response.statusCode()).as("body: %s", response.body()).isEqualTo(201);
    String id = json(response).path("id").asString();
    String institutionColumn = "EXTERNAL".equals(type) ? "organization" : "study_institution";
    Map<String, Object> row =
        queryRows(
                "SELECT first_name, last_name, "
                    + institutionColumn
                    + " AS institution FROM registration WHERE id = ?",
                UUID.fromString(id))
            .get(0);
    String institutionField = "EXTERNAL".equals(type) ? "organization" : "studyInstitution";
    assertThat(row)
        .containsEntry("first_name", "Čedomir Žiga")
        .containsEntry("last_name", "Šuštaršič")
        .containsEntry("institution", body.get(institutionField));

    JsonNode participant =
        JSON.readTree(Files.readString(JSON_COPY_DIR.resolve(id + ".json"), StandardCharsets.UTF_8))
            .path("participant");
    assertThat(participant.path("firstName").asString()).isEqualTo("Čedomir Žiga");
    assertThat(participant.path("lastName").asString()).isEqualTo("Šuštaršič");
    assertThat(participant.path(institutionField).asString()).isEqualTo(body.get(institutionField));
  }

  @SuppressWarnings("unchecked")
  private static List<Object> castList(Object value) {
    return (List<Object>) value;
  }
}
