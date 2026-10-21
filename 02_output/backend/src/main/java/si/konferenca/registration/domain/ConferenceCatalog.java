package si.konferenca.registration.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** The configured options and consents, validated once when the backend starts (AR-04). */
public final class ConferenceCatalog {

  private final List<ConferenceOption> options;
  private final List<ConsentDefinition> consents;

  private ConferenceCatalog(List<ConferenceOption> options, List<ConsentDefinition> consents) {
    this.options = List.copyOf(options);
    this.consents = List.copyOf(consents);
  }

  /**
   * Validates and creates the catalog.
   *
   * @throws ConferenceConfigException for a duplicate or blank id, a blank name or text, a missing
   *     category or an option available to no registration type
   */
  public static ConferenceCatalog of(
      List<ConferenceOption> options, List<ConsentDefinition> consents) {
    Set<String> optionIds = new HashSet<>();
    for (ConferenceOption option : options) {
      requireText(option.id(), "option id");
      requireText(option.name(), "name of option '" + option.id() + "'");
      if (option.category() == null) {
        throw new ConferenceConfigException("Option '" + option.id() + "' has no category");
      }
      if (option.availableTo().isEmpty()) {
        throw new ConferenceConfigException(
            "Option '" + option.id() + "' is available to no registration type");
      }
      if (!optionIds.add(option.id())) {
        throw new ConferenceConfigException("Duplicate option id '" + option.id() + "'");
      }
    }
    Set<String> consentIds = new HashSet<>();
    for (ConsentDefinition consent : consents) {
      requireText(consent.id(), "consent id");
      requireText(consent.text(), "text of consent '" + consent.id() + "'");
      if (!consentIds.add(consent.id())) {
        throw new ConferenceConfigException("Duplicate consent id '" + consent.id() + "'");
      }
    }
    return new ConferenceCatalog(options, consents);
  }

  private static void requireText(String value, String what) {
    if (value == null || value.isBlank()) {
      throw new ConferenceConfigException("Missing " + what);
    }
  }

  /** Active options in configuration order. */
  public List<ConferenceOption> activeOptions() {
    return options.stream().filter(ConferenceOption::active).toList();
  }

  /** Any configured option, active or not. */
  public Optional<ConferenceOption> option(String id) {
    return options.stream().filter(o -> o.id().equals(id)).findFirst();
  }

  public List<ConsentDefinition> consents() {
    return consents;
  }

  public Optional<ConsentDefinition> consent(String id) {
    return consents.stream().filter(c -> c.id().equals(id)).findFirst();
  }
}
