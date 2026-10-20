package si.konferenca.registration.adapter.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.FormConfigService;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;

/** {@code GET /api/form-config}: active options, consents and the site key (AC-003-01). */
@RestController
class FormConfigController {

  private final FormConfigService formConfig;

  FormConfigController(FormConfigService formConfig) {
    this.formConfig = formConfig;
  }

  @GetMapping(value = "/api/form-config", produces = MediaType.APPLICATION_JSON_VALUE)
  FormConfigResponse formConfig() {
    var conference = formConfig.conference();
    return new FormConfigResponse(
        conference.activeOptions().stream().map(OptionResponse::of).toList(),
        conference.consents().stream().map(ConsentResponse::of).toList(),
        formConfig.captchaSiteKey(),
        formConfig.captchaTestMode(),
        conference.conferenceName());
  }

  record FormConfigResponse(
      List<OptionResponse> options,
      List<ConsentResponse> consents,
      String captchaSiteKey,
      boolean captchaTestMode,
      String conferenceName) {}

  record OptionResponse(String id, String name, String category, List<String> availableTo) {
    static OptionResponse of(ConferenceOption option) {
      return new OptionResponse(
          option.id(),
          option.name(),
          option.category().name(),
          option.availableTo().stream().sorted().map(Enum::name).toList());
    }
  }

  record ConsentResponse(String id, String text, boolean mandatory) {
    static ConsentResponse of(ConsentDefinition consent) {
      return new ConsentResponse(consent.id(), consent.text(), consent.mandatory());
    }
  }
}
