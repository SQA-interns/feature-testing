package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DomainValuesTest {

  @Test
  void submissionTurnsMissingListsIntoEmptyOnesAndKeepsNulls() {
    Submission missing =
        new Submission(null, null, null, null, null, null, null, null, null, null, null);
    Submission withNull =
        new Submission(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            DomainFixtures.withNull("a"),
            DomainFixtures.withNull(),
            null);

    assertThat(missing.optionIds()).isEmpty();
    assertThat(missing.consents()).isEmpty();
    assertThat(withNull.optionIds()).containsExactly("a", null);
    assertThat(withNull.consents()).containsExactly((String) null);
    assertThatThrownBy(() -> withNull.optionIds().add("b"))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> withNull.consents().add("b"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void submissionCopiesItsLists() {
    List<String> ids = new ArrayList<>(List.of("a"));
    List<String> consents = new ArrayList<>(List.of("privacy"));
    Submission submission =
        new Submission(null, null, null, null, null, null, null, null, ids, consents, null);
    ids.add("b");
    consents.add("photos");

    assertThat(submission.optionIds()).containsExactly("a");
    assertThat(submission.consents()).containsExactly("privacy");
  }

  @Test
  void validatedSubmissionIsValidOnlyWithoutErrors() {
    ValidatedSubmission ok = new ValidatedSubmission(List.of(), null, null, List.of(), List.of());
    ValidatedSubmission failed =
        new ValidatedSubmission(
            List.of(new FieldError("type", ErrorCode.REQUIRED)), null, null, List.of(), List.of());

    assertThat(ok.isValid()).isTrue();
    assertThat(failed.isValid()).isFalse();
  }

  @Test
  void rejectionCarriesACopyOfTheErrors() {
    List<FieldError> errors = new ArrayList<>(List.of(new FieldError("email", ErrorCode.REQUIRED)));
    RegistrationRejectedException e = new RegistrationRejectedException(errors);
    errors.clear();

    assertThat(e.errors()).containsExactly(new FieldError("email", ErrorCode.REQUIRED));
    assertThat(e).hasMessage("registration rejected");
  }

  @Test
  void registrationAndOptionCopyTheirCollections() {
    List<SelectedOption> options =
        new ArrayList<>(List.of(new SelectedOption("ws-a", "A", OptionCategory.WORKSHOP)));
    List<GivenConsent> consents = new ArrayList<>();
    Registration registration =
        new Registration(
            UUID.randomUUID(), RegistrationType.EXTERNAL, Instant.EPOCH, null, options, consents);
    options.clear();
    consents.add(new GivenConsent("privacy", "t", Instant.EPOCH));

    assertThat(registration.options()).hasSize(1);
    assertThat(registration.consents()).isEmpty();

    Set<RegistrationType> types = new java.util.HashSet<>(Set.of(RegistrationType.STUDENT));
    ConferenceOption option = new ConferenceOption("x", "X", OptionCategory.MEAL, true, types);
    types.add(RegistrationType.EXTERNAL);
    assertThat(option.isAvailableTo(RegistrationType.EXTERNAL)).isFalse();
  }
}
