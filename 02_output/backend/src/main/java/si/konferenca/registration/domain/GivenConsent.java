package si.konferenca.registration.domain;

import java.time.Instant;

/** A consent the participant gave, with the time it was given (SB-14). */
public record GivenConsent(String id, Instant givenAt) {}
