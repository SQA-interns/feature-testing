package si.konferenca.registration.adapter.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Spring Data access to {@link RegistrationEntity}. */
public interface RegistrationJpaRepository
    extends JpaRepository<RegistrationEntity, UUID>, JpaSpecificationExecutor<RegistrationEntity> {}
