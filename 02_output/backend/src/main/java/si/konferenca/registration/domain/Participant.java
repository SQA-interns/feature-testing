package si.konferenca.registration.domain;

/**
 * The fixed participant fields of BR-01, trimmed; the fields of the other type are null. External
 * participants have {@code organization}; students the three study fields.
 */
public record Participant(
    RegistrationType type,
    String firstName,
    String lastName,
    String email,
    String organization,
    String studyInstitution,
    String studyProgramme,
    String studentId) {}
