package si.konferenca.registration.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The configured options and consents, in configuration order (AR-04). */
public final class ConferenceCatalog {

  private final List<ConferenceOption> options;
  private final List<ConsentDefinition> consents;
  private final Map<String, ConferenceOption> optionsById = new LinkedHashMap<>();
  private final Map<String, ConsentDefinition> consentsById = new LinkedHashMap<>();

  /**
   * Creates the catalog.
   *
   * @throws IllegalArgumentException when an option or consent id is not unique
   */
  public ConferenceCatalog(List<ConferenceOption> options, List<ConsentDefinition> consents) {
    this.options = List.copyOf(options);
    this.consents = List.copyOf(consents);
    for (ConferenceOption option : this.options) {
      if (optionsById.putIfAbsent(option.id(), option) != null) {
        throw new IllegalArgumentException("duplicate option id: " + option.id());
      }
    }
    for (ConsentDefinition consent : this.consents) {
      if (consentsById.putIfAbsent(consent.id(), consent) != null) {
        throw new IllegalArgumentException("duplicate consent id: " + consent.id());
      }
    }
  }

  /** All options, active and inactive. */
  public List<ConferenceOption> options() {
    return options;
  }

  /** Active options only. */
  public List<ConferenceOption> activeOptions() {
    return options.stream().filter(ConferenceOption::active).toList();
  }

  public List<ConsentDefinition> consents() {
    return consents;
  }

  public Optional<ConferenceOption> option(String id) {
    return Optional.ofNullable(optionsById.get(id));
  }

  public Optional<ConsentDefinition> consent(String id) {
    return Optional.ofNullable(consentsById.get(id));
  }
}
