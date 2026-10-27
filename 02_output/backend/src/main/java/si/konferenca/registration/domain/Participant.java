package si.konferenca.registration.domain;

/** Trimmed participant fields; fields of the other registration type are null (BR-01). */
public record Participant(
    String firstName,
    String lastName,
    String email,
    String organization,
    String studyInstitution,
    String studyProgramme,
    String studentId) {}
