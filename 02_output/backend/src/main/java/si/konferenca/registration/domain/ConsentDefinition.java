package si.konferenca.registration.domain;

/** A configured consent (OQ-02). */
public record ConsentDefinition(String id, String text, boolean mandatory) {}
