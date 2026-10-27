package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.domain.DomainFixtures.PHOTOS;
import static si.konferenca.registration.domain.DomainFixtures.PRIVACY;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class RegistrationValidatorTest {

  private final RegistrationValidator validator =
      new RegistrationValidator(DomainFixtures.catalog());

  private static Submission with(Submission s, String field, String value) {
    return new Submission(
        field.equals("type") ? value : s.type(),
        field.equals("firstName") ? value : s.firstName(),
        field.equals("lastName") ? value : s.lastName(),
        field.equals("email") ? value : s.email(),
        field.equals("organization") ? value : s.organization(),
        field.equals("studyInstitution") ? value : s.studyInstitution(),
        field.equals("studyProgramme") ? value : s.studyProgramme(),
        field.equals("studentId") ? value : s.studentId(),
        s.optionIds(),
        s.consents(),
        field.equals("recaptchaToken") ? value : s.recaptchaToken());
  }

  private List<FieldError> errors(Submission submission) {
    return validator.validate(submission).errors();
  }

  @Test
  void validExternalSubmissionIsNormalised() {
    ValidatedSubmission result =
        validator.validate(with(DomainFixtures.external(), "firstName", "  Ana\t"));

    assertThat(result.isValid()).isTrue();
    assertThat(result.type()).isEqualTo(RegistrationType.EXTERNAL);
    assertThat(result.participant())
        .isEqualTo(
            new Participant("Ana", "Novak", "ana@example.org", "Institute", null, null, null));
    assertThat(result.options())
        .containsExactly(new SelectedOption("ws-a", "Workshop A", OptionCategory.WORKSHOP));
    assertThat(result.consents()).containsExactly(PRIVACY);
  }

  @Test
  void validStudentSubmissionKeepsStudentFieldsAndConsentOrder() {
    ValidatedSubmission result = validator.validate(DomainFixtures.student());

    assertThat(result.errors()).isEmpty();
    assertThat(result.type()).isEqualTo(RegistrationType.STUDENT);
    assertThat(result.participant())
        .isEqualTo(
            new Participant(
                "Žiga",
                "Šuštar",
                "ziga@student.example.org",
                null,
                "University",
                "Informatics",
                "63200001"));
    assertThat(result.consents()).containsExactly(PRIVACY, PHOTOS);
  }

  @Test
  void typeIsTrimmedAndMatchedExactly() {
    assertThat(errors(with(DomainFixtures.external(), "type", " EXTERNAL "))).isEmpty();
    assertThat(errors(with(DomainFixtures.external(), "type", "external")))
        .contains(new FieldError("type", ErrorCode.REQUIRED));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "OTHER"})
  void missingOrUnknownTypeIsReportedWithoutParticipant(String type) {
    ValidatedSubmission result = validator.validate(with(DomainFixtures.external(), "type", type));

    assertThat(result.errors()).containsExactly(new FieldError("type", ErrorCode.REQUIRED));
    assertThat(result.type()).isNull();
    assertThat(result.participant()).isNull();
    assertThat(result.isValid()).isFalse();
  }

  @Test
  void withoutTypeAnOptionIsNotCheckedForAvailability() {
    Submission student = DomainFixtures.student();
    Submission noType =
        new Submission(
            null,
            student.firstName(),
            student.lastName(),
            student.email(),
            null,
            null,
            null,
            null,
            List.of("ev-dinner"),
            List.of("privacy"),
            "token");

    assertThat(errors(noType)).containsExactly(new FieldError("type", ErrorCode.REQUIRED));
  }

  @ParameterizedTest
  @ValueSource(strings = {"firstName", "lastName", "email", "organization"})
  void externalRequiredFieldsAreReported(String field) {
    assertThat(errors(with(DomainFixtures.external(), field, " ")))
        .containsExactly(new FieldError(field, ErrorCode.REQUIRED));
    assertThat(errors(with(DomainFixtures.external(), field, null)))
        .containsExactly(new FieldError(field, ErrorCode.REQUIRED));
  }

  @ParameterizedTest
  @ValueSource(strings = {"studyInstitution", "studyProgramme", "studentId"})
  void studentRequiredFieldsAreReported(String field) {
    assertThat(errors(with(DomainFixtures.student(), field, "")))
        .containsExactly(new FieldError(field, ErrorCode.REQUIRED));
  }

  @ParameterizedTest
  @ValueSource(strings = {"studyInstitution", "studyProgramme", "studentId"})
  void studentFieldsAreNotAllowedForExternal(String field) {
    assertThat(errors(with(DomainFixtures.external(), field, "x")))
        .containsExactly(new FieldError(field, ErrorCode.NOT_ALLOWED));
    assertThat(errors(with(DomainFixtures.external(), field, "  "))).isEmpty();
  }

  @Test
  void organizationIsNotAllowedForStudent() {
    assertThat(errors(with(DomainFixtures.student(), "organization", "Company")))
        .containsExactly(new FieldError("organization", ErrorCode.NOT_ALLOWED));
    assertThat(errors(with(DomainFixtures.student(), "organization", ""))).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"firstName", "lastName", "organization"})
  void textFieldsAllow200CharactersAfterTrimming(String field) {
    assertThat(errors(with(DomainFixtures.external(), field, " " + "č".repeat(200) + " ")))
        .isEmpty();
    assertThat(errors(with(DomainFixtures.external(), field, "č".repeat(201))))
        .containsExactly(new FieldError(field, ErrorCode.TOO_LONG));
  }

  @Test
  void lengthIsCountedInCodePoints() {
    String emoji = "😀";
    assertThat(errors(with(DomainFixtures.external(), "firstName", emoji.repeat(200)))).isEmpty();
    assertThat(errors(with(DomainFixtures.external(), "firstName", emoji.repeat(201))))
        .containsExactly(new FieldError("firstName", ErrorCode.TOO_LONG));
  }

  @Test
  void studentFieldLimits() {
    assertThat(errors(with(DomainFixtures.student(), "studyInstitution", "a".repeat(200))))
        .isEmpty();
    assertThat(errors(with(DomainFixtures.student(), "studyProgramme", "a".repeat(201))))
        .containsExactly(new FieldError("studyProgramme", ErrorCode.TOO_LONG));
    assertThat(errors(with(DomainFixtures.student(), "studentId", "1".repeat(50)))).isEmpty();
    assertThat(errors(with(DomainFixtures.student(), "studentId", "1".repeat(51))))
        .containsExactly(new FieldError("studentId", ErrorCode.TOO_LONG));
  }

  @Test
  void emailLimitIs254CharactersAndTooLongIsNotAlsoAFormatError() {
    String domain = "@" + "d".repeat(63) + "." + "e".repeat(63) + "." + "f".repeat(63) + ".org";
    String at254 = "a".repeat(254 - domain.length()) + domain;
    assertThat(at254).hasSize(254);
    assertThat(errors(with(DomainFixtures.external(), "email", at254))).isEmpty();
    assertThat(errors(with(DomainFixtures.external(), "email", "a" + at254)))
        .containsExactly(new FieldError("email", ErrorCode.TOO_LONG));
  }

  @Test
  void invalidEmailIsAFormatError() {
    assertThat(errors(with(DomainFixtures.external(), "email", "ana@example")))
        .containsExactly(new FieldError("email", ErrorCode.INVALID_FORMAT));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "  "})
  void blankCaptchaTokenIsReported(String token) {
    assertThat(errors(with(DomainFixtures.external(), "recaptchaToken", token)))
        .containsExactly(new FieldError("recaptchaToken", ErrorCode.CAPTCHA_FAILED));
  }

  @Test
  void optionProblemsAreReportedPerOption() {
    List<String> ids = DomainFixtures.withNull("ws-a", "nope", "ws-old", "ws-a");
    ValidatedSubmission result =
        validator.validate(DomainFixtures.external(ids, List.of("privacy")));

    assertThat(result.errors())
        .containsExactly(
            new FieldError("optionIds", ErrorCode.UNKNOWN_OPTION),
            new FieldError("optionIds", ErrorCode.INACTIVE_OPTION),
            new FieldError("optionIds", ErrorCode.DUPLICATE_OPTION),
            new FieldError("optionIds", ErrorCode.UNKNOWN_OPTION));
    assertThat(result.options())
        .containsExactly(new SelectedOption("ws-a", "Workshop A", OptionCategory.WORKSHOP));
  }

  @Test
  void optionNotOfferedToTheTypeIsReported() {
    Submission student = DomainFixtures.student();
    Submission withDinner =
        new Submission(
            student.type(),
            student.firstName(),
            student.lastName(),
            student.email(),
            null,
            student.studyInstitution(),
            student.studyProgramme(),
            student.studentId(),
            List.of("ws-a", "ev-dinner"),
            List.of("privacy"),
            "token");

    assertThat(errors(withDinner))
        .containsExactly(new FieldError("optionIds", ErrorCode.OPTION_NOT_AVAILABLE));
  }

  @Test
  void noOptionsIsValid() {
    ValidatedSubmission result =
        validator.validate(DomainFixtures.external(List.of(), List.of("privacy")));

    assertThat(result.isValid()).isTrue();
    assertThat(result.options()).isEmpty();
  }

  @Test
  void consentProblemsAreReportedPerConsent() {
    ValidatedSubmission result =
        validator.validate(
            DomainFixtures.external(List.of(), DomainFixtures.withNull("photos", "spam")));

    assertThat(result.errors())
        .containsExactly(
            new FieldError("consents.spam", ErrorCode.UNKNOWN_CONSENT),
            new FieldError("consents.null", ErrorCode.UNKNOWN_CONSENT),
            new FieldError("consents.privacy", ErrorCode.CONSENT_REQUIRED));
    assertThat(result.consents()).containsExactly(PHOTOS);
  }

  @Test
  void allErrorsAreCollected() {
    Submission empty =
        new Submission(null, null, null, null, null, null, null, null, null, null, null);

    assertThat(errors(empty))
        .containsExactly(
            new FieldError("type", ErrorCode.REQUIRED),
            new FieldError("firstName", ErrorCode.REQUIRED),
            new FieldError("lastName", ErrorCode.REQUIRED),
            new FieldError("email", ErrorCode.REQUIRED),
            new FieldError("consents.privacy", ErrorCode.CONSENT_REQUIRED),
            new FieldError("recaptchaToken", ErrorCode.CAPTCHA_FAILED));
  }
}
