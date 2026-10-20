package si.konferenca.registration.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;
import java.util.Objects;

/** Row of {@code registration_consent}: a consent id with the time it was given. */
@Embeddable
class ConsentEmbeddable {

  @Column(name = "consent_id", nullable = false, length = 64)
  private String consentId;

  @Column(name = "given_at", nullable = false)
  private Instant givenAt;

  protected ConsentEmbeddable() {
    // for JPA
  }

  ConsentEmbeddable(String consentId, Instant givenAt) {
    this.consentId = consentId;
    this.givenAt = givenAt;
  }

  String consentId() {
    return consentId;
  }

  Instant givenAt() {
    return givenAt;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ConsentEmbeddable that && Objects.equals(consentId, that.consentId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(consentId);
  }
}
