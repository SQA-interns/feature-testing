package si.konferenca.registration.domain;

import java.util.Set;

/**
 * A configured workshop, event, meal or other activity (US-003, OQ-01).
 *
 * @param availableTo the registration types that may select the option
 */
public record ConferenceOption(
    String id,
    String name,
    OptionCategory category,
    boolean active,
    Set<RegistrationType> availableTo) {

  public ConferenceOption {
    availableTo = Set.copyOf(availableTo);
  }

  /** Whether a registration of the given type may select this option (BR-04, OQ-01). */
  public boolean selectableBy(RegistrationType type) {
    return active && availableTo.contains(type);
  }
}
