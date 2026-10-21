package si.konferenca.registration.domain;

/** A consent with its wording; mandatory consents must be given (BR-05, OQ-02). */
public record ConsentDefinition(String id, String text, boolean mandatory) {}
