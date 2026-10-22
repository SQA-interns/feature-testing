package si.konferenca.registration.infrastructure.jsoncopy;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import si.konferenca.registration.domain.Registration;
import tools.jackson.databind.json.JsonMapper;

/** The raw JSON copy of a registration (02_contracts/registration-copy.schema.json). */
final class RegistrationJson {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private RegistrationJson() {}

  @JsonPropertyOrder({
    "id",
    "type",
    "firstName",
    "lastName",
    "email",
    "organization",
    "studyInstitution",
    "studyProgramme",
    "studentId",
    "options",
    "consents",
    "registeredAt"
  })
  record Copy(
      String id,
      String type,
      String firstName,
      String lastName,
      String email,
      String organization,
      String studyInstitution,
      String studyProgramme,
      String studentId,
      List<String> options,
      List<Consent> consents,
      String registeredAt) {}

  @JsonPropertyOrder({"id", "givenAt"})
  record Consent(String id, String givenAt) {}

  /** UTF-8 bytes of the copy, without byte order mark. */
  static byte[] toBytes(Registration r) {
    Copy copy =
        new Copy(
            r.id().toString(),
            r.type().name(),
            r.firstName(),
            r.lastName(),
            r.email(),
            r.organization(),
            r.studyInstitution(),
            r.studyProgramme(),
            r.studentId(),
            r.options(),
            r.consents().stream()
                .map(
                    c ->
                        new Consent(
                            c.consentId(), DateTimeFormatter.ISO_INSTANT.format(c.givenAt())))
                .toList(),
            DateTimeFormatter.ISO_INSTANT.format(r.registeredAt()));
    return JSON.writeValueAsString(copy).getBytes(StandardCharsets.UTF_8);
  }
}
