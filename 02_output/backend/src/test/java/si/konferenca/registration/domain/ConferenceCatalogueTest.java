package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConferenceCatalogueTest {

  private static final Consent PRIVACY = new Consent("privacy", "I agree.", true);

  private static ConferenceCatalogue withOption(ConferenceOption option) {
    return new ConferenceCatalogue(List.of(option), List.of(PRIVACY));
  }

  @Test
  void lookupsAnswerFromTheConfiguration() {
    ConferenceCatalogue catalogue = Catalogues.standard();

    assertThat(catalogue.options())
        .extracting(ConferenceOption::id)
        .containsExactly("ws", "ex", "st", "old");
    assertThat(catalogue.activeOptions())
        .extracting(ConferenceOption::id)
        .containsExactly("ws", "ex", "st");
    assertThat(catalogue.option("ex")).isPresent();
    assertThat(catalogue.option("missing")).isEmpty();
    assertThat(catalogue.hasConsent("photos")).isTrue();
    assertThat(catalogue.hasConsent("missing")).isFalse();
    assertThat(catalogue.mandatoryConsents()).extracting(Consent::id).containsExactly("privacy");
    assertThat(catalogue.consents()).hasSize(2);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "Upper", "with space", "under_score"})
  void invalidOptionIdIsRefused(String id) {
    assertThatThrownBy(() -> withOption(Catalogues.option(id, OptionCategory.OTHER)))
        .isInstanceOf(InvalidConfigurationException.class)
        .hasMessageContaining("invalid option id");
  }

  @Test
  void idOfSixtyFourCharactersIsAllowedButNotLonger() {
    assertThat(withOption(Catalogues.option("a".repeat(64), OptionCategory.OTHER)).options())
        .hasSize(1);
    assertThatThrownBy(() -> withOption(Catalogues.option("a".repeat(65), OptionCategory.OTHER)))
        .isInstanceOf(InvalidConfigurationException.class);
  }

  @Test
  void nullOptionIdIsRefused() {
    assertThatThrownBy(() -> withOption(Catalogues.option(null, OptionCategory.OTHER)))
        .isInstanceOf(InvalidConfigurationException.class);
  }

  @Test
  void duplicateOptionIdIsRefused() {
    ConferenceOption ws = Catalogues.option("ws", OptionCategory.WORKSHOP);
    assertThatThrownBy(() -> new ConferenceCatalogue(List.of(ws, ws), List.of(PRIVACY)))
        .isInstanceOf(InvalidConfigurationException.class)
        .hasMessageContaining("duplicate option id: ws");
  }

  @Test
  void blankOrMissingNameIsRefused() {
    Set<RegistrationType> both = EnumSet.allOf(RegistrationType.class);
    assertThatThrownBy(
            () -> withOption(new ConferenceOption("ws", " ", OptionCategory.OTHER, true, both)))
        .hasMessageContaining("missing name of option ws");
    assertThatThrownBy(
            () -> withOption(new ConferenceOption("ws", null, OptionCategory.OTHER, true, both)))
        .isInstanceOf(InvalidConfigurationException.class);
  }

  @Test
  void missingCategoryOrTypesAreRefused() {
    Set<RegistrationType> both = EnumSet.allOf(RegistrationType.class);
    assertThatThrownBy(() -> withOption(new ConferenceOption("ws", "W", null, true, both)))
        .hasMessageContaining("has no category");
    assertThatThrownBy(
            () -> withOption(new ConferenceOption("ws", "W", OptionCategory.OTHER, true, Set.of())))
        .hasMessageContaining("available to no registration type");
  }

  @Test
  void consentRulesAreChecked() {
    assertThatThrownBy(() -> new ConferenceCatalogue(List.of(), List.of(PRIVACY, PRIVACY)))
        .hasMessageContaining("duplicate consent id: privacy");
    assertThatThrownBy(
            () -> new ConferenceCatalogue(List.of(), List.of(new Consent("privacy", "", true))))
        .hasMessageContaining("missing text of consent privacy");
    assertThatThrownBy(
            () -> new ConferenceCatalogue(List.of(), List.of(new Consent("Bad", "Text", true))))
        .hasMessageContaining("invalid consent id");
  }

  @Test
  void theCatalogueKeepsItsOwnCopies() {
    List<ConferenceOption> options =
        new ArrayList<>(List.of(Catalogues.option("ws", OptionCategory.OTHER)));
    ConferenceCatalogue catalogue = new ConferenceCatalogue(options, List.of(PRIVACY));
    options.clear();

    assertThat(catalogue.options()).hasSize(1);
  }

  @Test
  void optionsAreOfferedOnlyWhenActiveAndAvailable() {
    ConferenceOption externalOnly =
        Catalogues.option("ex", OptionCategory.EVENT, RegistrationType.EXTERNAL);

    assertThat(externalOnly.isOfferedTo(RegistrationType.EXTERNAL)).isTrue();
    assertThat(externalOnly.isOfferedTo(RegistrationType.STUDENT)).isFalse();
    assertThat(Catalogues.inactive("old").isOfferedTo(RegistrationType.EXTERNAL)).isFalse();
  }

  @Test
  void enumParsingIsExactAndCaseSensitive() {
    assertThat(RegistrationType.parse("STUDENT")).contains(RegistrationType.STUDENT);
    assertThat(RegistrationType.parse("student")).isEmpty();
    assertThat(RegistrationType.parse(null)).isEmpty();
    assertThat(OptionCategory.parse("MEAL")).contains(OptionCategory.MEAL);
    assertThat(OptionCategory.parse("meal")).isEmpty();
  }

  @Test
  void registrationListsItsConsentIdsAndKeepsCopies() {
    Instant now = Instant.parse("2026-10-20T10:00:00Z");
    List<String> options = new ArrayList<>(List.of("ws"));
    Registration registration =
        new Registration(
            UUID.randomUUID(),
            new Participant(RegistrationType.EXTERNAL, "A", "B", "a@b.co", "Org", null, null, null),
            options,
            List.of(new GivenConsent("privacy", now), new GivenConsent("photos", now)),
            now);
    options.add("other");

    assertThat(registration.consentIds()).containsExactly("privacy", "photos");
    assertThat(registration.options()).containsExactly("ws");
  }
}
