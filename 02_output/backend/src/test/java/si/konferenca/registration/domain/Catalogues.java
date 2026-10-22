package si.konferenca.registration.domain;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Catalogues for the domain unit tests. */
final class Catalogues {

  private Catalogues() {}

  static ConferenceOption option(String id, OptionCategory category, RegistrationType... types) {
    Set<RegistrationType> available =
        types.length == 0 ? EnumSet.allOf(RegistrationType.class) : Set.of(types);
    return new ConferenceOption(id, "Name of " + id, category, true, available);
  }

  static ConferenceOption inactive(String id) {
    return new ConferenceOption(
        id, "Name of " + id, OptionCategory.OTHER, false, EnumSet.allOf(RegistrationType.class));
  }

  /** ws (both), ex (external only), st (students only), old (inactive); privacy mandatory. */
  static ConferenceCatalogue standard() {
    return new ConferenceCatalogue(
        List.of(
            option("ws", OptionCategory.WORKSHOP),
            option("ex", OptionCategory.EVENT, RegistrationType.EXTERNAL),
            option("st", OptionCategory.MEAL, RegistrationType.STUDENT),
            inactive("old")),
        List.of(new Consent("privacy", "I agree.", true), new Consent("photos", "Photos.", false)));
  }
}
