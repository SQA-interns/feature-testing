package si.konferenca.registration.domain;

/** A configured consent with its wording (OQ-02). */
public record ConsentDefinition(String id, String text, boolean mandatory) {}
