package si.konferenca.registration.application;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionOperations;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationSubmission;
import si.konferenca.registration.domain.RegistrationValidator;
import si.konferenca.registration.domain.ValidRegistration;
import si.konferenca.registration.domain.ValidationException;

/**
 * Use case: register a participant (US-001, US-002, US-005, US-006, US-007). Storage comes before
 * any notification: the database row and the JSON copy are written in one transaction (AR-05);
 * emails follow the commit and never undo an accepted registration (OQ-03).
 */
public class RegistrationService {

  static final String CAPTCHA_REJECTED = "The anti-robot check failed. Please try again.";

  private static final Logger LOG = LoggerFactory.getLogger(RegistrationService.class);

  private final RegistrationValidator validator;
  private final CaptchaVerifier captcha;
  private final RegistrationRepository repository;
  private final JsonCopyStore jsonCopies;
  private final RegistrationNotifier notifier;
  private final TransactionOperations transactions;
  private final Clock clock;

  public RegistrationService(
      RegistrationValidator validator,
      CaptchaVerifier captcha,
      RegistrationRepository repository,
      JsonCopyStore jsonCopies,
      RegistrationNotifier notifier,
      TransactionOperations transactions,
      Clock clock) {
    this.validator = validator;
    this.captcha = captcha;
    this.repository = repository;
    this.jsonCopies = jsonCopies;
    this.notifier = notifier;
    this.transactions = transactions;
    this.clock = clock;
  }

  /**
   * @return the id of the accepted registration
   * @throws ValidationException when the submission is rejected (nothing stored, no email)
   * @throws StorageException when the row or the JSON copy cannot be written (nothing remains)
   */
  public UUID register(RegistrationSubmission submission) {
    ValidRegistration valid = validator.validate(submission);
    if (!captcha.verify(submission.captchaToken())) {
      throw new ValidationException(List.of(new FieldError("captchaToken", CAPTCHA_REJECTED)));
    }
    Registration registration =
        valid.accept(UUID.randomUUID(), clock.instant().truncatedTo(ChronoUnit.MICROS));
    byte[] jsonCopy = store(registration);
    notify(registration, jsonCopy);
    return registration.id();
  }

  private byte[] store(Registration registration) {
    try {
      return transactions.execute(
          status -> {
            repository.save(registration);
            return jsonCopies.write(registration);
          });
    } catch (RuntimeException e) {
      jsonCopies.delete(registration.id());
      LOG.error(
          "registration {} could not be stored ({})",
          registration.id(),
          e.getClass().getSimpleName());
      throw new StorageException(e);
    }
  }

  private void notify(Registration registration, byte[] jsonCopy) {
    try {
      notifier.sendParticipantConfirmation(registration);
    } catch (RuntimeException e) {
      LOG.warn(
          "confirmation mail failed for registration {} ({})",
          registration.id(),
          e.getClass().getSimpleName());
    }
    try {
      notifier.sendOrganizerNotification(registration, jsonCopy);
    } catch (RuntimeException e) {
      LOG.warn(
          "organizer mail failed for registration {} ({})",
          registration.id(),
          e.getClass().getSimpleName());
    }
  }
}
