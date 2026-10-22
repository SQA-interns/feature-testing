package si.konferenca.registration.domain;

import java.util.Optional;

/** Option groups of BR-04. */
public enum OptionCategory {
  WORKSHOP,
  EVENT,
  MEAL,
  OTHER;

  /** The category with exactly this name, or empty. */
  public static Optional<OptionCategory> parse(String name) {
    for (OptionCategory category : values()) {
      if (category.name().equals(name)) {
        return Optional.of(category);
      }
    }
    return Optional.empty();
  }
}
