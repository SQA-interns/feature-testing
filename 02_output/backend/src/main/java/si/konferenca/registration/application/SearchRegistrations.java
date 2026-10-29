package si.konferenca.registration.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import si.konferenca.registration.application.port.RegistrationStore;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ErrorCode;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationFilter;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.SearchRejectedException;
import si.konferenca.registration.domain.SearchText;

/** Use case: the organizer's read-only registration search (US-009, specification S4.9). */
@Service
public class SearchRegistrations {

  static final String SEARCH_FIELD = "search";
  static final String TYPE_FIELD = "type";

  private final RegistrationStore registrations;
  private final ConferenceCatalog catalog;

  public SearchRegistrations(RegistrationStore registrations, ConferenceCatalog catalog) {
    this.registrations = registrations;
    this.catalog = catalog;
  }

  /** The name of a category's filter, as in the API and its field errors. */
  public static String filterField(OptionCategory category) {
    return category.name().toLowerCase(Locale.ROOT);
  }

  /**
   * Lists the matching registrations, newest first, with the option counts.
   *
   * @throws SearchRejectedException when the text is too long or a filter is unknown (AC-009-16,
   *     AC-009-22)
   */
  @Transactional(readOnly = true)
  public SearchResult search(SearchCriteria criteria) {
    RegistrationFilter filter = filterOf(criteria);
    List<Registration> newestFirst = new ArrayList<>(registrations.findAllOldestFirst());
    Collections.reverse(newestFirst);
    List<Registration> matching = newestFirst.stream().filter(filter::matches).toList();
    return new SearchResult(matching, counts(matching));
  }

  private RegistrationFilter filterOf(SearchCriteria criteria) {
    List<FieldError> errors = new ArrayList<>();
    if (!SearchText.fits(SearchText.strip(criteria.search()))) {
      errors.add(new FieldError(SEARCH_FIELD, ErrorCode.TOO_LONG));
    }
    Optional<RegistrationType> type = Optional.empty();
    if (isSet(criteria.type())) {
      type =
          Arrays.stream(RegistrationType.values())
              .filter(t -> t.name().equals(criteria.type()))
              .findFirst();
      if (type.isEmpty()) {
        errors.add(new FieldError(TYPE_FIELD, ErrorCode.UNKNOWN_TYPE));
      }
    }
    List<String> optionIds = new ArrayList<>();
    for (OptionCategory category : OptionCategory.values()) {
      String id = criteria.options().get(category);
      if (isSet(id)) {
        Optional<ConferenceOption> option = catalog.option(id);
        if (option.isEmpty()) {
          errors.add(new FieldError(filterField(category), ErrorCode.UNKNOWN_OPTION));
        } else if (option.get().category() != category) {
          errors.add(new FieldError(filterField(category), ErrorCode.WRONG_CATEGORY));
        } else {
          optionIds.add(id);
        }
      }
    }
    if (!errors.isEmpty()) {
      throw new SearchRejectedException(errors);
    }
    return new RegistrationFilter(SearchText.of(criteria.search()), type, optionIds);
  }

  private static boolean isSet(String value) {
    return value != null && !value.isBlank();
  }

  private List<SearchResult.CategoryCount> counts(List<Registration> matching) {
    List<SearchResult.CategoryCount> counts = new ArrayList<>();
    for (OptionCategory category : OptionCategory.values()) {
      List<SearchResult.OptionCount> options =
          catalog.options().stream()
              .filter(o -> o.category() == category)
              .map(o -> new SearchResult.OptionCount(o, selections(matching, o.id())))
              .toList();
      counts.add(new SearchResult.CategoryCount(category, options));
    }
    return counts;
  }

  private static int selections(List<Registration> matching, String optionId) {
    return (int)
        matching.stream()
            .filter(r -> r.options().stream().anyMatch(o -> o.id().equals(optionId)))
            .count();
  }
}
