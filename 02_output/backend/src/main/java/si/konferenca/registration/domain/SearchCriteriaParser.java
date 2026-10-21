package si.konferenca.registration.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Checks the organizer's search parameters on the server (SB-01, AC-009-08): one {@link FieldError}
 * per invalid parameter, named as the query parameter. Text over 100 characters after trimming
 * (D-26); type exactly {@code EXTERNAL} or {@code STUDENT}, option a configured option id, page a
 * whole number from 1 (D-27).
 */
public final class SearchCriteriaParser {

  static final int MAX_TEXT_LENGTH = 100;
  private static final Pattern PAGE = Pattern.compile("[0-9]{1,6}");

  private final ConferenceConfig conference;

  public SearchCriteriaParser(ConferenceConfig conference) {
    this.conference = conference;
  }

  /**
   * @param text search text or {@code null}
   * @param type registration type filter or {@code null}
   * @param option option id filter or {@code null}
   * @param page page number or {@code null} for the first page
   * @throws ValidationException when any parameter is invalid
   */
  public SearchCriteria parse(String text, String type, String option, String page) {
    List<FieldError> errors = new ArrayList<>();
    String trimmed = text == null ? "" : text.strip();
    if (trimmed.codePointCount(0, trimmed.length()) > MAX_TEXT_LENGTH) {
      errors.add(new FieldError("text", "The search text may have at most 100 characters."));
    }
    RegistrationType registrationType = null;
    if (type != null) {
      registrationType = registrationType(type);
      if (registrationType == null) {
        errors.add(new FieldError("type", "The type must be EXTERNAL or STUDENT."));
      }
    }
    if (option != null && conference.option(option).isEmpty()) {
      errors.add(new FieldError("option", "This option does not exist."));
    }
    int pageNumber = 1;
    if (page != null) {
      pageNumber = PAGE.matcher(page).matches() ? Integer.parseInt(page) : 0;
      if (pageNumber < 1) {
        errors.add(new FieldError("page", "The page must be a whole number of 1 or more."));
      }
    }
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
    return new SearchCriteria(trimmed, registrationType, option, pageNumber);
  }

  private static RegistrationType registrationType(String value) {
    for (RegistrationType candidate : RegistrationType.values()) {
      if (candidate.name().equals(value)) {
        return candidate;
      }
    }
    return null;
  }
}
