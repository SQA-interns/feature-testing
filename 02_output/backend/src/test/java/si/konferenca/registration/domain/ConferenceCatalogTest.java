package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConferenceCatalogTest {

  private static final Set<RegistrationType> BOTH = Set.of(RegistrationType.values());

  private static ConferenceOption option(String id, String name, boolean active) {
    return new ConferenceOption(id, name, OptionCategory.MEAL, active, BOTH);
  }

  private static final ConsentDefinition PRIVACY = new ConsentDefinition("privacy", "Text", true);

  @Test
  void listsOnlyActiveOptionsInConfigurationOrderAndFindsEveryOption() {
    ConferenceCatalog catalog =
        ConferenceCatalog.of(
            List.of(option("b", "B", true), option("old", "Old", false), option("a", "A", true)),
            List.of(PRIVACY));

    assertThat(catalog.activeOptions()).extracting(ConferenceOption::id).containsExactly("b", "a");
    assertThat(catalog.option("old")).map(ConferenceOption::name).contains("Old");
    assertThat(catalog.option("missing")).isEmpty();
    assertThat(catalog.consents()).containsExactly(PRIVACY);
    assertThat(catalog.consent("privacy")).contains(PRIVACY);
    assertThat(catalog.consent("other")).isEmpty();
  }

  @Test
  void rejectsDuplicateOptionIds() {
    assertThatThrownBy(
            () ->
                ConferenceCatalog.of(
                    List.of(option("a", "A", true), option("a", "B", true)), List.of()))
        .isInstanceOf(ConferenceConfigException.class)
        .hasMessage("Duplicate option id 'a'");
  }

  @Test
  void rejectsBlankOrMissingIdsNamesAndTexts() {
    assertThatThrownBy(() -> ConferenceCatalog.of(List.of(option(" ", "A", true)), List.of()))
        .hasMessage("Missing option id");
    assertThatThrownBy(() -> ConferenceCatalog.of(List.of(option(null, "A", true)), List.of()))
        .hasMessage("Missing option id");
    assertThatThrownBy(() -> ConferenceCatalog.of(List.of(option("a", "", true)), List.of()))
        .hasMessage("Missing name of option 'a'");
    assertThatThrownBy(
            () -> ConferenceCatalog.of(List.of(), List.of(new ConsentDefinition("", "T", true))))
        .hasMessage("Missing consent id");
    assertThatThrownBy(
            () -> ConferenceCatalog.of(List.of(), List.of(new ConsentDefinition("c", " ", true))))
        .hasMessage("Missing text of consent 'c'");
  }

  @Test
  void rejectsMissingCategoryAndEmptyAvailability() {
    ConferenceOption noCategory = new ConferenceOption("a", "A", null, true, BOTH);
    assertThatThrownBy(() -> ConferenceCatalog.of(List.of(noCategory), List.of()))
        .hasMessage("Option 'a' has no category");
    ConferenceOption nobody = new ConferenceOption("a", "A", OptionCategory.OTHER, true, null);
    assertThatThrownBy(() -> ConferenceCatalog.of(List.of(nobody), List.of()))
        .hasMessage("Option 'a' is available to no registration type");
  }

  @Test
  void rejectsDuplicateConsentIds() {
    assertThatThrownBy(() -> ConferenceCatalog.of(List.of(), List.of(PRIVACY, PRIVACY)))
        .hasMessage("Duplicate consent id 'privacy'");
  }

  @Test
  void optionKnowsWhichTypesItIsAvailableTo() {
    ConferenceOption tour =
        new ConferenceOption(
            "t", "T", OptionCategory.EVENT, true, Set.of(RegistrationType.STUDENT));

    assertThat(tour.isAvailableTo(RegistrationType.STUDENT)).isTrue();
    assertThat(tour.isAvailableTo(RegistrationType.EXTERNAL)).isFalse();
  }

  @Test
  void acceptedRegistrationGivesEveryConsentAtTheRegistrationTime() {
    Instant at = Instant.parse("2026-10-21T10:00:00.123Z");
    UUID id = UUID.randomUUID();
    ValidRegistration valid =
        new ValidRegistration(
            RegistrationType.STUDENT,
            "A",
            "B",
            "a@b.si",
            null,
            "UL",
            "CS",
            "1",
            List.of("x"),
            List.of("privacy", "photos"));

    Registration r = Registration.accept(valid, id, at);

    assertThat(r.id()).isEqualTo(id);
    assertThat(r.registeredAt()).isEqualTo(at);
    assertThat(r.consents())
        .containsExactly(new GivenConsent("privacy", at), new GivenConsent("photos", at));
    assertThat(r)
        .extracting(
            Registration::type,
            Registration::firstName,
            Registration::lastName,
            Registration::email,
            Registration::organization,
            Registration::studyInstitution,
            Registration::studyProgramme,
            Registration::studentId,
            Registration::options)
        .containsExactly(
            RegistrationType.STUDENT, "A", "B", "a@b.si", null, "UL", "CS", "1", List.of("x"));
  }

  @Test
  void configExceptionKeepsItsCause() {
    IllegalStateException cause = new IllegalStateException("x");
    assertThat(new ConferenceConfigException("m", cause)).hasMessage("m").hasCause(cause);
  }
}
