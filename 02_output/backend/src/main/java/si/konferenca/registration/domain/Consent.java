package si.konferenca.registration.domain;

/** A consent a participant may give, with its wording (OQ-02). */
public record Consent(String id, String text, boolean mandatory) {}
