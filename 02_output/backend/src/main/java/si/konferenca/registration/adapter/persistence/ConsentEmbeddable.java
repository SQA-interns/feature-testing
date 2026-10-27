package si.konferenca.registration.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;
import si.konferenca.registration.domain.GivenConsent;

/** A row of registration_consent. */
@Embeddable
public class ConsentEmbeddable {

  @Column(name = "consent_id", nullable = false, length = 64)
  private String consentId;

  @Column(name = "consent_text", nullable = false, length = 1000)
  private String consentText;

  @Column(name = "given_at", nullable = false)
  private Instant givenAt;

  protected ConsentEmbeddable() {
    // for JPA
  }

  ConsentEmbeddable(GivenConsent consent) {
    this.consentId = consent.id();
    this.consentText = consent.text();
    this.givenAt = consent.givenAt();
  }

  GivenConsent toDomain() {
    return new GivenConsent(consentId, consentText, givenAt);
  }
}
