package si.konferenca.registration.domain;

import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Case-insensitive search text (BR-10): the searchable fields lower-cased independently of the
 * database collation, so Slovenian characters match in any case (AC-009-11).
 */
public final class SearchText {

  /** Separates the fields, so a search text never matches across two fields. */
  static final char SEPARATOR = '\u001F';

  private SearchText() {}

  /** First name, last name, email, organization and study institution, normalized. */
  public static String of(Registration registration) {
    return Stream.of(
            registration.firstName(),
            registration.lastName(),
            registration.email(),
            registration.organization(),
            registration.studyInstitution())
        .map(field -> field == null ? "" : normalize(field))
        .collect(Collectors.joining(String.valueOf(SEPARATOR)));
  }

  /** A search text or field value: lower case, without the separator character. */
  public static String normalize(String text) {
    return text.replace(String.valueOf(SEPARATOR), "").toLowerCase(Locale.ROOT);
  }
}
