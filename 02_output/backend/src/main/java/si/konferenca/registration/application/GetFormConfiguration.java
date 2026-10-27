package si.konferenca.registration.application;

import org.springframework.stereotype.Service;
import si.konferenca.registration.domain.ConferenceCatalog;

/** Use case: the form configuration with active options only (AC-003-01). */
@Service
public class GetFormConfiguration {

  private final ConferenceCatalog catalog;
  private final FormSettings settings;

  public GetFormConfiguration(ConferenceCatalog catalog, FormSettings settings) {
    this.catalog = catalog;
    this.settings = settings;
  }

  public FormConfiguration get() {
    return new FormConfiguration(
        settings.conferenceName(),
        settings.captchaTestMode(),
        settings.captchaSiteKey(),
        catalog.activeOptions(),
        catalog.consents());
  }
}
