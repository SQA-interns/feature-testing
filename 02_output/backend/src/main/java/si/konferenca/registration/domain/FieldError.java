package si.konferenca.registration.domain;

/** One invalid request field and a message for the participant. */
public record FieldError(String field, String message) {}
