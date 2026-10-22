package si.konferenca.registration.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.SearchText;

/** JPA mapping of the registration tables of V1__registration.sql. */
@Entity
@Table(name = "registration")
public class RegistrationEntity {

  @Id private UUID id;

  @Column(insertable = false, updatable = false)
  private Long seq;

  private String type;

  @Column(name = "first_name")
  private String firstName;

  @Column(name = "last_name")
  private String lastName;

  private String email;
  private String organization;

  @Column(name = "study_institution")
  private String studyInstitution;

  @Column(name = "study_programme")
  private String studyProgramme;

  @Column(name = "student_id")
  private String studentId;

  @Column(name = "search_text")
  private String searchText;

  @Column(name = "registered_at")
  private Instant registeredAt;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "registration_option",
      joinColumns = @JoinColumn(name = "registration_id"))
  @OrderColumn(name = "position")
  @Column(name = "option_id")
  private List<String> options = new ArrayList<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "registration_consent",
      joinColumns = @JoinColumn(name = "registration_id"))
  private List<ConsentValue> consents = new ArrayList<>();

  /** One given consent row. */
  @Embeddable
  public static class ConsentValue {

    @Column(name = "consent_id")
    private String consentId;

    @Column(name = "given_at")
    private Instant givenAt;

    protected ConsentValue() {}

    ConsentValue(String consentId, Instant givenAt) {
      this.consentId = consentId;
      this.givenAt = givenAt;
    }
  }

  protected RegistrationEntity() {}

  static RegistrationEntity from(Registration r) {
    RegistrationEntity e = new RegistrationEntity();
    e.id = r.id();
    e.type = r.type().name();
    e.firstName = r.firstName();
    e.lastName = r.lastName();
    e.email = r.email();
    e.organization = r.organization();
    e.studyInstitution = r.studyInstitution();
    e.studyProgramme = r.studyProgramme();
    e.studentId = r.studentId();
    e.searchText = SearchText.of(r);
    e.registeredAt = r.registeredAt();
    e.options = new ArrayList<>(r.options());
    e.consents =
        new ArrayList<>(
            r.consents().stream().map(c -> new ConsentValue(c.consentId(), c.givenAt())).toList());
    return e;
  }

  Registration toRegistration() {
    return new Registration(
        id,
        RegistrationType.valueOf(type),
        firstName,
        lastName,
        email,
        organization,
        studyInstitution,
        studyProgramme,
        studentId,
        options,
        consents.stream().map(c -> new Registration.GivenConsent(c.consentId, c.givenAt)).toList(),
        registeredAt);
  }
}
