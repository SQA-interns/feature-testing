package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegistrationValidatorTest {

  static final ConferenceConfig CONFIG =
      new ConferenceConfig(
          "K",
          List.of(
              option("ws-both", true, RegistrationType.EXTERNAL, RegistrationType.STUDENT),
              option("ws-ext", true, RegistrationType.EXTERNAL),
              option("ws-stu", true, RegistrationType.STUDENT),
              option("ws-off", false, RegistrationType.EXTERNAL, RegistrationType.STUDENT)),
          List.of(
              new ConsentDefinition("privacy", "p", true),
              new ConsentDefinition("photos", "f", false)));

  final RegistrationValidator validator = new RegistrationValidator(CONFIG);

  static ConferenceOption option(String id, boolean active, RegistrationType... types) {
    return new ConferenceOption(id, id, OptionCategory.WORKSHOP, active, Set.of(types));
  }

  static RegistrationSubmission external(
      String email, List<String> options, List<String> consents) {
    return new RegistrationSubmission(
        "EXTERNAL", " Ana ", "Novak", email, "Org", "x", "y", "z", options, consents, "tok");
  }

  static RegistrationSubmission student(List<String> options) {
    return new RegistrationSubmission(
        "STUDENT",
        "Luka",
        "Horvat",
        "l@x.org",
        "ignored",
        "Uni",
        "CS",
        "123",
        options,
        List.of("privacy"),
        "tok");
  }

  List<FieldError> errors(RegistrationSubmission submission) {
    return catchThrowableOfType(ValidationException.class, () -> validator.validate(submission))
        .errors();
  }

  @Test
  void externalIsTrimmedAndIgnoresStudentFields() {
    ValidRegistration valid =
        validator.validate(external(" a@b.org ", List.of("ws-both", "ws-ext"), List.of("privacy")));

    assertThat(valid.type()).isEqualTo(RegistrationType.EXTERNAL);
    assertThat(valid.firstName()).isEqualTo("Ana");
    assertThat(valid.email()).isEqualTo("a@b.org");
    assertThat(valid.organization()).isEqualTo("Org");
    assertThat(valid.studyInstitution()).isNull();
    assertThat(valid.studyProgramme()).isNull();
    assertThat(valid.studentId()).isNull();
    assertThat(valid.options()).containsExactly("ws-both", "ws-ext");
    assertThat(valid.consents()).containsExactly("privacy");
  }

  @Test
  void studentKeepsStudentFieldsAndDropsOrganization() {
    ValidRegistration valid = validator.validate(student(List.of("ws-stu")));

    assertThat(valid.type()).isEqualTo(RegistrationType.STUDENT);
    assertThat(valid.organization()).isNull();
    assertThat(valid.studyInstitution()).isEqualTo("Uni");
    assertThat(valid.studyProgramme()).isEqualTo("CS");
    assertThat(valid.studentId()).isEqualTo("123");
  }

  @Test
  void missingOptionsAndOptionalConsentAreFine() {
    ValidRegistration valid =
        validator.validate(external("a@b.org", null, List.of("privacy", "photos")));

    assertThat(valid.options()).isEmpty();
    assertThat(valid.consents()).containsExactly("privacy", "photos");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "  ", "OTHER", "external"})
  void unknownTypeIsAFieldError(String type) {
    RegistrationSubmission submission =
        new RegistrationSubmission(
            type, "A", "B", "a@b.org", "O", null, null, null, List.of(), List.of("privacy"), "t");

    assertThat(errors(submission))
        .containsExactly(new FieldError("type", RegistrationValidator.INVALID_TYPE));
  }

  @Test
  void typeIsTrimmedAndNullTypeIsAnError() {
    RegistrationSubmission padded =
        new RegistrationSubmission(
            " STUDENT ", "A", "B", "a@b.org", null, "U", "P", "1", null, List.of("privacy"), "t");
    assertThat(validator.validate(padded).type()).isEqualTo(RegistrationType.STUDENT);

    RegistrationSubmission none =
        new RegistrationSubmission(
            null, "A", "B", "a@b.org", null, null, null, null, List.of("ws-off"), null, null);
    assertThat(errors(none))
        .extracting(FieldError::field)
        .containsExactly("type", "options", "consents", "captchaToken");
  }

  @Test
  void textLongerThanTheLimitIsRejectedAndTheLimitItselfIsAccepted() {
    String atLimit = "x".repeat(RegistrationValidator.MAX_TEXT_LENGTH);
    RegistrationSubmission ok =
        new RegistrationSubmission(
            "EXTERNAL",
            atLimit,
            "B",
            "a@b.org",
            "O",
            null,
            null,
            null,
            null,
            List.of("privacy"),
            "t");
    assertThat(validator.validate(ok).firstName()).hasSize(200);

    RegistrationSubmission tooLong =
        new RegistrationSubmission(
            "EXTERNAL",
            atLimit + "y",
            "B",
            "a@b.org",
            "O",
            null,
            null,
            null,
            null,
            List.of("privacy"),
            "t");
    assertThat(errors(tooLong))
        .containsExactly(
            new FieldError("firstName", "This field must be at most 200 characters long."));
  }

  @Test
  void emailLongerThan254IsRejectedAnd254IsAccepted() {
    String domain = "@" + "d".repeat(60) + ".org";
    String at254 = "a".repeat(254 - domain.length()) + domain;
    assertThat(validator.validate(external(at254, null, List.of("privacy"))).email()).hasSize(254);

    assertThat(errors(external("b" + at254, null, List.of("privacy"))))
        .containsExactly(
            new FieldError("email", "This field must be at most 254 characters long."));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ana@example",
        "ana@-example.org",
        "ana@example-.org",
        "ana@exa_mple.org",
        ".ana@example.org",
        "ana.@example.org",
        "an..a@example.org",
        "ana\n@example.org",
        "<ana>@example.org",
        "ana@example.org\r\nBcc: x@y.org"
      })
  void invalidEmailFormats(String email) {
    assertThat(errors(external(email, null, List.of("privacy"))))
        .containsExactly(new FieldError("email", RegistrationValidator.INVALID_EMAIL));
  }

  @ParameterizedTest
  @ValueSource(strings = {"a@b.co", "first.last+tag@sub.example.org", "x_y-z@a1-b2.si"})
  void validEmailFormats(String email) {
    assertThat(validator.validate(external(email, null, List.of("privacy"))).email())
        .isEqualTo(email);
  }

  @Test
  void blankEmailIsRequired() {
    assertThat(errors(external("   ", null, List.of("privacy"))))
        .containsExactly(new FieldError("email", RegistrationValidator.REQUIRED));
  }

  @Test
  void optionRules() {
    assertThat(errors(external("a@b.org", List.of("ws-stu"), List.of("privacy"))))
        .containsExactly(new FieldError("options", RegistrationValidator.UNAVAILABLE_OPTION));
    assertThat(errors(student(List.of("ws-ext"))))
        .containsExactly(new FieldError("options", RegistrationValidator.UNAVAILABLE_OPTION));
    assertThat(errors(external("a@b.org", List.of("ws-off"), List.of("privacy"))))
        .containsExactly(new FieldError("options", RegistrationValidator.UNAVAILABLE_OPTION));
    assertThat(errors(external("a@b.org", List.of("nope"), List.of("privacy"))))
        .containsExactly(new FieldError("options", RegistrationValidator.UNAVAILABLE_OPTION));
    assertThat(errors(external("a@b.org", List.of("ws-ext", "ws-ext"), List.of("privacy"))))
        .containsExactly(new FieldError("options", RegistrationValidator.DUPLICATE_OPTION));
  }

  @Test
  void consentRules() {
    assertThat(errors(external("a@b.org", null, null)))
        .containsExactly(new FieldError("consents", RegistrationValidator.MISSING_CONSENT));
    assertThat(errors(external("a@b.org", null, List.of("photos"))))
        .containsExactly(new FieldError("consents", RegistrationValidator.MISSING_CONSENT));
    assertThat(errors(external("a@b.org", null, List.of("privacy", "unknown"))))
        .containsExactly(new FieldError("consents", RegistrationValidator.UNKNOWN_CONSENT));
    assertThat(errors(external("a@b.org", null, List.of("privacy", "privacy"))))
        .containsExactly(new FieldError("consents", RegistrationValidator.UNKNOWN_CONSENT));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void blankCaptchaTokenIsAnError(String token) {
    RegistrationSubmission submission =
        new RegistrationSubmission(
            "EXTERNAL",
            "A",
            "B",
            "a@b.org",
            "O",
            null,
            null,
            null,
            null,
            List.of("privacy"),
            token);

    assertThat(errors(submission))
        .containsExactly(new FieldError("captchaToken", RegistrationValidator.MISSING_CAPTCHA));
  }

  @Test
  void studentRequiresEveryStudentField() {
    RegistrationSubmission submission =
        new RegistrationSubmission(
            "STUDENT", "A", "B", "a@b.org", "O", " ", null, "", null, List.of("privacy"), "t");

    assertThat(errors(submission))
        .extracting(FieldError::field)
        .containsExactly("studyInstitution", "studyProgramme", "studentId");
  }
}
