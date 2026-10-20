package si.konferenca.registration.application;

import si.konferenca.registration.domain.ConferenceConfig;

/** What the registration form needs: the conference programme and the reCAPTCHA site settings. */
public class FormConfigService {

  private final ConferenceConfig conference;
  private final String captchaSiteKey;
  private final boolean captchaTestMode;

  public FormConfigService(
      ConferenceConfig conference, String captchaSiteKey, boolean captchaTestMode) {
    this.conference = conference;
    this.captchaSiteKey = captchaSiteKey == null ? "" : captchaSiteKey;
    this.captchaTestMode = captchaTestMode;
  }

  public ConferenceConfig conference() {
    return conference;
  }

  /** The reCAPTCHA v2 site key from configuration (AR-07); empty in test mode. */
  public String captchaSiteKey() {
    return captchaSiteKey;
  }

  public boolean captchaTestMode() {
    return captchaTestMode;
  }
}
