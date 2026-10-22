package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegistrationValidatorTest {

  private final RegistrationValidator validator = new RegistrationValidator(Catalogues.standard());

  private static RegistrationForm external(List<String> options, List<String> consents) {
    return new RegistrationForm(
        "EXTERNAL",
        " Ana ",
        "Novak",
        "ana@example.org",
        "Org",
        "ignored",
        "ignored",
        "ignored",
        options,
        consents,
        "token");
  }

  private static RegistrationForm student(List<String> options) {
    return new RegistrationForm(
        "STUDENT",
        "Luka",
        "Horvat",
        "luka@example.org",
        "ignored",
        "Uni",
        "CS",
        "63",
        options,
        List.of("privacy"),
        "token");
  }

  private static List<String> fields(RegistrationValidator.Result result) {
    return result.errors().stream().map(FieldError::field).toList();
  }

  @Test
  void validExternalIsNormalisedAndIgnoresStudentFields() {
    RegistrationValidator.Result result =
        validator.validate(external(List.of("ws", "ex"), List.of("privacy", "photos")));

    assertThat(result.valid()).isTrue();
    Participant p = result.participant();
    assertThat(p.type()).isEqualTo(RegistrationType.EXTERNAL);
    assertThat(p.firstName()).isEqualTo("Ana");
    assertThat(p.organization()).isEqualTo("Org");
    assertThat(p.studyInstitution()).isNull();
    assertThat(p.studyProgramme()).isNull();
    assertThat(p.studentId()).isNull();
    assertThat(result.options()).containsExactly("ws", "ex");
    assertThat(result.consents()).containsExactly("privacy", "photos");
  }

  @Test
  void validStudentKeepsStudyFieldsAndIgnoresOrganization() {
    RegistrationValidator.Result result = validator.validate(student(List.of("ws", "st")));

    assertThat(result.valid()).isTrue();
    assertThat(result.participant().organization()).isNull();
    assertThat(result.participant().studyInstitution()).isEqualTo("Uni");
    assertThat(result.participant().studyProgramme()).isEqualTo("CS");
    assertThat(result.participant().studentId()).isEqualTo("63");
  }

  @Test
  void missingTypeIsRejectedAndNoParticipantIsBuilt() {
    RegistrationValidator.Result result =
        validator.validate(
            new RegistrationForm(
                null,
                "A",
                "B",
                "a@b.co",
                "Org",
                null,
                null,
                null,
                List.of("ws"),
                List.of("privacy"),
                "t"));

    assertThat(fields(result)).containsExactly("type");
    assertThat(result.participant()).isNull();
    assertThat(result.valid()).isFalse();
    assertThat(result.errors().getFirst().message()).isNotBlank();
  }

  @Test
  void unknownTypeStillChecksOptionsForExistence() {
    RegistrationValidator.Result result =
        validator.validate(
            new RegistrationForm(
                "VISITOR",
                "A",
                "B",
                "a@b.co",
                null,
                null,
                null,
                null,
                List.of("ex", "nope"),
                List.of("privacy"),
                "t"));

    assertThat(fields(result)).containsExactly("type", "options");
  }

  @Test
  void unknownTypeAcceptsAnyActiveOption() {
    RegistrationValidator.Result result =
        validator.validate(
            new RegistrationForm(
                " EXTERNAL ",
                "A",
                "B",
                "a@b.co",
                "Org",
                null,
                null,
                null,
                List.of("ex"),
                List.of("privacy"),
                "t"));

    assertThat(result.valid()).as("the type is trimmed before parsing").isTrue();
  }

  @Test
  void eachFieldLongerThanItsLimitIsRejected() {
    String name = "x".repeat(RegistrationValidator.NAME_MAX + 1);
    String institution = "x".repeat(RegistrationValidator.INSTITUTION_MAX + 1);
    String studentId = "x".repeat(RegistrationValidator.STUDENT_ID_MAX + 1);
    String email = "a@" + "x".repeat(RegistrationValidator.EMAIL_MAX) + ".org";

    RegistrationValidator.Result externalResult =
        validator.validate(
            new RegistrationForm(
                "EXTERNAL",
                name,
                name,
                email,
                institution,
                null,
                null,
                null,
                null,
                List.of("privacy"),
                "t"));
    RegistrationValidator.Result studentResult =
        validator.validate(
            new RegistrationForm(
                "STUDENT",
                "A",
                "B",
                "a@b.co",
                null,
                institution,
                institution,
                studentId,
                null,
                List.of("privacy"),
                "t"));

    assertThat(fields(externalResult))
        .containsExactly("firstName", "lastName", "email", "organization");
    assertThat(externalResult.errors().getFirst().message()).contains("100");
    assertThat(fields(studentResult))
        .containsExactly("studyInstitution", "studyProgramme", "studentId");
  }

  @Test
  void fieldsAtTheirLimitAreAccepted() {
    String name = "x".repeat(RegistrationValidator.NAME_MAX);
    String institution = "x".repeat(RegistrationValidator.INSTITUTION_MAX);
    RegistrationValidator.Result result =
        validator.validate(
            new RegistrationForm(
                "EXTERNAL",
                name,
                name,
                "a@b.co",
                institution,
                null,
                null,
                null,
                null,
                List.of("privacy"),
                "t"));

    assertThat(result.valid()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"a@b", "a b@c.de", "a@b@c.de", "a@.", "a@b.", "a\u0007@b.de", "@b.de"})
  void invalidEmailIsRejected(String email) {
    RegistrationValidator.Result result =
        validator.validate(
            new RegistrationForm(
                "EXTERNAL",
                "A",
                "B",
                email,
                "Org",
                null,
                null,
                null,
                null,
                List.of("privacy"),
                "t"));

    assertThat(fields(result)).containsExactly("email");
    assertThat(result.errors().getFirst().message()).isEqualTo(RegistrationValidator.INVALID_EMAIL);
  }

  @Test
  void missingRequiredFieldUsesTheRequiredMessage() {
    RegistrationValidator.Result result =
        validator.validate(
            new RegistrationForm(
                "STUDENT", "A", "B", null, null, "Uni", "CS", "  ", null, List.of("privacy"), "t"));

    assertThat(fields(result)).containsExactly("email", "studentId");
    assertThat(result.errors()).allMatch(e -> e.message().equals(RegistrationValidator.REQUIRED));
  }

  @Test
  void missingOptionListMeansNoOptions() {
    RegistrationValidator.Result result = validator.validate(external(null, List.of("privacy")));

    assertThat(result.valid()).isTrue();
    assertThat(result.options()).isEmpty();
  }

  @Test
  void moreThanFiftyOptionsAreRejected() {
    List<String> many =
        new ArrayList<>(Collections.nCopies(RegistrationValidator.OPTIONS_MAX + 1, "ws"));

    RegistrationValidator.Result result = validator.validate(external(many, List.of("privacy")));

    assertThat(fields(result)).containsExactly("options");
    assertThat(result.errors().getFirst().message()).contains("50");
    assertThat(result.options()).isEmpty();
  }

  @Test
  void optionProblemsHaveDistinctMessages() {
    String unknown =
        validator
            .validate(external(List.of("nope"), List.of("privacy")))
            .errors()
            .getFirst()
            .message();
    String inactive =
        validator
            .validate(external(List.of("old"), List.of("privacy")))
            .errors()
            .getFirst()
            .message();
    String unavailable = validator.validate(student(List.of("ex"))).errors().getFirst().message();
    String twice =
        validator
            .validate(external(List.of("ws", "ws"), List.of("privacy")))
            .errors()
            .getFirst()
            .message();

    assertThat(unknown).isEqualTo(inactive).contains("not offered");
    assertThat(unavailable).contains("not available");
    assertThat(twice).contains("more than once");
  }

  @Test
  void studentOnlyOptionIsRejectedForExternals() {
    assertThat(fields(validator.validate(external(List.of("st"), List.of("privacy")))))
        .containsExactly("options");
  }

  @Test
  void nullOptionEntryIsRejected() {
    List<String> withNull = new ArrayList<>();
    withNull.add(null);

    assertThat(fields(validator.validate(external(withNull, List.of("privacy")))))
        .containsExactly("options");
  }

  @Test
  void consentProblemsAreRejectedOnce() {
    assertThat(fields(validator.validate(external(List.of(), List.of("privacy", "nope")))))
        .containsExactly("consents");
    assertThat(fields(validator.validate(external(List.of(), List.of("privacy", "privacy")))))
        .containsExactly("consents");
    assertThat(
            validator
                .validate(external(List.of(), List.of("photos")))
                .errors()
                .getFirst()
                .message())
        .isEqualTo("This consent is required.");
    assertThat(validator.validate(external(List.of(), null)).errors())
        .extracting(FieldError::field)
        .containsExactly("consents");
    assertThat(
            validator.validate(external(List.of(), List.of("nope"))).errors().getFirst().message())
        .isEqualTo("A consent is not valid.");
  }

  @Test
  void errorsOfEveryFieldAreCollectedTogether() {
    RegistrationValidator.Result result =
        validator.validate(
            new RegistrationForm(
                "EXTERNAL", "", "", "bad", "", null, null, null, List.of("nope"), List.of(), null));

    assertThat(fields(result))
        .containsExactly("firstName", "lastName", "email", "organization", "options", "consents");
    assertThat(result.options()).isEmpty();
    assertThat(result.consents()).isEmpty();
  }
}
