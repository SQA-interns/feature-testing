package si.konferenca.registration.adapter.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.RegistrationService;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.RegistrationSubmission;
import si.konferenca.registration.domain.ValidationException;
import tools.jackson.databind.JsonNode;

/** {@code POST /api/registrations} (US-001, US-002): 201 with the id, or 400 with field errors. */
@RestController
class RegistrationController {

  static final String NOT_TEXT = "This field must be text.";
  static final String NOT_LIST = "This field must be a list of identifiers.";
  static final String NOT_OBJECT = "The request body must be a JSON object.";

  private final RegistrationService registrations;

  RegistrationController(RegistrationService registrations) {
    this.registrations = registrations;
  }

  @PostMapping(
      value = "/api/registrations",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<Map<String, String>> register(@RequestBody JsonNode body) {
    UUID id = registrations.register(parse(body));
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id.toString()));
  }

  /** Maps the JSON body to a submission; a value of the wrong JSON type is a field error. */
  static RegistrationSubmission parse(JsonNode body) {
    if (body == null || !body.isObject()) {
      throw new ValidationException(List.of(new FieldError("body", NOT_OBJECT)));
    }
    List<FieldError> errors = new ArrayList<>();
    RegistrationSubmission submission =
        new RegistrationSubmission(
            text(body, "type", errors),
            text(body, "firstName", errors),
            text(body, "lastName", errors),
            text(body, "email", errors),
            text(body, "organization", errors),
            text(body, "studyInstitution", errors),
            text(body, "studyProgramme", errors),
            text(body, "studentId", errors),
            list(body, "options", errors),
            list(body, "consents", errors),
            text(body, "captchaToken", errors));
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
    return submission;
  }

  private static String text(JsonNode body, String field, List<FieldError> errors) {
    JsonNode value = body.get(field);
    if (value == null || value.isNull()) {
      return null;
    }
    if (value.isString()) {
      return value.asString();
    }
    errors.add(new FieldError(field, NOT_TEXT));
    return null;
  }

  private static List<String> list(JsonNode body, String field, List<FieldError> errors) {
    JsonNode value = body.get(field);
    if (value == null || value.isNull()) {
      return null;
    }
    if (value.isArray()) {
      List<String> items = new ArrayList<>();
      for (JsonNode item : value) {
        if (!item.isString()) {
          errors.add(new FieldError(field, NOT_LIST));
          return null;
        }
        items.add(item.asString());
      }
      return items;
    }
    errors.add(new FieldError(field, NOT_LIST));
    return null;
  }
}
