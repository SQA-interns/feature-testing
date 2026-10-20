package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DomainModelTest {

  static ConferenceOption option(String id, boolean active) {
    return new ConferenceOption(
        id, "n-" + id, OptionCategory.MEAL, active, Set.of(RegistrationType.STUDENT));
  }

  @Test
  void optionIsSelectableOnlyWhenActiveAndAvailable() {
    assertThat(option("a", true).isSelectableBy(RegistrationType.STUDENT)).isTrue();
    assertThat(option("a", true).isSelectableBy(RegistrationType.EXTERNAL)).isFalse();
    assertThat(option("a", false).isSelectableBy(RegistrationType.STUDENT)).isFalse();
  }

  @Test
  void optionCopiesItsTypes() {
    Set<RegistrationType> types = EnumSet.of(RegistrationType.EXTERNAL);
    ConferenceOption option = new ConferenceOption("a", "A", OptionCategory.EVENT, true, types);
    types.add(RegistrationType.STUDENT);

    assertThat(option.availableTo()).containsExactly(RegistrationType.EXTERNAL);
  }

  @Test
  void configListsActiveOptionsInOrderAndFindsById() {
    ConferenceConfig config =
        new ConferenceConfig(
            "Konferenca",
            List.of(option("b", true), option("off", false), option("a", true)),
            List.of(new ConsentDefinition("privacy", "text", true)));

    assertThat(config.conferenceName()).isEqualTo("Konferenca");
    assertThat(config.activeOptions()).extracting(ConferenceOption::id).containsExactly("b", "a");
    assertThat(config.option("off")).map(ConferenceOption::active).contains(false);
    assertThat(config.option("missing")).isEmpty();
    assertThat(config.consent("privacy")).map(ConsentDefinition::text).contains("text");
    assertThat(config.consent("missing")).isEmpty();
    assertThat(config.consents()).hasSize(1);
  }

  @Test
  void configRejectsBlankNameAndDuplicateIds() {
    List<ConsentDefinition> consents = List.of(new ConsentDefinition("c", "t", false));
    assertThatThrownBy(() -> new ConferenceConfig(null, List.of(), consents))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ConferenceConfig(" ", List.of(), consents))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new ConferenceConfig("K", List.of(option("a", true), option("a", false)), consents))
        .hasMessageContaining("duplicate option id a");
    assertThatThrownBy(
            () ->
                new ConferenceConfig(
                    "K",
                    List.of(),
                    List.of(
                        new ConsentDefinition("c", "t", true),
                        new ConsentDefinition("c", "u", false))))
        .hasMessageContaining("duplicate consent id c");
  }

  @Test
  void configConsentsAreImmutableCopies() {
    List<ConsentDefinition> consents =
        new ArrayList<>(List.of(new ConsentDefinition("c", "t", true)));
    ConferenceConfig config = new ConferenceConfig("K", List.of(), consents);
    consents.clear();

    assertThat(config.consents()).hasSize(1);
  }

  @Test
  void acceptGivesIdTimeAndConsentTimes() {
    ValidRegistration valid =
        new ValidRegistration(
            RegistrationType.EXTERNAL,
            "A",
            "B",
            "a@b.org",
            "O",
            null,
            null,
            null,
            List.of("x"),
            List.of("privacy", "photos"));
    UUID id = UUID.randomUUID();
    Instant at = Instant.parse("2026-10-20T10:00:00.123456Z");

    Registration registration = valid.accept(id, at);

    assertThat(registration.id()).isEqualTo(id);
    assertThat(registration.registeredAt()).isEqualTo(at);
    assertThat(registration.type()).isEqualTo(RegistrationType.EXTERNAL);
    assertThat(registration.firstName()).isEqualTo("A");
    assertThat(registration.lastName()).isEqualTo("B");
    assertThat(registration.email()).isEqualTo("a@b.org");
    assertThat(registration.organization()).isEqualTo("O");
    assertThat(registration.options()).containsExactly("x");
    assertThat(registration.consents())
        .containsExactly(new GivenConsent("privacy", at), new GivenConsent("photos", at));
  }

  @Test
  void submissionCopiesListsAndKeepsNulls() {
    List<String> options = new ArrayList<>(Arrays.asList("a", null));
    RegistrationSubmission submission =
        new RegistrationSubmission(
            "T", null, null, null, null, null, null, null, options, null, null);
    options.clear();

    assertThat(submission.options()).containsExactly("a", "null");
    assertThat(submission.consents()).isNull();
  }

  @Test
  void validationExceptionCopiesErrors() {
    List<FieldError> errors = new ArrayList<>(List.of(new FieldError("email", "bad")));
    ValidationException e = new ValidationException(errors);
    errors.clear();

    assertThat(e.errors()).containsExactly(new FieldError("email", "bad"));
    assertThat(e).hasMessageContaining("1 field error");
  }
}
