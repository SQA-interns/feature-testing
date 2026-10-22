package si.konferenca.registration.domain;

import java.util.List;

/** A registration exactly as submitted, before validation; any field may be null. */
public record RegistrationSubmission(
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
