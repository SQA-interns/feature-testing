package si.konferenca.registration.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** A small catalog and valid submissions for the domain unit tests. */
final class DomainFixtures {

  private DomainFixtures() {}

  static final ConferenceOption WORKSHOP =
      new ConferenceOption(
          "ws-a",
          "Workshop A",
          OptionCategory.WORKSHOP,
          true,
          Set.of(RegistrationType.EXTERNAL, RegistrationType.STUDENT));
  static final ConferenceOption DINNER =
      new ConferenceOption(
          "ev-dinner", "Dinner", OptionCategory.EVENT, true, Set.of(RegistrationType.EXTERNAL));
  static final ConferenceOption LEGACY =
      new ConferenceOption(
          "ws-old", "Old", OptionCategory.WORKSHOP, false, Set.of(RegistrationType.EXTERNAL));
  static final ConsentDefinition PRIVACY = new ConsentDefinition("privacy", "Privacy text", true);
  static final ConsentDefinition PHOTOS = new ConsentDefinition("photos", "Photos text", false);

  static ConferenceCatalog catalog() {
    return new ConferenceCatalog(List.of(WORKSHOP, DINNER, LEGACY), List.of(PRIVACY, PHOTOS));
  }

  static Submission external(List<String> optionIds, List<String> consents) {
    return new Submission(
        "EXTERNAL",
        "Ana",
        "Novak",
        "ana@example.org",
        "Institute",
        null,
        null,
        null,
        optionIds,
        consents,
        "token");
  }

  static Submission external() {
    return external(List.of("ws-a"), List.of("privacy"));
  }

  static Submission student() {
    return new Submission(
        "STUDENT",
        "Žiga",
        "Šuštar",
        "ziga@student.example.org",
        null,
        "University",
        "Informatics",
        "63200001",
        List.of("ws-a"),
        List.of("privacy", "photos"),
        "token");
  }

  /** A list with a null element, which {@code List.of} does not allow. */
  static List<String> withNull(String... values) {
    List<String> list = new ArrayList<>(List.of(values));
    list.add(null);
    return list;
  }
}
