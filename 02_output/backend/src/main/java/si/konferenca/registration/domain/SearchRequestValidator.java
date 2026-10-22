package si.konferenca.registration.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Validates the search parameters (AC-009-08, SB-01): {@code text} at most 100 characters, {@code
 * type} {@code EXTERNAL} or {@code STUDENT}, {@code option} a configured option id (active or not),
 * {@code page} a whole number from 1. An empty parameter counts as absent.
 */
public final class SearchRequestValidator {

  private final ConferenceCatalogue catalogue;

  public SearchRequestValidator(ConferenceCatalogue catalogue) {
    this.catalogue = catalogue;
  }

  /** Errors, or (when there are none) the criteria. */
  public record Result(List<FieldError> errors, SearchCriteria criteria) {
    public Result {
      errors = List.copyOf(errors);
    }
  }

  public Result validate(String text, String type, String option, String page) {
    List<FieldError> errors = new ArrayList<>();
    String searchText = text == null ? "" : text;
    if (searchText.length() > SearchCriteria.TEXT_MAX) {
      errors.add(new FieldError("text", "Use at most " + SearchCriteria.TEXT_MAX + " characters."));
    }
    RegistrationType registrationType = null;
    if (present(type)) {
      Optional<RegistrationType> parsed = RegistrationType.parse(type);
      if (parsed.isEmpty()) {
        errors.add(new FieldError("type", "Use EXTERNAL or STUDENT."));
      } else {
        registrationType = parsed.get();
      }
    }
    String optionId = null;
    if (present(option)) {
      if (catalogue.option(option).isEmpty()) {
        errors.add(new FieldError("option", "Unknown option."));
      } else {
        optionId = option;
      }
    }
    int pageNumber = pageNumber(errors, page);
    if (!errors.isEmpty()) {
      return new Result(List.copyOf(errors), null);
    }
    return new Result(
        List.of(), new SearchCriteria(searchText.strip(), registrationType, optionId, pageNumber));
  }

  private static boolean present(String value) {
    return value != null && !value.isEmpty();
  }

  private static int pageNumber(List<FieldError> errors, String page) {
    if (!present(page)) {
      return 1;
    }
    int number;
    try {
      number = Integer.parseInt(page.strip());
    } catch (NumberFormatException e) {
      number = 0;
    }
    if (number >= 1) {
      return number;
    }
    errors.add(new FieldError("page", "Use a page number of 1 or more."));
    return 1;
  }
}
