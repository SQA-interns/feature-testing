package si.konferenca.registration.application;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationInput;
import si.konferenca.registration.domain.RegistrationValidator;
import si.konferenca.registration.domain.ValidationResult;

/**
 * Registers a participant: validate, verify the captcha, store row and copy, then send the emails
 * (BR-06, BR-07, OQ-03).
 */
public class RegisterParticipant {

  public static final String CAPTCHA_FAILED = "The reCAPTCHA check failed. Please try again.";

  private static final Logger LOG = LoggerFactory.getLogger(RegisterParticipant.class);

  private final RegistrationValidator validator;
  private final CaptchaVerifier captcha;
  private final RegistrationStorage storage;
  private final RegistrationNotifier notifier;
  private final Clock clock;

  public RegisterParticipant(
      RegistrationValidator validator,
      CaptchaVerifier captcha,
      RegistrationStorage storage,
      RegistrationNotifier notifier,
      Clock clock) {
    this.validator = validator;
    this.captcha = captcha;
    this.storage = storage;
    this.notifier = notifier;
    this.clock = clock;
  }

  /**
   * Returns the accepted registration.
   *
   * @throws RegistrationRejectedException with the field errors; nothing stored or sent
   * @throws StorageFailedException when the row or the copy could not be written
   */
  public Registration register(RegistrationInput input) {
    ValidationResult result = validator.validate(input);
    List<FieldError> errors = new ArrayList<>(result.errors());
    if (errors.isEmpty() && !captcha.verify(input.captchaToken())) {
      errors.add(new FieldError("captchaToken", CAPTCHA_FAILED));
    }
    if (!errors.isEmpty()) {
      throw new RegistrationRejectedException(errors);
    }
    Registration registration =
        Registration.accept(
            result.registration(),
            UUID.randomUUID(),
            clock.instant().truncatedTo(ChronoUnit.MILLIS));
    UUID id = registration.id();
    byte[] copy;
    try {
      copy = storage.store(registration);
    } catch (RuntimeException e) {
      LOG.error("Storing registration {} failed", id, e);
      throw new StorageFailedException(e);
    }
    try {
      notifier.notifyParticipant(registration);
    } catch (RuntimeException e) {
      LOG.warn("Email for registration {} failed", id);
    }
    try {
      notifier.notifyOrganizers(registration, copy);
    } catch (RuntimeException e) {
      LOG.warn("Organizer email for registration {} failed", id);
    }
    return registration;
  }
}
