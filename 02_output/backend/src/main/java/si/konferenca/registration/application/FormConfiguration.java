package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.ConferenceCatalogue;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.Consent;

/** What the registration form offers: the active options and every consent (AC-003-01). */
public class FormConfiguration {

  private final ConferenceCatalogue catalogue;

  public FormConfiguration(ConferenceCatalogue catalogue) {
    this.catalogue = catalogue;
  }

  public List<ConferenceOption> activeOptions() {
    return catalogue.activeOptions();
  }

  public List<Consent> consents() {
    return catalogue.consents();
  }
}
