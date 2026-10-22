package si.konferenca.registration.application;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import si.konferenca.registration.domain.ConferenceConfiguration;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationSubmission;
import si.konferenca.registration.domain.RegistrationValidator;

/**
 * Accepts a registration: validation, captcha, then the database row and the JSON copy in one
 * transaction (BR-07, AR-05). Success is reported only after both are written.
 */
@Service
public class RegistrationService {

  private final RegistrationValidator validator;
  private final CaptchaVerifier captcha;
  private final RegistrationStore store;
  private final JsonCopyStore copies;
  private final TransactionTemplate transaction;
  private final Clock clock;

  public RegistrationService(
      ConferenceConfiguration configuration,
      CaptchaVerifier captcha,
      RegistrationStore store,
      JsonCopyStore copies,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    this.validator = new RegistrationValidator(configuration);
    this.captcha = captcha;
    this.store = store;
    this.copies = copies;
    this.transaction = new TransactionTemplate(transactionManager);
    this.clock = clock;
  }

  /** The id of the accepted registration, or the field errors of a rejected one. */
  public record Outcome(UUID id, List<FieldError> errors) {

    public boolean accepted() {
      return id != null;
    }
  }

  /**
   * Registers the submission.
   *
   * @throws CaptchaUnavailableException if the captcha cannot be verified
   * @throws StorageException if the registration cannot be stored
   */
  public Outcome register(RegistrationSubmission submission) {
    RegistrationValidator.Result result = validator.validate(submission);
    if (!result.valid()) {
      return new Outcome(null, result.errors());
    }
    if (!captcha.verify(submission.captchaToken())) {
      return new Outcome(
          null, List.of(new FieldError("captchaToken", "The captcha was not accepted.")));
    }
    Registration registration =
        Registration.accept(UUID.randomUUID(), result.data(), clock.instant());
    store(registration);
    return new Outcome(registration.id(), List.of());
  }

  private byte[] store(Registration registration) {
    byte[][] written = new byte[1][];
    try {
      transaction.executeWithoutResult(
          status -> {
            store.save(registration);
            written[0] = copies.write(registration);
          });
    } catch (RuntimeException e) {
      if (written[0] != null) {
        copies.delete(registration.id());
      }
      if (e instanceof StorageException storageException) {
        throw storageException;
      }
      throw new StorageException("Registration " + registration.id() + " not stored", e);
    }
    return written[0];
  }
}
