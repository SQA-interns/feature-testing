package si.konferenca.registration.domain;

import java.util.Optional;

/** The two registration types of BR-01. */
public enum RegistrationType {
  EXTERNAL,
  STUDENT;

  /** The type with exactly this name, or empty (names are case-sensitive). */
  public static Optional<RegistrationType> parse(String name) {
    for (RegistrationType type : values()) {
      if (type.name().equals(name)) {
        return Optional.of(type);
      }
    }
    return Optional.empty();
  }
}
