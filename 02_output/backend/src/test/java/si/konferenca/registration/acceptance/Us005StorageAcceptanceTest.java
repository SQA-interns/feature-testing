package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.acceptance.support.Submissions.validExternal;
import static si.konferenca.registration.acceptance.support.Submissions.validStudent;
import static si.konferenca.registration.acceptance.support.Submissions.with;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.Database;
import si.konferenca.registration.acceptance.support.StandardAcceptanceTest;
import tools.jackson.databind.JsonNode;

/** US-005 Reliable registration storage: database row and raw JSON copy. */
class Us005StorageAcceptanceTest extends StandardAcceptanceTest {

  /** Tolerance for clock differences between the test JVM, the backend and the database. */
  private static final Duration CLOCK_TOLERANCE = Duration.ofSeconds(60);

  @Test
  @DisplayName(
      "AC-005-01 the database holds the registration with fields, options, consents and times")
  void ac005_01_databaseHoldsTheWholeRegistration() {
    OffsetDateTime before = OffsetDateTime.now().minus(CLOCK_TOLERANCE);
    UUID external =
        registerAccepted(
            with(
                validExternal(),
                "options",
                List.of("ws-ai", "ev-dinner"),
                "consents",
                List.of("privacy", "photos")));
    UUID student = registerAccepted(validStudent());
    OffsetDateTime after = OffsetDateTime.now().plus(CLOCK_TOLERANCE);

    assertThat(db.countRegistrations()).isEqualTo(2);
    Database.Row row = db.row(external);
    assertThat(row.type()).isEqualTo("EXTERNAL");
    assertThat(row.firstName()).isEqualTo("Ana");
    assertThat(row.lastName()).isEqualTo("Novak");
    assertThat(row.email()).isEqualTo("ana.novak@example.org");
    assertThat(row.organization()).isEqualTo("Example d.o.o.");
    assertThat(row.registeredAt()).isBetween(before, after);
    assertThat(db.optionIds(external)).containsExactlyInAnyOrder("ws-ai", "ev-dinner");
    Map<String, OffsetDateTime> consents = db.consents(external);
    assertThat(consents).containsOnlyKeys("privacy", "photos");
    assertThat(consents.values())
        .allSatisfy(givenAt -> assertThat(givenAt).isBetween(before, after));

    Database.Row studentRow = db.row(student);
    assertThat(studentRow.type()).isEqualTo("STUDENT");
    assertThat(studentRow.firstName()).isEqualTo("Luka");
    assertThat(studentRow.lastName()).isEqualTo("Horvat");
    assertThat(studentRow.email()).isEqualTo("luka.horvat@student.example.org");
    assertThat(studentRow.studyInstitution()).isEqualTo("Univerza v Ljubljani");
    assertThat(studentRow.studyProgramme()).isEqualTo("Computer Science");
    assertThat(studentRow.studentId()).isEqualTo("63210001");
    assertThat(studentRow.registeredAt()).isBetween(before, after);
    assertThat(db.optionIds(student)).containsExactlyInAnyOrder("ws-ai", "meal-lunch");
    assertThat(db.consents(student)).containsOnlyKeys("privacy");
  }

  @Test
  @DisplayName(
      "AC-005-02 a JSON copy named after the registration id holds the accepted registration")
  void ac005_02_jsonCopyHoldsTheAcceptedRegistration() {
    UUID id = registerAccepted(with(validExternal(), "firstName", "  Ana  "));

    assertThat(copies.files()).containsExactly(id + ".json");
    JsonNode copy = copies.json(id);
    assertThat(copy.path("id").asString()).isEqualTo(id.toString());
    assertThat(copy.path("type").asString()).isEqualTo("EXTERNAL");
    assertThat(copy.path("firstName").asString()).isEqualTo("Ana");
    assertThat(copy.path("lastName").asString()).isEqualTo("Novak");
    assertThat(copy.path("email").asString()).isEqualTo("ana.novak@example.org");
    assertThat(copy.path("organization").asString()).isEqualTo("Example d.o.o.");
    assertThat(copy.path("options").valueStream().map(JsonNode::asString))
        .containsExactlyInAnyOrder("ws-ai", "ev-dinner");
    assertThat(copy.path("consents").toString()).contains("privacy");
    assertThat(OffsetDateTime.parse(copy.path("registeredAt").asString()))
        .isCloseTo(
            db.row(id).registeredAt(),
            org.assertj.core.api.Assertions.within(Duration.ofSeconds(1)));
  }

  @Test
  @DisplayName("AC-005-04 Slovenian characters are stored unchanged in the row and the JSON copy")
  void ac005_04_slovenianCharactersAreStoredUnchanged() {
    String firstName = "Čedomir Žan";
    String lastName = "Šuštaršič-Žužek čšž";
    String organization = "Društvo čebelarjev Šentjur ČŠŽ";
    String institution = "Univerza v Mariboru, Fakulteta za računalništvo in informatiko";
    UUID external =
        registerAccepted(
            with(
                validExternal(),
                "firstName",
                firstName,
                "lastName",
                lastName,
                "organization",
                organization));
    UUID student =
        registerAccepted(
            with(validStudent(), "lastName", lastName, "studyInstitution", institution));

    assertThat(db.row(external).firstName()).isEqualTo(firstName);
    assertThat(db.row(external).lastName()).isEqualTo(lastName);
    assertThat(db.row(external).organization()).isEqualTo(organization);
    assertThat(db.row(student).lastName()).isEqualTo(lastName);
    assertThat(db.row(student).studyInstitution()).isEqualTo(institution);

    JsonNode copy = copies.json(external);
    assertThat(copy.path("firstName").asString()).isEqualTo(firstName);
    assertThat(copy.path("lastName").asString()).isEqualTo(lastName);
    assertThat(copy.path("organization").asString()).isEqualTo(organization);
    assertThat(new String(copies.bytes(external), StandardCharsets.UTF_8))
        .as("raw UTF-8 text in the file")
        .contains(firstName, lastName, organization);
    assertThat(new String(copies.bytes(student), StandardCharsets.UTF_8)).contains(institution);
  }
}
