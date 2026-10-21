package si.konferenca.registration.adapter.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

/** The {@code registration} table with its options and consents. */
@Entity
@Table(name = "registration")
public class RegistrationEntity {

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

  @ElementCollection
  @CollectionTable(
      name = "registration_option",
      joinColumns = @JoinColumn(name = "registration_id"))
  @OrderColumn(name = "position")
  @Column(name = "option_id", nullable = false, length = 64)
  private List<String> options = new ArrayList<>();

  @ElementCollection
  @CollectionTable(
      name = "registration_consent",
      joinColumns = @JoinColumn(name = "registration_id"))
  private List<ConsentEmbeddable> consents = new ArrayList<>();

  protected RegistrationEntity() {
    // for JPA
  }

  static RegistrationEntity from(Registration r) {
    RegistrationEntity e = new RegistrationEntity();
    e.id = r.id();
    e.type = r.type();
    e.firstName = r.firstName();
    e.lastName = r.lastName();
    e.email = r.email();
    e.organization = r.organization();
    e.studyInstitution = r.studyInstitution();
    e.studyProgramme = r.studyProgramme();
    e.studentId = r.studentId();
    e.registeredAt = r.registeredAt();
    e.options = new ArrayList<>(r.options());
    e.consents =
        new ArrayList<>(
            r.consents().stream().map(c -> new ConsentEmbeddable(c.id(), c.givenAt())).toList());
    return e;
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
        List.copyOf(options),
        consents.stream().map(c -> new GivenConsent(c.consentId(), c.givenAt())).toList(),
        registeredAt);
  }
}
