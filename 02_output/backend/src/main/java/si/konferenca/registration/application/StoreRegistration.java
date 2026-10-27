package si.konferenca.registration.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import si.konferenca.registration.application.port.JsonCopyStore;
import si.konferenca.registration.application.port.RegistrationStore;
import si.konferenca.registration.domain.Registration;

/**
 * Stores a registration in the database and as a JSON copy, both or neither (BR-07, AR-05): the
 * copy is written inside the transaction, and removed again if the transaction rolls back.
 */
@Service
public class StoreRegistration {

  private final RegistrationStore registrations;
  private final JsonCopyStore jsonCopies;

  public StoreRegistration(RegistrationStore registrations, JsonCopyStore jsonCopies) {
    this.registrations = registrations;
    this.jsonCopies = jsonCopies;
  }

  /**
   * Stores the registration; returns after the commit.
   *
   * @return the bytes of the JSON copy
   */
  @Transactional
  public byte[] store(Registration registration) {
    registrations.save(registration);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) {
              jsonCopies.delete(registration.id());
            }
          }
        });
    return jsonCopies.write(registration);
  }
}
