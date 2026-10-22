package si.konferenca.registration.adapter.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

/** A row of {@code registration} with its options and consents. */
@Entity
@Table(name = "registration")
public class RegistrationEntity {

  @Id private UUID id;

  @Column(nullable = false, length = 16)
  private String type;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Column(nullable = false, length = 254)
  private String email;

  @Column(length = 200)
  private String organization;

  @Column(name = "study_institution", length = 200)
  private String studyInstitution;

  @Column(name = "study_programme", length = 200)
  private String studyProgramme;

  @Column(name = "student_id", length = 50)
  private String studentId;

  @Column(name = "registered_at", nullable = false)
  private Instant registeredAt;

  @ElementCollection
  @CollectionTable(
      name = "registration_option",
      joinColumns = @JoinColumn(name = "registration_id"))
  @Column(name = "option_id", nullable = false, length = 64)
  @OrderBy
  private List<String> optionIds = new ArrayList<>();

  @ElementCollection
  @CollectionTable(
      name = "registration_consent",
      joinColumns = @JoinColumn(name = "registration_id"))
  private List<ConsentEmbeddable> consents = new ArrayList<>();

  protected RegistrationEntity() {}

  static RegistrationEntity from(Registration registration) {
    RegistrationEntity entity = new RegistrationEntity();
    Participant p = registration.participant();
    entity.id = registration.id();
    entity.type = p.type().name();
    entity.firstName = p.firstName();
    entity.lastName = p.lastName();
    entity.email = p.email();
    entity.organization = p.organization();
    entity.studyInstitution = p.studyInstitution();
    entity.studyProgramme = p.studyProgramme();
    entity.studentId = p.studentId();
    entity.registeredAt = registration.registeredAt();
    entity.optionIds = new ArrayList<>(registration.options());
    entity.consents =
        new ArrayList<>(
            registration.consents().stream()
                .map(c -> new ConsentEmbeddable(c.id(), c.givenAt()))
                .toList());
    return entity;
  }

  Registration toDomain() {
    Participant participant =
        new Participant(
            RegistrationType.valueOf(type),
            firstName,
            lastName,
            email,
            organization,
            studyInstitution,
            studyProgramme,
            studentId);
    List<GivenConsent> given =
        consents.stream()
            .sorted(Comparator.comparing(ConsentEmbeddable::consentId))
            .map(c -> new GivenConsent(c.consentId(), c.givenAt()))
            .toList();
    return new Registration(id, participant, List.copyOf(optionIds), given, registeredAt);
  }
}
