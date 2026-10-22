package si.konferenca.registration.adapter.web;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.RegisterParticipant;
import si.konferenca.registration.application.RegistrationOutcome;
import si.konferenca.registration.domain.RegistrationForm;

/** {@code POST /api/registrations} (public): 201 with the id, or 400 with field errors. */
@RestController
public class RegistrationController {

  record RegistrationRequest(
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

  record Created(String id) {}

  private final RegisterParticipant registerParticipant;

  public RegistrationController(RegisterParticipant registerParticipant) {
    this.registerParticipant = registerParticipant;
  }

  @PostMapping(
      path = "/api/registrations",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = ErrorResponses.JSON_UTF8)
  ResponseEntity<Object> register(@RequestBody RegistrationRequest request) {
    RegistrationOutcome outcome =
        registerParticipant.register(
            new RegistrationForm(
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
    return switch (outcome) {
      case RegistrationOutcome.Accepted accepted ->
          ResponseEntity.status(HttpStatus.CREATED).body(new Created(accepted.id().toString()));
      case RegistrationOutcome.Rejected rejected ->
          ResponseEntity.badRequest()
              .body(
                  new ErrorBody(
                      rejected.errors().stream()
                          .map(e -> new ErrorBody.Item(e.field(), e.message()))
                          .toList()));
    };
  }
}
