package si.konferenca.registration.domain;

import java.util.Set;

/**
 * A selectable workshop, event, meal or other activity (BR-04), offered to the registration types
 * in {@code availableTo} (OQ-01).
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

  /** True when the option can be selected in a registration of this type. */
  public boolean isOfferedTo(RegistrationType type) {
    return active && availableTo.contains(type);
  }
}
