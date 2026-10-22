package si.konferenca.registration.domain;

import java.util.Optional;

/** The two fixed registration types (BR-01). */
public enum RegistrationType {
  EXTERNAL,
  STUDENT;

  /** The type with exactly this name, or empty for null or any other text. */
  public static Optional<RegistrationType> parse(String value) {
    for (RegistrationType type : values()) {
      if (type.name().equals(value)) {
        return Optional.of(type);
      }
    }
    return Optional.empty();
  }
}
