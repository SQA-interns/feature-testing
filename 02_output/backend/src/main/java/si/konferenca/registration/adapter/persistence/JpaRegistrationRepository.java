package si.konferenca.registration.adapter.persistence;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Repository;
import si.konferenca.registration.application.RegistrationRepository;
import si.konferenca.registration.domain.Registration;

/** PostgreSQL storage of registrations through JPA (BR-07, AC-005-01). */
@Repository
class JpaRegistrationRepository implements RegistrationRepository {

  private final EntityManager entityManager;
  private final RegistrationJpaRepository registrations;

  JpaRegistrationRepository(EntityManager entityManager, RegistrationJpaRepository registrations) {
    this.entityManager = entityManager;
    this.registrations = registrations;
  }

  @Override
  public void save(Registration registration) {
    entityManager.persist(RegistrationEntity.from(registration));
    entityManager.flush();
  }

  @Override
  public List<Registration> findAll() {
    return registrations.findAllByOrderByRegisteredAtAscIdAsc().stream()
        .map(RegistrationEntity::toDomain)
        .toList();
  }
}
