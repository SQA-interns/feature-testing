package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;

/**
 * The matching registrations, newest first, and per category the number of them that selected each
 * configured option (BR-13).
 */
public record SearchResult(List<Registration> registrations, List<CategoryCount> optionCounts) {

  /** Copies the lists so the result cannot change afterwards. */
  public SearchResult {
    registrations = List.copyOf(registrations);
    optionCounts = List.copyOf(optionCounts);
  }

  /** The counts of one category, every configured option in configuration order. */
  public record CategoryCount(OptionCategory category, List<OptionCount> options) {

    /** Copies the list so the count cannot change afterwards. */
    public CategoryCount {
      options = List.copyOf(options);
    }
  }

  /** How many matching registrations selected the option. */
  public record OptionCount(ConferenceOption option, int count) {}
}
