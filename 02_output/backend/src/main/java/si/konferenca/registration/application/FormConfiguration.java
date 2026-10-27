package si.konferenca.registration.application;

import java.util.List;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;

/** What the form needs: the conference name, captcha mode, active options and consents. */
public record FormConfiguration(
    String conferenceName,
    boolean captchaTestMode,
    String captchaSiteKey,
    List<ConferenceOption> activeOptions,
    List<ConsentDefinition> consents) {

  /** Copies the lists so the configuration cannot change afterwards. */
  public FormConfiguration {
    activeOptions = List.copyOf(activeOptions);
    consents = List.copyOf(consents);
  }
}
