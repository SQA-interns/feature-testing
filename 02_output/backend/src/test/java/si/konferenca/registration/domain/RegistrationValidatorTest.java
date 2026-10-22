package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static si.konferenca.registration.domain.DomainFixtures.configuration;
import static si.konferenca.registration.domain.DomainFixtures.external;
import static si.konferenca.registration.domain.DomainFixtures.student;
import static si.konferenca.registration.domain.DomainFixtures.with;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegistrationValidatorTest {

  private final RegistrationValidator validator = new RegistrationValidator(configuration());

  private List<FieldError> errors(RegistrationSubmission submission) {
    RegistrationValidator.Result result = validator.validate(submission);
    assertThat(result.valid()).isFalse();
    assertThat(result.data()).isNull();
    return result.errors();
  }

  private static List<String> fields(List<FieldError> errors) {
    return errors.stream().map(FieldError::field).toList();
  }

  @Test
  void validExternalSubmissionGivesTrimmedDataWithoutStudentFields() {
    RegistrationSubmission s =
        new RegistrationSubmission(
            "EXTERNAL",
            "  Ana ",
            "\tNovak",
            " ana@example.org ",
            " Org ",
            "ignored",
            "ignored",
            "ignored",
            List.of("ws-ai", "ws-cloud"),
            List.of("privacy", "photo"),
            "t");

    RegistrationValidator.Result result = validator.validate(s);

    assertThat(result.valid()).isTrue();
    assertThat(result.errors()).isEmpty();
    assertThat(result.data())
        .isEqualTo(
            new RegistrationData(
                RegistrationType.EXTERNAL,
                "Ana",
                "Novak",
                "ana@example.org",
                "Org",
                null,
                null,
                null,
                List.of("ws-ai", "ws-cloud"),
                List.of("privacy", "photo")));
  }

  @Test
  void validStudentSubmissionGivesStudentFieldsWithoutOrganization() {
    RegistrationData data = validator.validate(with(student(), "organization", "x")).data();

    assertThat(data.type()).isEqualTo(RegistrationType.STUDENT);
    assertThat(data.organization()).isNull();
    assertThat(data.studyInstitution()).isEqualTo("University of Ljubljana");
    assertThat(data.studyProgramme()).isEqualTo("Computer Science");
    assertThat(data.studentId()).isEqualTo("63200001");
    assertThat(data.options()).containsExactly("ev-night");
  }

  @Test
  void missingOptionsAndOptionalConsentAreAccepted() {
    RegistrationData data =
        validator
            .validate(with(with(external(), "options", null), "consents", List.of("privacy")))
            .data();

    assertThat(data.options()).isEmpty();
    assertThat(data.consents()).containsExactly("privacy");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "external", "OTHER", " EXTERNAL"})
  void unknownTypeIsAnErrorAndSkipsTypeFieldsAndOptions(String type) {
    List<FieldError> errors =
        errors(with(with(external(), "type", type), "options", List.of("no-such")));

    assertThat(fields(errors)).containsExactly("type");
    assertThat(errors.get(0).message()).isEqualTo("Choose EXTERNAL or STUDENT.");
  }

  @Test
  void missingTypeStillChecksCommonFields() {
    assertThat(fields(errors(with(with(external(), "type", null), "firstName", " "))))
        .containsExactly("type", "firstName");
  }

  @ParameterizedTest
  @ValueSource(strings = {"firstName", "lastName", "email", "organization"})
  void requiredExternalFieldMissingOrBlank(String field) {
    for (String value : Arrays.asList(null, "", "  \t")) {
      List<FieldError> errors = errors(with(external(), field, value));

      assertThat(errors).containsExactly(new FieldError(field, "This field is required."));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"studyInstitution", "studyProgramme", "studentId"})
  void requiredStudentFieldMissing(String field) {
    assertThat(errors(with(student(), field, " ")))
        .containsExactly(new FieldError(field, "This field is required."));
  }

  @Test
  void textUpToTwoHundredCharactersIsAccepted() {
    String max = "a".repeat(200);

    assertThat(validator.validate(with(external(), "lastName", "  " + max + " ")).data().lastName())
        .isEqualTo(max);
  }

  @Test
  void textOverTwoHundredCharactersIsRejected() {
    assertThat(errors(with(external(), "organization", "a".repeat(201))))
        .containsExactly(new FieldError("organization", "At most 200 characters."));
  }

  @Test
  void invalidEmailIsRejectedOnceWithFormatMessage() {
    assertThat(errors(with(external(), "email", "ana.example.org")))
        .containsExactly(new FieldError("email", "Enter a valid email address."));
  }

  @Test
  void emptyEmailReportsOnlyRequired() {
    assertThat(errors(with(external(), "email", "")))
        .containsExactly(new FieldError("email", "This field is required."));
  }

  @Test
  void duplicateOptionIsRejectedOnce() {
    assertThat(errors(with(external(), "options", List.of("ws-ai", "ws-ai", "no-such"))))
        .containsExactly(new FieldError("options", "Each option can be selected only once."));
  }

  @Test
  void nullOptionIsRejected() {
    assertThat(errors(with(external(), "options", Arrays.asList("ws-ai", null))))
        .containsExactly(new FieldError("options", "Each option can be selected only once."));
  }

  @ParameterizedTest
  @ValueSource(strings = {"no-such", "meal-old", "ev-night"})
  void unknownInactiveOrUnavailableOptionIsRejectedForExternal(String id) {
    assertThat(errors(with(external(), "options", List.of("ws-ai", id, "also-unknown"))))
        .containsExactly(new FieldError("options", "Option " + id + " is not available."));
  }

  @Test
  void externalOnlyOptionIsRejectedForStudent() {
    assertThat(fields(errors(with(student(), "options", List.of("ws-cloud")))))
        .containsExactly("options");
  }

  @Test
  void missingMandatoryConsentIsRejected() {
    assertThat(errors(with(external(), "consents", List.of("photo"))))
        .containsExactly(new FieldError("consents", "The consent \"privacy\" is required."));
    assertThat(fields(errors(with(external(), "consents", null)))).containsExactly("consents");
  }

  @Test
  void unknownRepeatedOrNullConsentIsRejectedOnce() {
    for (List<String> consents :
        List.of(
            List.of("privacy", "nope"),
            List.of("privacy", "privacy"),
            Arrays.asList("privacy", null))) {
      assertThat(errors(with(external(), "consents", consents)))
          .containsExactly(new FieldError("consents", "Unknown or repeated consent."));
    }
  }

  @Test
  void missingOrBlankCaptchaTokenIsRejected() {
    for (String token : Arrays.asList(null, "", "  ")) {
      assertThat(errors(with(external(), "captchaToken", token)))
          .containsExactly(new FieldError("captchaToken", "Confirm that you are not a robot."));
    }
  }

  @Test
  void allProblemsAreReportedTogetherInFieldOrder() {
    RegistrationSubmission s =
        new RegistrationSubmission(
            "STUDENT", "", "", "bad", null, "", "", "", List.of("x"), List.of(), null);

    assertThat(fields(errors(s)))
        .containsExactly(
            "firstName",
            "lastName",
            "email",
            "studyInstitution",
            "studyProgramme",
            "studentId",
            "options",
            "consents",
            "captchaToken");
  }

  @Test
  void errorsAreImmutable() {
    List<FieldError> errors = errors(with(external(), "firstName", ""));

    assertThatThrownBy(() -> errors.add(new FieldError("x", "y")))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
