package si.konferenca.registration.domain;

/** One field error of a rejected submission. */
public record FieldError(String field, ErrorCode code) {}
