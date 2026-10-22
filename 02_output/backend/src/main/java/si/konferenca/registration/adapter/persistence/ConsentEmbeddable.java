package si.konferenca.registration.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;
import java.util.Objects;

/** One row of {@code registration_consent}. */
@Embeddable
public class ConsentEmbeddable {

  @Column(name = "consent_id", nullable = false, length = 64)
  private String consentId;

  @Column(name = "given_at", nullable = false)
  private Instant givenAt;

  protected ConsentEmbeddable() {}

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
    return other instanceof ConsentEmbeddable that
        && Objects.equals(consentId, that.consentId)
        && Objects.equals(givenAt, that.givenAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(consentId, givenAt);
  }
}
