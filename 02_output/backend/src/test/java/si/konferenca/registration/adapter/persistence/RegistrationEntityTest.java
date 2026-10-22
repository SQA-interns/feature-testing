package si.konferenca.registration.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

class RegistrationEntityTest {

  private static final Instant AT = Instant.parse("2026-10-20T10:15:30Z");

  @Test
  void mapsEveryFieldBothWaysWithConsentsSortedById() {
    Registration registration =
        new Registration(
            UUID.randomUUID(),
            new Participant(
                RegistrationType.STUDENT, "Luka", "Horvat", "l@u.si", null, "Uni", "CS", "63"),
            List.of("ws", "meal"),
            List.of(new GivenConsent("privacy", AT), new GivenConsent("photos", AT.plusSeconds(1))),
            AT);

    Registration back = RegistrationEntity.from(registration).toDomain();

    assertThat(back.id()).isEqualTo(registration.id());
    assertThat(back.participant()).isEqualTo(registration.participant());
    assertThat(back.options()).containsExactly("ws", "meal");
    assertThat(back.registeredAt()).isEqualTo(AT);
    assertThat(back.consents())
        .containsExactly(
            new GivenConsent("photos", AT.plusSeconds(1)), new GivenConsent("privacy", AT));
  }

  @Test
  void consentRowsCompareByIdAndTime() {
    ConsentEmbeddable a = new ConsentEmbeddable("privacy", AT);

    assertThat(a).isEqualTo(new ConsentEmbeddable("privacy", AT));
    assertThat(a).hasSameHashCodeAs(new ConsentEmbeddable("privacy", AT));
    assertThat(a).isNotEqualTo(new ConsentEmbeddable("privacy", AT.plusSeconds(1)));
    assertThat(a).isNotEqualTo(new ConsentEmbeddable("photos", AT));
    assertThat(a).isNotEqualTo("privacy");
    assertThat(new ConsentEmbeddable()).isEqualTo(new ConsentEmbeddable());
  }

  @Test
  void likeWildcardsAreEscaped() {
    assertThat(JpaRegistrationStore.escapeLike("100%_a\\b")).isEqualTo("100\\%\\_a\\\\b");
    assertThat(JpaRegistrationStore.escapeLike("plain")).isEqualTo("plain");
  }
}
