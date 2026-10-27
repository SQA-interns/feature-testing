package si.konferenca.registration.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import si.konferenca.registration.application.port.CaptchaVerifier;
import si.konferenca.registration.application.port.RegistrationNotifier;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.ErrorCode;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationRejectedException;
import si.konferenca.registration.domain.RegistrationValidator;
import si.konferenca.registration.domain.Submission;
import si.konferenca.registration.domain.ValidatedSubmission;

/**
 * Use case: register a participant (specification S4). Validates, then verifies the captcha, then
 * stores; emails go out only after the commit and never change the outcome (OQ-03).
 */
@Service
public class RegisterParticipant {

  private static final Logger LOG = LoggerFactory.getLogger(RegisterParticipant.class);

  private final RegistrationValidator validator;
  private final CaptchaVerifier captcha;
  private final StoreRegistration storage;
  private final RegistrationNotifier notifier;
  private final Clock clock;

  public RegisterParticipant(
      ConferenceCatalog catalog,
      CaptchaVerifier captcha,
      StoreRegistration storage,
      RegistrationNotifier notifier,
      Clock clock) {
    this.validator = new RegistrationValidator(catalog);
    this.captcha = captcha;
    this.storage = storage;
    this.notifier = notifier;
    this.clock = clock;
  }

  /**
   * Registers the submission.
   *
   * @throws RegistrationRejectedException when a rule is broken or the captcha is rejected
   */
  public Registration register(Submission submission) {
    ValidatedSubmission valid = validator.validate(submission);
    if (!valid.isValid()) {
      throw new RegistrationRejectedException(valid.errors());
    }
    if (!captcha.verify(submission.recaptchaToken())) {
      throw new RegistrationRejectedException(
          List.of(new FieldError("recaptchaToken", ErrorCode.CAPTCHA_FAILED)));
    }
    Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
    Registration registration =
        new Registration(
            UUID.randomUUID(),
            valid.type(),
            now,
            valid.participant(),
            valid.options(),
            valid.consents().stream().map(c -> new GivenConsent(c.id(), c.text(), now)).toList());
    byte[] jsonCopy = storage.store(registration);
    notifyQuietly(registration, jsonCopy);
    return registration;
  }

  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  private void notifyQuietly(Registration registration, byte[] jsonCopy) {
    try {
      notifier.sendParticipantConfirmation(registration);
    } catch (RuntimeException e) {
      LOG.warn("registration {}: participant email not sent", registration.id());
    }
    try {
      notifier.sendOrganizerNotifications(registration, jsonCopy);
    } catch (RuntimeException e) {
      LOG.warn("registration {}: organizer email not sent", registration.id());
    }
  }
}
