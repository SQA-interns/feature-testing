package si.konferenca.registration.adapter.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data access to registration rows. */
public interface RegistrationJpaRepository extends JpaRepository<RegistrationEntity, UUID> {

  List<RegistrationEntity> findAllByOrderByRegisteredAtAsc();
}
