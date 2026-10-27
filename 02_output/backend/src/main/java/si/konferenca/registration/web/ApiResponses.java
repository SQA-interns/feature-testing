package si.konferenca.registration.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import si.konferenca.registration.application.FormConfiguration;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

/** Response bodies of openapi.yaml. */
final class ApiResponses {

  private ApiResponses() {}

  /** RegistrationAccepted. */
  record Accepted(
      String id,
      String registeredAt,
      String type,
      String firstName,
      String lastName,
      List<Option> options) {

    Accepted {
      options = List.copyOf(options);
    }

    static Accepted of(Registration r) {
      return new Accepted(
          r.id().toString(),
          r.registeredAt().toString(),
          r.type().name(),
          r.participant().firstName(),
          r.participant().lastName(),
          r.options().stream()
              .map(o -> new Option(o.id(), o.name(), o.category().name()))
              .toList());
    }
  }

  /** An option of RegistrationAccepted. */
  record Option(String id, String name, String category) {}

  /** FormConfig. */
  record FormConfig(
      String conferenceName,
      Captcha captcha,
      List<FormOption> options,
      List<FormConsent> consents) {

    FormConfig {
      options = List.copyOf(options);
      consents = List.copyOf(consents);
    }

    static FormConfig of(FormConfiguration c) {
      return new FormConfig(
          c.conferenceName(),
          c.captchaTestMode()
              ? new Captcha("TEST", null)
              : new Captcha("RECAPTCHA", c.captchaSiteKey()),
          c.activeOptions().stream().map(FormOption::of).toList(),
          c.consents().stream().map(FormConsent::of).toList());
    }
  }

  /** Captcha mode and the public site key (AR-07). */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  record Captcha(String mode, String siteKey) {}

  /** FormOption. */
  record FormOption(String id, String name, String category, List<String> availableTo) {

    FormOption {
      availableTo = List.copyOf(availableTo);
    }

    static FormOption of(ConferenceOption o) {
      return new FormOption(
          o.id(),
          o.name(),
          o.category().name(),
          java.util.Arrays.stream(RegistrationType.values())
              .filter(o::isAvailableTo)
              .map(Enum::name)
              .toList());
    }
  }

  /** FormConsent. */
  record FormConsent(String id, String text, boolean mandatory) {

    static FormConsent of(ConsentDefinition c) {
      return new FormConsent(c.id(), c.text(), c.mandatory());
    }
  }

  /** Error. */
  record Error(String message) {}

  /** FieldError. */
  record FieldErrorBody(String field, String code, String message) {}

  /** ValidationError. */
  record ValidationError(String message, List<FieldErrorBody> fieldErrors) {

    ValidationError {
      fieldErrors = List.copyOf(fieldErrors);
    }
  }
}
