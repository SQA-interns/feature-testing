package si.konferenca.registration.application;

import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import si.konferenca.registration.domain.Registration;

/**
 * Stores the database row and the JSON copy in one transaction (AR-05): a failed copy rolls the row
 * back, and a rollback after the copy was written removes the copy.
 */
public class RegistrationStorage {

  private final RegistrationRepository repository;
  private final JsonCopyStore jsonCopies;

  public RegistrationStorage(RegistrationRepository repository, JsonCopyStore jsonCopies) {
    this.repository = repository;
    this.jsonCopies = jsonCopies;
  }

  /** Returns the bytes of the JSON copy. */
  @Transactional
  public byte[] store(Registration registration) {
    repository.save(registration);
    byte[] copy = jsonCopies.save(registration);
    TransactionSynchronizationManager.registerSynchronization(
        new DeleteCopyOnRollback(jsonCopies, registration.id()));
    return copy;
  }

  /** Removes the JSON copy when the transaction does not commit. */
  private record DeleteCopyOnRollback(JsonCopyStore jsonCopies, UUID registrationId)
      implements TransactionSynchronization {

    @Override
    public void afterCompletion(int status) {
      if (status != STATUS_COMMITTED) {
        jsonCopies.discard(registrationId);
      }
    }
  }
}
