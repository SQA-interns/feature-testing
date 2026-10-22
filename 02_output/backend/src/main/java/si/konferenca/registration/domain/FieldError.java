package si.konferenca.registration.domain;

/** A rejected request field with a message for the user (api-contract.md, "Common rules"). */
public record FieldError(String field, String message) {}
