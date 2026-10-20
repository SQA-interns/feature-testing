package si.konferenca.registration.adapter.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

/** Table {@code registration} with its options and consents (database-schema.sql). */
@Entity
@Table(name = "registration")
class RegistrationEntity {

  @Id private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private RegistrationType type;

  @Column(name = "first_name", nullable = false, length = 200)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 200)
  private String lastName;

  @Column(nullable = false, length = 254)
  private String email;

  @Column(length = 200)
  private String organization;

  @Column(name = "study_institution", length = 200)
  private String studyInstitution;

  @Column(name = "study_programme", length = 200)
  private String studyProgramme;

  @Column(name = "student_id", length = 200)
  private String studentId;

  @Column(name = "registered_at", nullable = false)
  private Instant registeredAt;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "registration_option",
      joinColumns = @JoinColumn(name = "registration_id"))
  @Column(name = "option_id", nullable = false, length = 64)
  private Set<String> options = new LinkedHashSet<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "registration_consent",
      joinColumns = @JoinColumn(name = "registration_id"))
  private Set<ConsentEmbeddable> consents = new LinkedHashSet<>();

  protected RegistrationEntity() {
    // for JPA
  }

  static RegistrationEntity from(Registration registration) {
    RegistrationEntity entity = new RegistrationEntity();
    entity.id = registration.id();
    entity.type = registration.type();
    entity.firstName = registration.firstName();
    entity.lastName = registration.lastName();
    entity.email = registration.email();
    entity.organization = registration.organization();
    entity.studyInstitution = registration.studyInstitution();
    entity.studyProgramme = registration.studyProgramme();
    entity.studentId = registration.studentId();
    entity.registeredAt = registration.registeredAt();
    entity.options = new LinkedHashSet<>(registration.options());
    entity.consents = new LinkedHashSet<>();
    for (GivenConsent consent : registration.consents()) {
      entity.consents.add(new ConsentEmbeddable(consent.id(), consent.givenAt()));
    }
    return entity;
  }

  Registration toDomain() {
    return new Registration(
        id,
        type,
        firstName,
        lastName,
        email,
        organization,
        studyInstitution,
        studyProgramme,
        studentId,
        options.stream().sorted().toList(),
        consents.stream()
            .sorted(Comparator.comparing(ConsentEmbeddable::consentId))
            .map(c -> new GivenConsent(c.consentId(), c.givenAt()))
            .toList(),
        registeredAt);
  }
}
