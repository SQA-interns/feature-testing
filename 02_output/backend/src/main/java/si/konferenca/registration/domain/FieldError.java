package si.konferenca.registration.domain;

/** One rejected request field with a message for the participant. */
public record FieldError(String field, String message) {}
