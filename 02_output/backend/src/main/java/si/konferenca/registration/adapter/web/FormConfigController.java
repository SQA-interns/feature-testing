package si.konferenca.registration.adapter.web;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.FormConfiguration;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.RegistrationType;

/** {@code GET /api/form-config} (public): active options, consents, reCAPTCHA site key. */
@RestController
public class FormConfigController {

  /** What the frontend needs to render the reCAPTCHA widget (AR-07). */
  public record CaptchaWidget(String siteKey, boolean testMode) {}

  record OptionView(String id, String name, String category, List<String> availableTo) {}

  record ConsentView(String id, String text, boolean mandatory) {}

  record FormConfigResponse(
      List<OptionView> options,
      List<ConsentView> consents,
      String captchaSiteKey,
      boolean captchaTestMode) {}

  private final FormConfiguration formConfiguration;
  private final CaptchaWidget captcha;

  public FormConfigController(FormConfiguration formConfiguration, CaptchaWidget captcha) {
    this.formConfiguration = formConfiguration;
    this.captcha = captcha;
  }

  @GetMapping(path = "/api/form-config", produces = ErrorResponses.JSON_UTF8)
  FormConfigResponse formConfig() {
    List<OptionView> options =
        formConfiguration.activeOptions().stream().map(FormConfigController::view).toList();
    List<ConsentView> consents =
        formConfiguration.consents().stream()
            .map(c -> new ConsentView(c.id(), c.text(), c.mandatory()))
            .toList();
    return new FormConfigResponse(options, consents, captcha.siteKey(), captcha.testMode());
  }

  private static OptionView view(ConferenceOption option) {
    List<String> types =
        List.of(RegistrationType.values()).stream()
            .filter(option.availableTo()::contains)
            .map(Enum::name)
            .toList();
    return new OptionView(option.id(), option.name(), option.category().name(), types);
  }
}
