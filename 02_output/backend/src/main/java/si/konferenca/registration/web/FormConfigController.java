package si.konferenca.registration.web;

import java.util.List;
import java.util.TreeSet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.FormConfigurationService;
import si.konferenca.registration.application.FormConfigurationService.FormConfiguration;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;

/** GET /api/form-config (api-contract.md, AC-003-01). */
@RestController
public class FormConfigController {

  private final FormConfigurationService service;

  public FormConfigController(FormConfigurationService service) {
    this.service = service;
  }

  /** One active option as the form shows it. */
  public record OptionView(
      String id, String name, OptionCategory category, List<RegistrationType> availableTo) {}

  /** One consent with its wording. */
  public record ConsentView(String id, String text, boolean mandatory) {}

  /** The form configuration response. */
  public record FormConfigResponse(
      List<OptionView> options,
      List<ConsentView> consents,
      String captchaSiteKey,
      boolean captchaTestMode) {}

  @GetMapping("/api/form-config")
  public FormConfigResponse formConfig() {
    FormConfiguration config = service.formConfiguration();
    return new FormConfigResponse(
        config.options().stream()
            .map(
                o ->
                    new OptionView(
                        o.id(),
                        o.name(),
                        o.category(),
                        List.copyOf(new TreeSet<>(o.availableTo()))))
            .toList(),
        config.consents().stream()
            .map(c -> new ConsentView(c.id(), c.text(), c.mandatory()))
            .toList(),
        config.captcha().siteKey(),
        config.captcha().testMode());
  }
}
