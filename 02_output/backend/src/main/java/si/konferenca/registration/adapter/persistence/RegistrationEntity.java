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
import org.springframework.data.domain.Persistable;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

/** A row of registration with its options and consents (database-schema.sql). */
@Entity
@Table(name = "registration")
public class RegistrationEntity implements Persistable<UUID> {

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

  @Column(name = "student_id", length = 50)
  private String studentId;

  @Column(name = "registered_at", nullable = false)
  private Instant registeredAt;

  @ElementCollection
  @CollectionTable(
      name = "registration_option",
      joinColumns = @JoinColumn(name = "registration_id"))
  @OrderColumn(name = "position")
  private List<OptionEmbeddable> options = new ArrayList<>();

  @ElementCollection
  @CollectionTable(
      name = "registration_consent",
      joinColumns = @JoinColumn(name = "registration_id"))
  private List<ConsentEmbeddable> consents = new ArrayList<>();

  private transient boolean isNew;

  protected RegistrationEntity() {
    // for JPA
  }

  RegistrationEntity(Registration registration) {
    Participant p = registration.participant();
    this.id = registration.id();
    this.type = registration.type();
    this.firstName = p.firstName();
    this.lastName = p.lastName();
    this.email = p.email();
    this.organization = p.organization();
    this.studyInstitution = p.studyInstitution();
    this.studyProgramme = p.studyProgramme();
    this.studentId = p.studentId();
    this.registeredAt = registration.registeredAt();
    registration.options().forEach(o -> options.add(new OptionEmbeddable(o)));
    registration.consents().forEach(c -> consents.add(new ConsentEmbeddable(c)));
    this.isNew = true;
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  Registration toDomain() {
    return new Registration(
        id,
        type,
        registeredAt,
        new Participant(
            firstName, lastName, email, organization, studyInstitution, studyProgramme, studentId),
        options.stream().map(OptionEmbeddable::toDomain).toList(),
        consents.stream().map(ConsentEmbeddable::toDomain).toList());
  }
}
