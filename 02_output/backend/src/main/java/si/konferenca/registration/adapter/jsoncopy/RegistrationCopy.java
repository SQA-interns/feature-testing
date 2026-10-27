package si.konferenca.registration.adapter.jsoncopy;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import si.konferenca.registration.domain.Registration;

/** The JSON copy document (registration-copy.schema.json, schemaVersion 1). */
record RegistrationCopy(
    int schemaVersion,
    String id,
    String type,
    String registeredAt,
    Participant participant,
    List<Option> options,
    List<Consent> consents) {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  record Participant(
      String firstName,
      String lastName,
      String email,
      String organization,
      String studyInstitution,
      String studyProgramme,
      String studentId) {}

  record Option(String id, String name, String category) {}

  record Consent(String id, String text, String givenAt) {}

  RegistrationCopy {
    options = List.copyOf(options);
    consents = List.copyOf(consents);
  }

  static RegistrationCopy of(Registration r) {
    var p = r.participant();
    return new RegistrationCopy(
        1,
        r.id().toString(),
        r.type().name(),
        r.registeredAt().toString(),
        new Participant(
            p.firstName(),
            p.lastName(),
            p.email(),
            p.organization(),
            p.studyInstitution(),
            p.studyProgramme(),
            p.studentId()),
        r.options().stream().map(o -> new Option(o.id(), o.name(), o.category().name())).toList(),
        r.consents().stream()
            .map(c -> new Consent(c.id(), c.text(), c.givenAt().toString()))
            .toList());
  }
}
