package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;

/** What the registration form needs: active options, consents and the captcha settings. */
public class FormConfiguration {

  /** The form configuration as offered to participants (AC-003-01). */
  public record View(
      List<ConferenceOption> options,
      List<ConsentDefinition> consents,
      String captchaSiteKey,
      boolean captchaTestMode) {

    public View {
      options = List.copyOf(options);
      consents = List.copyOf(consents);
    }
  }

  private final ConferenceCatalog catalog;
  private final String captchaSiteKey;
  private final boolean captchaTestMode;

  public FormConfiguration(
      ConferenceCatalog catalog, String captchaSiteKey, boolean captchaTestMode) {
    this.catalog = catalog;
    this.captchaSiteKey = captchaSiteKey == null ? "" : captchaSiteKey;
    this.captchaTestMode = captchaTestMode;
  }

  public View current() {
    return new View(catalog.activeOptions(), catalog.consents(), captchaSiteKey, captchaTestMode);
  }
}
