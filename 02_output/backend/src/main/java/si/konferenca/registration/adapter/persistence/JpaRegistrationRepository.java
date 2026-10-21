package si.konferenca.registration.adapter.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import si.konferenca.registration.application.RegistrationRepository;
import si.konferenca.registration.domain.Registration;

/** Stores registrations through JPA with bound parameters only (SB-05). */
@Repository
public class JpaRegistrationRepository implements RegistrationRepository {

  @PersistenceContext private EntityManager entityManager;

  @Override
  @Transactional
  public void save(Registration registration) {
    entityManager.persist(RegistrationEntity.from(registration));
    entityManager.flush();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Registration> findAllOrdered() {
    return entityManager
        .createQuery(
            "select r from RegistrationEntity r order by r.registeredAt, r.id",
            RegistrationEntity.class)
        .getResultList()
        .stream()
        .map(RegistrationEntity::toDomain)
        .toList();
  }
}
