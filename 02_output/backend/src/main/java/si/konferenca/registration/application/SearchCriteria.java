package si.konferenca.registration.application;

import java.util.Map;
import si.konferenca.registration.domain.OptionCategory;

/**
 * The organizer's search as entered: the search text, the registration type and one option id per
 * category; null or blank values restrict nothing (BR-11, BR-12).
 */
public record SearchCriteria(String search, String type, Map<OptionCategory, String> options) {

  /** Copies the filters so the criteria cannot change afterwards. */
  public SearchCriteria {
    options = Map.copyOf(options);
  }
}
