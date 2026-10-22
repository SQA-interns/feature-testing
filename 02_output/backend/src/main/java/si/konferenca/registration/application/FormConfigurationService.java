package si.konferenca.registration.application;

import java.util.List;
import org.springframework.stereotype.Service;
import si.konferenca.registration.domain.ConferenceConfiguration;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;

/** The configuration the registration form is built from (US-003, AC-003-01). */
@Service
public class FormConfigurationService {

  private final ConferenceConfiguration configuration;
  private final CaptchaSettings captcha;

  public FormConfigurationService(ConferenceConfiguration configuration, CaptchaSettings captcha) {
    this.configuration = configuration;
    this.captcha = captcha;
  }

  /** Active options, every consent and the public captcha settings. */
  public FormConfiguration formConfiguration() {
    return new FormConfiguration(configuration.activeOptions(), configuration.consents(), captcha);
  }

  /** A snapshot of the form configuration. */
  public record FormConfiguration(
      List<ConferenceOption> options, List<ConsentDefinition> consents, CaptchaSettings captcha) {}
}
