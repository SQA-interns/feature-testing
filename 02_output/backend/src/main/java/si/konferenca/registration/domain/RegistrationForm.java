package si.konferenca.registration.domain;

import java.util.List;

/** A submitted registration exactly as received; any field may be missing (null). */
public record RegistrationForm(
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
