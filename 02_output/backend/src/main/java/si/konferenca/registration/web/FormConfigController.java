package si.konferenca.registration.web;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.FormConfiguration;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.RegistrationType;

/** {@code GET /api/form-config} (public). */
@RestController
public class FormConfigController {

  /** One active option. */
  public record OptionResponse(String id, String name, String category, List<String> availableTo) {

    public OptionResponse {
      availableTo = List.copyOf(availableTo);
    }
  }

  /** One consent with its wording. */
  public record ConsentResponse(String id, String text, boolean mandatory) {}

  /** The response body of {@code openapi.json} FormConfig. */
  public record FormConfigResponse(
      List<OptionResponse> options,
      List<ConsentResponse> consents,
      String captchaSiteKey,
      boolean captchaTestMode) {

    public FormConfigResponse {
      options = List.copyOf(options);
      consents = List.copyOf(consents);
    }
  }

  private final FormConfiguration formConfiguration;

  public FormConfigController(FormConfiguration formConfiguration) {
    this.formConfiguration = formConfiguration;
  }

  @GetMapping("/api/form-config")
  public FormConfigResponse formConfig() {
    FormConfiguration.View view = formConfiguration.current();
    return new FormConfigResponse(
        view.options().stream().map(FormConfigController::option).toList(),
        view.consents().stream()
            .map(c -> new ConsentResponse(c.id(), c.text(), c.mandatory()))
            .toList(),
        view.captchaSiteKey(),
        view.captchaTestMode());
  }

  private static OptionResponse option(ConferenceOption option) {
    List<String> types =
        List.of(RegistrationType.values()).stream()
            .filter(option::isAvailableTo)
            .map(Enum::name)
            .toList();
    return new OptionResponse(option.id(), option.name(), option.category().name(), types);
  }
}
