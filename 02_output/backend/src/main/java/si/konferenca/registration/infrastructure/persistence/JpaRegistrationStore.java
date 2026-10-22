package si.konferenca.registration.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Repository;
import si.konferenca.registration.application.RegistrationStore;
import si.konferenca.registration.domain.Registration;

/** Stores registrations in PostgreSQL with parameterised JPA statements only (SB-05). */
@Repository
public class JpaRegistrationStore implements RegistrationStore {

  @PersistenceContext private EntityManager entityManager;

  @Override
  public void save(Registration registration) {
    entityManager.persist(RegistrationEntity.from(registration));
    entityManager.flush();
  }

  @Override
  public List<Registration> findAllOldestFirst() {
    return entityManager
        .createQuery("select r from RegistrationEntity r order by r.seq", RegistrationEntity.class)
        .getResultStream()
        .map(RegistrationEntity::toRegistration)
        .toList();
  }
}
