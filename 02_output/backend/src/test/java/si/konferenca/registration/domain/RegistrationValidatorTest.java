package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegistrationValidatorTest {

  private static final Set<RegistrationType> BOTH = Set.of(RegistrationType.values());

  private final ConferenceCatalog catalog =
      ConferenceCatalog.of(
          List.of(
              new ConferenceOption("ws-ai", "AI workshop", OptionCategory.WORKSHOP, true, BOTH),
              new ConferenceOption(
                  "ev-tour", "Tour", OptionCategory.EVENT, true, Set.of(RegistrationType.EXTERNAL)),
              new ConferenceOption("ws-old", "Old", OptionCategory.WORKSHOP, false, BOTH)),
          List.of(
              new ConsentDefinition("privacy", "Privacy text", true),
              new ConsentDefinition("photos", "Photos text", false)));

  private final RegistrationValidator validator = new RegistrationValidator(catalog);

  private static RegistrationInput external(String firstName, String email, List<String> options) {
    return new RegistrationInput(
        "EXTERNAL",
        firstName,
        "Novak",
        email,
        "Org",
        "ignored",
        "ignored",
        "ignored",
        options,
        List.of("privacy"),
        "token");
  }

  private static RegistrationInput student() {
    return new RegistrationInput(
        "STUDENT",
        " Luka ",
        "Kranjc",
        "luka@uni.si",
        "ignored org",
        " UL ",
        " CS ",
        " 6321 ",
        List.of("ws-ai"),
        List.of("privacy", "photos"),
        "token");
  }

  @Test
  void acceptsAValidExternalSubmissionWithTrimmedValuesAndOtherTypeFieldsNull() {
    ValidationResult result =
        validator.validate(external("  Ana ", " ana@example.org ", List.of("ws-ai", "ev-tour")));

    assertThat(result.isValid()).isTrue();
    assertThat(result.errors()).isEmpty();
    ValidRegistration r = result.registration();
    assertThat(r.type()).isEqualTo(RegistrationType.EXTERNAL);
    assertThat(r.firstName()).isEqualTo("Ana");
    assertThat(r.lastName()).isEqualTo("Novak");
    assertThat(r.email()).isEqualTo("ana@example.org");
    assertThat(r.organization()).isEqualTo("Org");
    assertThat(r.studyInstitution()).isNull();
    assertThat(r.studyProgramme()).isNull();
    assertThat(r.studentId()).isNull();
    assertThat(r.options()).containsExactly("ws-ai", "ev-tour");
    assertThat(r.consents()).containsExactly("privacy");
  }

  @Test
  void acceptsAValidStudentSubmissionAndIgnoresTheOrganization() {
    ValidationResult result = validator.validate(student());

    assertThat(result.isValid()).isTrue();
    ValidRegistration r = result.registration();
    assertThat(r.type()).isEqualTo(RegistrationType.STUDENT);
    assertThat(r.firstName()).isEqualTo("Luka");
    assertThat(r.organization()).isNull();
    assertThat(r.studyInstitution()).isEqualTo("UL");
    assertThat(r.studyProgramme()).isEqualTo("CS");
    assertThat(r.studentId()).isEqualTo("6321");
    assertThat(r.consents()).containsExactly("privacy", "photos");
  }

  @Test
  void studentWithoutStudyFieldsGetsOneErrorPerField() {
    RegistrationInput input =
        new RegistrationInput(
            "STUDENT",
            "Luka",
            "Kranjc",
            "luka@uni.si",
            "Org",
            null,
            "",
            "  ",
            null,
            List.of("privacy"),
            "token");

    ValidationResult result = validator.validate(input);

    assertThat(result.isValid()).isFalse();
    assertThat(result.registration()).isNull();
    assertThat(result.errors())
        .containsExactly(
            new FieldError("studyInstitution", RegistrationValidator.REQUIRED),
            new FieldError("studyProgramme", RegistrationValidator.REQUIRED),
            new FieldError("studentId", RegistrationValidator.REQUIRED));
  }

  @ParameterizedTest
  @ValueSource(strings = {"external", "Student", "OTHER", ""})
  void rejectsAnUnknownTypeWithOnlyTheTypeError(String type) {
    RegistrationInput input =
        new RegistrationInput(type, null, null, null, null, null, null, null, null, null, null);

    assertThat(validator.validate(input).errors())
        .containsExactly(new FieldError("type", RegistrationValidator.TYPE_INVALID));
  }

  @Test
  void rejectsAMissingType() {
    RegistrationInput input =
        new RegistrationInput(null, "A", "B", "a@b.si", "O", null, null, null, null, null, "t");

    assertThat(validator.validate(input).errors())
        .containsExactly(new FieldError("type", RegistrationValidator.TYPE_INVALID));
  }

  @Test
  void reportsEveryInvalidFieldTogether() {
    RegistrationInput input =
        new RegistrationInput("EXTERNAL", " ", null, "bad", "", null, null, null, null, null, null);

    assertThat(validator.validate(input).errors())
        .extracting(FieldError::field)
        .containsExactly(
            "firstName", "lastName", "email", "organization", "consents", "captchaToken");
  }

  @Test
  void textAtTheMaximumLengthIsAcceptedAndOneCodePointMoreIsRejected() {
    String max = "Ž".repeat(RegistrationValidator.MAX_TEXT_LENGTH);
    assertThat(validator.validate(external(max, "a@b.si", null)).isValid()).isTrue();

    String emoji = "😀".repeat(RegistrationValidator.MAX_TEXT_LENGTH);
    assertThat(validator.validate(external(emoji, "a@b.si", null)).isValid()).isTrue();

    ValidationResult tooLong = validator.validate(external(max + "a", "a@b.si", null));
    assertThat(tooLong.errors())
        .containsExactly(new FieldError("firstName", RegistrationValidator.TOO_LONG));
  }

  @Test
  void emailAtTheMaximumLengthIsAcceptedAndLongerIsRejected() {
    String domain = "@" + "d".repeat(60) + ".si";
    String local = "a".repeat(RegistrationValidator.MAX_EMAIL_LENGTH - domain.length());
    assertThat(validator.validate(external("Ana", local + domain, null)).isValid()).isTrue();

    ValidationResult tooLong = validator.validate(external("Ana", "a" + local + domain, null));
    assertThat(tooLong.errors())
        .containsExactly(new FieldError("email", RegistrationValidator.TOO_LONG));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ana",
        "ana@",
        "@example.org",
        "ana@example",
        "ana@-example.org",
        "ana@example-.org",
        "an a@example.org",
        "ana@ex ample.org",
        "ana@@example.org",
        "ana@example..org",
        "ana@example.org\r\nBcc: x@y.si"
      })
  void rejectsEmailsWithoutAValidFormat(String email) {
    assertThat(validator.validate(external("Ana", email, null)).errors())
        .containsExactly(new FieldError("email", RegistrationValidator.EMAIL_INVALID));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ana@example.org", "a.b+c@sub.example.co.uk", "x@a1.si", "Ž@x.si"})
  void emailPatternDecides(String email) {
    boolean expected = !email.startsWith("Ž");
    assertThat(validator.validate(external("Ana", email, null)).isValid()).isEqualTo(expected);
  }

  @Test
  void missingOptionsMeanNone() {
    ValidationResult result = validator.validate(external("Ana", "a@b.si", null));

    assertThat(result.registration().options()).isEmpty();
  }

  @Test
  void rejectsADuplicateOption() {
    ValidationResult result =
        validator.validate(external("Ana", "a@b.si", List.of("ws-ai", "ws-ai")));

    assertThat(result.errors())
        .containsExactly(new FieldError("options", "Option 'ws-ai' is selected more than once."));
  }

  @Test
  void rejectsUnknownInactiveAndNullOptionsNamingTheFirstOne() {
    assertThat(validator.validate(external("Ana", "a@b.si", List.of("nope", "ws-old"))).errors())
        .containsExactly(new FieldError("options", "Option 'nope' is not available."));
    assertThat(validator.validate(external("Ana", "a@b.si", List.of("ws-ai", "ws-old"))).errors())
        .containsExactly(new FieldError("options", "Option 'ws-old' is not available."));
    assertThat(validator.validate(external("Ana", "a@b.si", nullList())).errors())
        .containsExactly(new FieldError("options", "Option 'null' is not available."));
  }

  @Test
  void rejectsAnOptionNotAvailableToStudents() {
    RegistrationInput base = student();
    RegistrationInput input =
        new RegistrationInput(
            base.type(),
            base.firstName(),
            base.lastName(),
            base.email(),
            base.organization(),
            base.studyInstitution(),
            base.studyProgramme(),
            base.studentId(),
            List.of("ev-tour"),
            base.consents(),
            base.captchaToken());

    assertThat(validator.validate(input).errors())
        .containsExactly(new FieldError("options", "Option 'ev-tour' is not available."));
  }

  @Test
  void rejectsAMissingMandatoryConsent() {
    assertThat(validator.validate(withConsents(List.of("photos"))).errors())
        .containsExactly(new FieldError("consents", "Consent 'privacy' is required."));
    assertThat(validator.validate(withConsents(null)).errors())
        .containsExactly(new FieldError("consents", "Consent 'privacy' is required."));
  }

  @Test
  void rejectsAnUnknownOrNullConsent() {
    assertThat(validator.validate(withConsents(List.of("privacy", "marketing"))).errors())
        .containsExactly(new FieldError("consents", "Consent 'marketing' is unknown."));
    List<String> withNull = new ArrayList<>(Arrays.asList("privacy", null));
    assertThat(validator.validate(withConsents(withNull)).errors())
        .containsExactly(new FieldError("consents", "Consent 'null' is unknown."));
  }

  @Test
  void collapsesDuplicateConsents() {
    ValidationResult result = validator.validate(withConsents(List.of("privacy", "privacy")));

    assertThat(result.registration().consents()).containsExactly("privacy");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void rejectsABlankCaptchaToken(String token) {
    RegistrationInput input =
        new RegistrationInput(
            "EXTERNAL", "A", "B", "a@b.si", "O", null, null, null, null, List.of("privacy"), token);

    assertThat(validator.validate(input).errors())
        .containsExactly(new FieldError("captchaToken", RegistrationValidator.REQUIRED));
  }

  private static RegistrationInput withConsents(List<String> consents) {
    return new RegistrationInput(
        "EXTERNAL", "A", "B", "a@b.si", "O", null, null, null, null, consents, "t");
  }

  private static List<String> nullList() {
    List<String> list = new ArrayList<>();
    list.add(null);
    return list;
  }
}
