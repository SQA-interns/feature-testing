package si.konferenca.registration.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The conference options and consents (AR-04). Construction validates the configuration, so an
 * invalid configuration can never be used (AC-003-03).
 */
public final class ConferenceConfiguration {

  private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9-]{0,63}");

  private final List<ConferenceOption> options;
  private final List<ConsentDefinition> consents;

  public ConferenceConfiguration(List<ConferenceOption> options, List<ConsentDefinition> consents) {
    this.options = List.copyOf(options);
    this.consents = List.copyOf(consents);
    validate();
  }

  private void validate() {
    Set<String> optionIds = new HashSet<>();
    for (ConferenceOption option : options) {
      requireId(option.id(), "option");
      if (!optionIds.add(option.id())) {
        throw new InvalidConfigurationException("duplicate option id " + option.id());
      }
      requireText(option.name(), "name of option " + option.id());
      if (option.category() == null) {
        throw new InvalidConfigurationException("missing category of option " + option.id());
      }
      if (option.availableTo().isEmpty()) {
        throw new InvalidConfigurationException("option " + option.id() + " available to nobody");
      }
    }
    Set<String> consentIds = new HashSet<>();
    for (ConsentDefinition consent : consents) {
      requireId(consent.id(), "consent");
      if (!consentIds.add(consent.id())) {
        throw new InvalidConfigurationException("duplicate consent id " + consent.id());
      }
      requireText(consent.text(), "text of consent " + consent.id());
    }
  }

  private static void requireId(String id, String what) {
    if (id == null || !ID.matcher(id).matches()) {
      throw new InvalidConfigurationException("invalid " + what + " id " + id);
    }
  }

  private static void requireText(String text, String what) {
    if (text == null || text.isBlank()) {
      throw new InvalidConfigurationException("missing " + what);
    }
  }

  public List<ConferenceOption> options() {
    return options;
  }

  public List<ConsentDefinition> consents() {
    return consents;
  }

  /** The options currently offered for selection (BR-04, AC-003-01). */
  public List<ConferenceOption> activeOptions() {
    return options.stream().filter(ConferenceOption::active).toList();
  }

  public Optional<ConferenceOption> option(String id) {
    return options.stream().filter(o -> o.id().equals(id)).findFirst();
  }

  public Optional<ConsentDefinition> consent(String id) {
    return consents.stream().filter(c -> c.id().equals(id)).findFirst();
  }
}
