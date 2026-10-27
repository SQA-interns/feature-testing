package si.konferenca.registration.domain;

import java.time.Instant;

/** A consent as given with a registration (SB-14). */
public record GivenConsent(String id, String text, Instant givenAt) {}
