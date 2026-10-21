package si.konferenca.registration.domain;

import java.util.Set;

/** A selectable workshop, event, meal or other activity (BR-04, OQ-01). */
public record ConferenceOption(
    String id,
    String name,
    OptionCategory category,
    boolean active,
    Set<RegistrationType> availableTo) {

  public ConferenceOption {
    availableTo = availableTo == null ? Set.of() : Set.copyOf(availableTo);
  }

  public boolean isAvailableTo(RegistrationType type) {
    return availableTo.contains(type);
  }
}
