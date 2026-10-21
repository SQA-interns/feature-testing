package si.konferenca.registration.web;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.RegisterParticipant;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationInput;

/** {@code POST /api/registrations} (public, reCAPTCHA protected). */
@RestController
public class RegistrationController {

  /** The request body of {@code openapi.json} RegistrationRequest; any field may be missing. */
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
      String captchaToken) {

    public RegistrationRequest {
      options = options == null ? null : Collections.unmodifiableList(new ArrayList<>(options));
      consents = consents == null ? null : Collections.unmodifiableList(new ArrayList<>(consents));
    }

    RegistrationInput toInput() {
      return new RegistrationInput(
          type,
          firstName,
          lastName,
          email,
          organization,
          studyInstitution,
          studyProgramme,
          studentId,
          options,
          consents,
          captchaToken);
    }
  }

  /** The 201 response body. */
  public record CreatedResponse(String id) {}

  private final RegisterParticipant registerParticipant;

  public RegistrationController(RegisterParticipant registerParticipant) {
    this.registerParticipant = registerParticipant;
  }

  @PostMapping(path = "/api/registrations", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CreatedResponse> register(@RequestBody RegistrationRequest request) {
    Registration registration = registerParticipant.register(request.toInput());
    String id = registration.id().toString();
    return ResponseEntity.created(URI.create("/api/registrations/" + id))
        .body(new CreatedResponse(id));
  }
}
