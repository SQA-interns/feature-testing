package si.konferenca.registration.domain;

import java.util.Set;

/**
 * A configurable workshop, event, meal or other activity (US-003) with the registration types it is
 * available to (OQ-01).
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

  /** Whether a participant of the given type may select this option (BR-04, OQ-01). */
  public boolean isSelectableBy(RegistrationType type) {
    return active && availableTo.contains(type);
  }
}
