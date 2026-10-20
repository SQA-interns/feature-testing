package si.konferenca.registration.domain;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The conference programme: name, options and consents (US-003, AR-04). Participant fields are
 * fixed in code and are not part of the configuration (BR-01).
 */
public final class ConferenceConfig {

  private final String conferenceName;
  private final Map<String, ConferenceOption> options;
  private final List<ConsentDefinition> consents;

  /**
   * @throws IllegalArgumentException when the name is blank or an option or consent id repeats
   */
  public ConferenceConfig(
      String conferenceName, List<ConferenceOption> options, List<ConsentDefinition> consents) {
    if (conferenceName == null || conferenceName.isBlank()) {
      throw new IllegalArgumentException("conferenceName is missing");
    }
    Map<String, ConferenceOption> byId = new LinkedHashMap<>();
    for (ConferenceOption option : options) {
      if (byId.putIfAbsent(option.id(), option) != null) {
        throw new IllegalArgumentException("duplicate option id " + option.id());
      }
    }
    Set<String> consentIds = new HashSet<>();
    for (ConsentDefinition consent : consents) {
      if (!consentIds.add(consent.id())) {
        throw new IllegalArgumentException("duplicate consent id " + consent.id());
      }
    }
    this.conferenceName = conferenceName;
    this.options = byId;
    this.consents = List.copyOf(consents);
  }

  public String conferenceName() {
    return conferenceName;
  }

  /** Active options in configuration order (AC-003-01). */
  public List<ConferenceOption> activeOptions() {
    return options.values().stream().filter(ConferenceOption::active).toList();
  }

  public Optional<ConferenceOption> option(String id) {
    return Optional.ofNullable(options.get(id));
  }

  public List<ConsentDefinition> consents() {
    return consents;
  }

  public Optional<ConsentDefinition> consent(String id) {
    return consents.stream().filter(c -> c.id().equals(id)).findFirst();
  }
}
