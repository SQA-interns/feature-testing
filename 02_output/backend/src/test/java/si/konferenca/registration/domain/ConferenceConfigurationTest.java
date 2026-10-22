package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static si.konferenca.registration.domain.DomainFixtures.configuration;
import static si.konferenca.registration.domain.DomainFixtures.option;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConferenceConfigurationTest {

  private static final List<ConsentDefinition> CONSENTS =
      List.of(new ConsentDefinition("privacy", "I agree.", true));

  private static void assertInvalid(
      List<ConferenceOption> options, List<ConsentDefinition> consents, String message) {
    assertThatThrownBy(() -> new ConferenceConfiguration(options, consents))
        .isInstanceOf(InvalidConfigurationException.class)
        .hasMessage("Invalid conference configuration: " + message);
  }

  @Test
  void activeOptionsLeaveOutInactiveOnesInOrder() {
    assertThat(configuration().activeOptions())
        .extracting(ConferenceOption::id)
        .containsExactly("ws-ai", "ws-cloud", "ev-night");
    assertThat(configuration().options()).hasSize(4);
    assertThat(configuration().consents())
        .extracting(ConsentDefinition::id)
        .containsExactly("privacy", "photo");
  }

  @Test
  void findsOptionsAndConsentsById() {
    ConferenceConfiguration config = configuration();

    assertThat(config.option("meal-old")).map(ConferenceOption::name).contains("Old meal");
    assertThat(config.option("none")).isEmpty();
    assertThat(config.consent("photo")).map(ConsentDefinition::mandatory).contains(false);
    assertThat(config.consent("none")).isEmpty();
  }

  @Test
  void listsAreCopied() {
    List<ConferenceOption> options = new ArrayList<>(List.of(option("a", OptionCategory.MEAL)));
    ConferenceConfiguration config = new ConferenceConfiguration(options, CONSENTS);
    options.clear();

    assertThat(config.options()).hasSize(1);
  }

  @Test
  void rejectsDuplicateOptionId() {
    assertInvalid(
        List.of(option("a", OptionCategory.MEAL), option("a", OptionCategory.EVENT)),
        CONSENTS,
        "duplicate option id a");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "A", "-a", "a_b", "a b", "ä"})
  void rejectsInvalidOptionId(String id) {
    assertInvalid(List.of(option(id, OptionCategory.MEAL)), CONSENTS, "invalid option id " + id);
  }

  @Test
  void acceptsIdsOfOneToSixtyFourCharacters() {
    String longest = "a" + "-".repeat(63);

    assertThat(
            new ConferenceConfiguration(
                    List.of(option("0", OptionCategory.MEAL), option(longest, OptionCategory.MEAL)),
                    CONSENTS)
                .options())
        .hasSize(2);
    assertInvalid(
        List.of(option(longest + "a", OptionCategory.MEAL)),
        CONSENTS,
        "invalid option id " + longest + "a");
  }

  @Test
  void rejectsNullOptionId() {
    assertInvalid(
        List.of(
            new ConferenceOption(
                null, "n", OptionCategory.MEAL, true, Set.of(RegistrationType.STUDENT))),
        CONSENTS,
        "invalid option id null");
  }

  @Test
  void rejectsMissingOrBlankName() {
    for (String name : new String[] {null, " "}) {
      assertInvalid(
          List.of(
              new ConferenceOption(
                  "a", name, OptionCategory.MEAL, true, Set.of(RegistrationType.STUDENT))),
          CONSENTS,
          "missing name of option a");
    }
  }

  @Test
  void rejectsMissingCategory() {
    assertInvalid(
        List.of(new ConferenceOption("a", "n", null, true, Set.of(RegistrationType.STUDENT))),
        CONSENTS,
        "missing category of option a");
  }

  @Test
  void rejectsOptionAvailableToNobody() {
    assertInvalid(
        List.of(new ConferenceOption("a", "n", OptionCategory.MEAL, true, Set.of())),
        CONSENTS,
        "option a available to nobody");
  }

  @Test
  void rejectsDuplicateConsentId() {
    assertInvalid(
        List.of(),
        List.of(new ConsentDefinition("p", "x", true), new ConsentDefinition("p", "y", false)),
        "duplicate consent id p");
  }

  @Test
  void rejectsInvalidConsentIdAndMissingText() {
    assertInvalid(
        List.of(), List.of(new ConsentDefinition("P", "x", true)), "invalid consent id P");
    assertInvalid(
        List.of(), List.of(new ConsentDefinition("p", "", true)), "missing text of consent p");
    assertInvalid(
        List.of(), List.of(new ConsentDefinition("p", null, true)), "missing text of consent p");
  }
}
