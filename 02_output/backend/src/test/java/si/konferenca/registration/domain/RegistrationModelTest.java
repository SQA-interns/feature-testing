package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Registration, SearchText, RegistrationType and ConferenceOption. */
class RegistrationModelTest {

  private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

  private static RegistrationData data(String organization, String institution) {
    return new RegistrationData(
        RegistrationType.EXTERNAL,
        "Ana",
        "ČEŠNJEVEC",
        "Ana@Example.org",
        organization,
        institution,
        null,
        null,
        List.of("ws-ai"),
        List.of("privacy", "photo"));
  }

  @Test
  void acceptTruncatesTheTimeToMillisecondsAndGivesEveryConsentThen() {
    Instant now = Instant.parse("2026-10-22T10:15:30.123456789Z");

    Registration r = Registration.accept(ID, data("Org", null), now);

    Instant expected = Instant.parse("2026-10-22T10:15:30.123Z");
    assertThat(r.id()).isEqualTo(ID);
    assertThat(r.registeredAt()).isEqualTo(expected);
    assertThat(r.consents())
        .containsExactly(
            new Registration.GivenConsent("privacy", expected),
            new Registration.GivenConsent("photo", expected));
    assertThat(r.type()).isEqualTo(RegistrationType.EXTERNAL);
    assertThat(r.firstName()).isEqualTo("Ana");
    assertThat(r.lastName()).isEqualTo("ČEŠNJEVEC");
    assertThat(r.email()).isEqualTo("Ana@Example.org");
    assertThat(r.organization()).isEqualTo("Org");
    assertThat(r.options()).containsExactly("ws-ai");
  }

  @Test
  void listsAreCopied() {
    List<String> options = new ArrayList<>(List.of("a"));
    RegistrationData d =
        new RegistrationData(
            RegistrationType.STUDENT, "a", "b", "c", null, "i", "p", "s", options, List.of());
    options.add("b");

    assertThat(d.options()).containsExactly("a");
  }

  @Test
  void searchTextJoinsLowerCasedFieldsWithTheSeparator() {
    Registration r = Registration.accept(ID, data("Org\u001FX", null), Instant.EPOCH);

    assertThat(SearchText.of(r))
        .isEqualTo("ana\u001Fčešnjevec\u001Fana@example.org\u001Forgx\u001F");
  }

  @Test
  void searchTextUsesTheStudyInstitution() {
    Registration r = Registration.accept(ID, data(null, "UNI"), Instant.EPOCH);

    assertThat(SearchText.of(r)).endsWith("\u001F\u001Funi");
  }

  @Test
  void normalizeLowerCasesWithoutLocaleAndRemovesTheSeparator() {
    assertThat(SearchText.normalize("ČŠŽ I\u001Fİ")).isEqualTo("čšž i" + "i̇");
  }

  @Test
  void parseAcceptsOnlyExactNames() {
    assertThat(RegistrationType.parse("STUDENT")).contains(RegistrationType.STUDENT);
    assertThat(RegistrationType.parse("EXTERNAL")).contains(RegistrationType.EXTERNAL);
    assertThat(RegistrationType.parse("student")).isEmpty();
    assertThat(RegistrationType.parse(null)).isEmpty();
  }

  @Test
  void optionIsSelectableOnlyWhenActiveAndAvailable() {
    ConferenceOption forStudents =
        new ConferenceOption(
            "o", "O", OptionCategory.OTHER, true, Set.of(RegistrationType.STUDENT));
    ConferenceOption inactive =
        new ConferenceOption(
            "o", "O", OptionCategory.OTHER, false, Set.of(RegistrationType.values()));

    assertThat(forStudents.selectableBy(RegistrationType.STUDENT)).isTrue();
    assertThat(forStudents.selectableBy(RegistrationType.EXTERNAL)).isFalse();
    assertThat(inactive.selectableBy(RegistrationType.STUDENT)).isFalse();
  }

  @Test
  void invalidConfigurationMessageNamesTheProblem() {
    assertThat(new InvalidConfigurationException("x"))
        .hasMessage("Invalid conference configuration: x");
  }
}
