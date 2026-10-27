package si.konferenca.registration.adapter.persistence;

import java.util.List;
import org.springframework.stereotype.Repository;
import si.konferenca.registration.application.port.RegistrationStore;
import si.konferenca.registration.domain.Registration;

/** {@link RegistrationStore} on PostgreSQL through JPA (parameterised queries only, SB-05). */
@Repository
public class JpaRegistrationStore implements RegistrationStore {

  private final RegistrationJpaRepository repository;

  public JpaRegistrationStore(RegistrationJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public void save(Registration registration) {
    repository.saveAndFlush(new RegistrationEntity(registration));
  }

  @Override
  public List<Registration> findAllOldestFirst() {
    return repository.findAllByOrderByRegisteredAtAsc().stream()
        .map(RegistrationEntity::toDomain)
        .toList();
  }
}
