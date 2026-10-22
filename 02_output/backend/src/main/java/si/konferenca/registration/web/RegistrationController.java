package si.konferenca.registration.web;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.RegistrationService;
import si.konferenca.registration.domain.RegistrationSubmission;

/** POST /api/registrations (api-contract.md, US-001, US-002). */
@RestController
public class RegistrationController {

  private final RegistrationService registrations;

  public RegistrationController(RegistrationService registrations) {
    this.registrations = registrations;
  }

  /** The registration request body; every field may be missing. */
  public record RegistrationRequest(
      String type,
      String firstName,
      String lastName,
      String email,
      String organization,
      String studyInstitution,
      String studyProgramme,
      String studentId,
      List<String> options,
      List<String> consents,
      String captchaToken) {}

  /** The answer for an accepted registration. */
  public record Created(String id) {}

  @PostMapping(path = "/api/registrations", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> register(@RequestBody RegistrationRequest request) {
    RegistrationService.Outcome outcome =
        registrations.register(
            new RegistrationSubmission(
                request.type(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.organization(),
                request.studyInstitution(),
                request.studyProgramme(),
                request.studentId(),
                request.options(),
                request.consents(),
                request.captchaToken()));
    if (outcome.accepted()) {
      return ResponseEntity.status(HttpStatus.CREATED).body(new Created(outcome.id().toString()));
    }
    return ResponseEntity.badRequest().body(new ErrorResponses.FieldErrors(outcome.errors()));
  }
}
