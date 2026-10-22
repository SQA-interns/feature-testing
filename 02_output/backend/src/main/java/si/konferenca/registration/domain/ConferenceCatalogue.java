package si.konferenca.registration.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The configured options and consents (US-003). Construction checks the rules of AR-04, so an
 * instance is always valid.
 */
public final class ConferenceCatalogue {

  private static final Pattern ID = Pattern.compile("[a-z0-9-]{1,64}");

  private final List<ConferenceOption> options;
  private final List<Consent> consents;

  public ConferenceCatalogue(List<ConferenceOption> options, List<Consent> consents) {
    this.options = List.copyOf(options);
    this.consents = List.copyOf(consents);
    Set<String> optionIds = new HashSet<>();
    for (ConferenceOption option : this.options) {
      requireId(option.id(), optionIds, "option");
      requireText(option.name(), "name of option " + option.id());
      if (option.category() == null) {
        throw new InvalidConfigurationException("option " + option.id() + " has no category");
      }
      if (option.availableTo().isEmpty()) {
        throw new InvalidConfigurationException(
            "option " + option.id() + " is available to no registration type");
      }
    }
    Set<String> consentIds = new HashSet<>();
    for (Consent consent : this.consents) {
      requireId(consent.id(), consentIds, "consent");
      requireText(consent.text(), "text of consent " + consent.id());
    }
  }

  private static void requireId(String id, Set<String> seen, String kind) {
    if (id == null || !ID.matcher(id).matches()) {
      throw new InvalidConfigurationException("invalid " + kind + " id: " + id);
    }
    if (!seen.add(id)) {
      throw new InvalidConfigurationException("duplicate " + kind + " id: " + id);
    }
  }

  private static void requireText(String text, String what) {
    if (text == null || text.isBlank()) {
      throw new InvalidConfigurationException("missing " + what);
    }
  }

  /** Every configured option, active or not, in configuration order. */
  public List<ConferenceOption> options() {
    return options;
  }

  public List<ConferenceOption> activeOptions() {
    return options.stream().filter(ConferenceOption::active).toList();
  }

  public List<Consent> consents() {
    return consents;
  }

  public Optional<ConferenceOption> option(String id) {
    return options.stream().filter(o -> o.id().equals(id)).findFirst();
  }

  public boolean hasConsent(String id) {
    return consents.stream().anyMatch(c -> c.id().equals(id));
  }

  public List<Consent> mandatoryConsents() {
    return consents.stream().filter(Consent::mandatory).toList();
  }
}
