package si.konferenca.registration.domain;

/** A rejected field: its name as the API writes it, and a message for the participant. */
public record FieldError(String field, String message) {}
