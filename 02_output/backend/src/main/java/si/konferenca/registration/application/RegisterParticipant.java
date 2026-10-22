package si.konferenca.registration.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationForm;
import si.konferenca.registration.domain.RegistrationValidator;

/**
 * Registers a participant (US-001, US-002): validate, verify the captcha, store the row and the
 * JSON copy in one unit of work (AR-05), then send the emails (OQ-03).
 */
public class RegisterParticipant {

  private static final Logger LOG = LoggerFactory.getLogger(RegisterParticipant.class);

  private final RegistrationValidator validator;
  private final CaptchaVerifier captcha;
  private final RegistrationStore store;
  private final JsonCopyStore jsonCopies;
  private final TransactionRunner transactions;
  private final Notifier notifier;
  private final Clock clock;

  public RegisterParticipant(
      RegistrationValidator validator,
      CaptchaVerifier captcha,
      RegistrationStore store,
      JsonCopyStore jsonCopies,
      TransactionRunner transactions,
      Notifier notifier,
      Clock clock) {
    this.validator = validator;
    this.captcha = captcha;
    this.store = store;
    this.jsonCopies = jsonCopies;
    this.transactions = transactions;
    this.notifier = notifier;
    this.clock = clock;
  }

  public RegistrationOutcome register(RegistrationForm form) {
    RegistrationValidator.Result result = validator.validate(form);
    List<FieldError> errors = new ArrayList<>(result.errors());
    String token = form.captchaToken() == null ? "" : form.captchaToken().strip();
    if (token.isEmpty()) {
      errors.add(new FieldError("captchaToken", "Confirm that you are not a robot."));
    } else if (!captcha.verify(token)) {
      errors.add(new FieldError("captchaToken", "The reCAPTCHA check failed. Please try again."));
    }
    if (!errors.isEmpty()) {
      return new RegistrationOutcome.Rejected(errors);
    }

    Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
    Registration registration =
        new Registration(
            UUID.randomUUID(),
            result.participant(),
            result.options(),
            result.consents().stream().map(id -> new GivenConsent(id, now)).toList(),
            now);
    byte[] jsonCopy = store(registration);
    try {
      notifier.registrationAccepted(registration, jsonCopy);
    } catch (RuntimeException e) {
      LOG.warn("Email for registration {} failed", registration.id());
    }
    return new RegistrationOutcome.Accepted(registration.id());
  }

  private byte[] store(Registration registration) {
    byte[][] written = new byte[1][];
    try {
      transactions.inTransaction(
          () -> {
            store.add(registration);
            written[0] = jsonCopies.write(registration);
          });
    } catch (RuntimeException e) {
      jsonCopies.delete(registration.id());
      throw new RegistrationNotStoredException(registration.id(), e);
    }
    return written[0];
  }
}
