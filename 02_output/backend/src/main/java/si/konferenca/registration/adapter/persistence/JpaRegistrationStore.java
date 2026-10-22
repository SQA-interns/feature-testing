package si.konferenca.registration.adapter.persistence;

import jakarta.persistence.EntityManager;
import si.konferenca.registration.application.RegistrationStore;
import si.konferenca.registration.domain.Registration;

/** {@link RegistrationStore} on PostgreSQL through JPA. */
public class JpaRegistrationStore implements RegistrationStore {

  private final EntityManager entityManager;

  public JpaRegistrationStore(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public void add(Registration registration) {
    entityManager.persist(RegistrationEntity.from(registration));
    entityManager.flush();
  }
}
