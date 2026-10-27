package si.konferenca.registration.domain;

import java.util.Set;

/** A configured option (BR-04, OQ-01). */
public record ConferenceOption(
    String id,
    String name,
    OptionCategory category,
    boolean active,
    Set<RegistrationType> availableTo) {

  /** Copies the types so the option cannot change afterwards. */
  public ConferenceOption {
    availableTo = Set.copyOf(availableTo);
  }

  /** Whether the option is offered to the given registration type. */
  public boolean isAvailableTo(RegistrationType type) {
    return availableTo.contains(type);
  }
}
