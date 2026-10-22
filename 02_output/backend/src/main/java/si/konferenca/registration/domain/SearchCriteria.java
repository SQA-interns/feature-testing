package si.konferenca.registration.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A valid organizer search (BR-10): normalized text (empty matches all), optional type and option
 * filters, and a page of {@link #PAGE_SIZE} registrations.
 */
public record SearchCriteria(
    String text, Optional<RegistrationType> type, Optional<String> option, int page) {

  public static final int PAGE_SIZE = 20;
  public static final int MAX_TEXT_LENGTH = 100;

  /** Index of the first registration of the page. */
  public int offset() {
    return (page - 1) * PAGE_SIZE;
  }

  /** The outcome of parsing: either errors or criteria, never both. */
  public record Parsed(List<FieldError> errors, SearchCriteria criteria) {}

  /**
   * Parses the query parameters (AC-009-08, SB-01); an empty parameter counts as absent. The text
   * limit applies to the raw text (D-11); option ids must be configured, active or not.
   */
  public static Parsed parse(
      String text, String type, String option, String page, ConferenceConfiguration config) {
    List<FieldError> errors = new ArrayList<>();
    String rawText = text == null ? "" : text;
    if (rawText.length() > MAX_TEXT_LENGTH) {
      errors.add(new FieldError("text", "At most " + MAX_TEXT_LENGTH + " characters."));
    }
    Optional<RegistrationType> parsedType = Optional.empty();
    if (present(type)) {
      parsedType = RegistrationType.parse(type);
      if (parsedType.isEmpty()) {
        errors.add(new FieldError("type", "Choose EXTERNAL or STUDENT."));
      }
    }
    Optional<String> parsedOption = Optional.empty();
    if (present(option)) {
      parsedOption = config.option(option).map(ConferenceOption::id);
      if (parsedOption.isEmpty()) {
        errors.add(new FieldError("option", "Unknown option."));
      }
    }
    int parsedPage = 1;
    if (present(page)) {
      parsedPage = parsePage(page);
      if (parsedPage < 1) {
        errors.add(new FieldError("page", "The page must be a number from 1."));
      }
    }
    if (!errors.isEmpty()) {
      return new Parsed(List.copyOf(errors), null);
    }
    return new Parsed(
        List.of(),
        new SearchCriteria(
            SearchText.normalize(rawText.strip()), parsedType, parsedOption, parsedPage));
  }

  private static boolean present(String value) {
    return value != null && !value.isEmpty();
  }

  private static int parsePage(String page) {
    try {
      return Integer.parseInt(page);
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
