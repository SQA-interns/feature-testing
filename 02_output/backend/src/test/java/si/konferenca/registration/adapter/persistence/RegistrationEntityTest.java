package si.konferenca.registration.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

class RegistrationEntityTest {

  static final Instant AT = Instant.parse("2026-10-20T10:00:00.123456Z");

  @Test
  void mapsEveryFieldBothWaysWithSortedOptionsAndConsents() {
    Registration registration =
        new Registration(
            UUID.randomUUID(),
            RegistrationType.STUDENT,
            "Špela",
            "Ž",
            "s@x.org",
            null,
            "Uni",
            "CS",
            "123",
            List.of("ws", "ev"),
            List.of(new GivenConsent("privacy", AT), new GivenConsent("photos", AT.plusSeconds(1))),
            AT);

    Registration back = RegistrationEntity.from(registration).toDomain();

    assertThat(back.id()).isEqualTo(registration.id());
    assertThat(back.type()).isEqualTo(RegistrationType.STUDENT);
    assertThat(back.firstName()).isEqualTo("Špela");
    assertThat(back.lastName()).isEqualTo("Ž");
    assertThat(back.email()).isEqualTo("s@x.org");
    assertThat(back.organization()).isNull();
    assertThat(back.studyInstitution()).isEqualTo("Uni");
    assertThat(back.studyProgramme()).isEqualTo("CS");
    assertThat(back.studentId()).isEqualTo("123");
    assertThat(back.registeredAt()).isEqualTo(AT);
    assertThat(back.options()).containsExactly("ev", "ws");
    assertThat(back.consents())
        .containsExactly(
            new GivenConsent("photos", AT.plusSeconds(1)), new GivenConsent("privacy", AT));
  }

  @Test
  void consentsAreEqualByIdOnly() {
    ConsentEmbeddable a = new ConsentEmbeddable("privacy", AT);
    ConsentEmbeddable b = new ConsentEmbeddable("privacy", AT.plusSeconds(5));
    ConsentEmbeddable c = new ConsentEmbeddable("photos", AT);

    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(c).isNotEqualTo("privacy");
    assertThat(a.hashCode()).isEqualTo("privacy".hashCode());
    assertThat(a.consentId()).isEqualTo("privacy");
    assertThat(a.givenAt()).isEqualTo(AT);
    assertThat(new ConsentEmbeddable().hashCode()).isZero();
  }
}
