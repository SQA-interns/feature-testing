package si.konferenca.registration.web;

import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import si.konferenca.registration.application.GetFormConfiguration;
import si.konferenca.registration.application.RegisterParticipant;
import si.konferenca.registration.domain.Registration;

/** Public endpoints: the form configuration and registration (openapi.yaml). */
@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class RegistrationController {

  private final GetFormConfiguration formConfiguration;
  private final RegisterParticipant registerParticipant;

  public RegistrationController(
      GetFormConfiguration formConfiguration, RegisterParticipant registerParticipant) {
    this.formConfiguration = formConfiguration;
    this.registerParticipant = registerParticipant;
  }

  @GetMapping("/form-config")
  ApiResponses.FormConfig formConfig() {
    return ApiResponses.FormConfig.of(formConfiguration.get());
  }

  @PostMapping(path = "/registrations", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<ApiResponses.Accepted> register(@RequestBody RegistrationRequest request) {
    Registration registration = registerParticipant.register(request.toSubmission());
    return ResponseEntity.created(URI.create("/api/registrations/" + registration.id()))
        .body(ApiResponses.Accepted.of(registration));
  }
}
