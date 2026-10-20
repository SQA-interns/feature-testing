package si.konferenca.registration.adapter.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.StringJoiner;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import si.konferenca.registration.application.RegistrationNotifier;
import si.konferenca.registration.domain.ConferenceConfig;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

/**
 * Participant confirmation (US-006) and organizer notification (US-007) over SMTP.
 *
 * <p>SR-05: bodies are {@code text/plain; charset=UTF-8}, so markup is shown literally; headers
 * hold no participant input except the validated recipient address, which is set through the mail
 * API. SR-07: the organizer mail carries only registration fields.
 */
public class MailRegistrationNotifier implements RegistrationNotifier {

  private static final String UTF_8 = "UTF-8";

  private final JavaMailSender sender;
  private final ConferenceConfig conference;
  private final String from;
  private final List<String> organizers;

  public MailRegistrationNotifier(
      JavaMailSender sender, ConferenceConfig conference, String from, List<String> organizers) {
    this.sender = sender;
    this.conference = conference;
    this.from = from;
    this.organizers = List.copyOf(organizers);
  }

  @Override
  public void sendParticipantConfirmation(Registration registration) {
    MimeMessage message = sender.createMimeMessage();
    try {
      MimeMessageHelper helper = new MimeMessageHelper(message, false, UTF_8);
      helper.setFrom(from);
      helper.setTo(registration.email());
      helper.setSubject("Registration confirmed: " + conference.conferenceName());
      helper.setText(participantText(registration), false);
    } catch (MessagingException e) {
      throw new MailPreparationException(e);
    }
    sender.send(message);
  }

  @Override
  public void sendOrganizerNotification(Registration registration, byte[] jsonCopy) {
    for (String organizer : organizers) {
      MimeMessage message = sender.createMimeMessage();
      try {
        MimeMessageHelper helper = new MimeMessageHelper(message, true, UTF_8);
        helper.setFrom(from);
        helper.setTo(organizer);
        helper.setSubject("New registration: " + conference.conferenceName());
        helper.setText(organizerText(registration), false);
        helper.addAttachment(
            "registration-" + registration.id() + ".json",
            new ByteArrayResource(jsonCopy.clone()),
            "application/json");
      } catch (MessagingException e) {
        throw new MailPreparationException(e);
      }
      sender.send(message);
    }
  }

  String participantText(Registration r) {
    StringBuilder text = new StringBuilder(256);
    text.append("Dear ")
        .append(r.firstName())
        .append(' ')
        .append(r.lastName())
        .append(",\n\nthank you for registering for ")
        .append(conference.conferenceName())
        .append(".\n\nRegistration id: ")
        .append(r.id())
        .append("\nSelected options:\n");
    if (r.options().isEmpty()) {
      text.append("- none\n");
    }
    for (String id : r.options()) {
      text.append("- ").append(optionName(id)).append('\n');
    }
    text.append("\nKind regards,\nThe organizers\n");
    return text.toString();
  }

  String organizerText(Registration r) {
    StringBuilder text = new StringBuilder(512);
    text.append("New registration for ").append(conference.conferenceName()).append("\n\n");
    line(text, "id", r.id().toString());
    line(text, "type", r.type().name());
    line(text, "firstName", r.firstName());
    line(text, "lastName", r.lastName());
    line(text, "email", r.email());
    if (r.type() == RegistrationType.EXTERNAL) {
      line(text, "organization", r.organization());
    } else {
      line(text, "studyInstitution", r.studyInstitution());
      line(text, "studyProgramme", r.studyProgramme());
      line(text, "studentId", r.studentId());
    }
    StringJoiner options = new StringJoiner(", ");
    for (String id : r.options()) {
      options.add(id + " (" + optionName(id) + ")");
    }
    line(text, "options", options.toString());
    StringJoiner consents = new StringJoiner(", ");
    for (GivenConsent consent : r.consents()) {
      consents.add(
          consent.id() + " (" + DateTimeFormatter.ISO_INSTANT.format(consent.givenAt()) + ")");
    }
    line(text, "consents", consents.toString());
    line(text, "registeredAt", DateTimeFormatter.ISO_INSTANT.format(r.registeredAt()));
    text.append("\nThe registration as stored is attached as JSON.\n");
    return text.toString();
  }

  private String optionName(String id) {
    return conference.option(id).map(ConferenceOption::name).orElse(id);
  }

  private static void line(StringBuilder text, String label, String value) {
    text.append(label).append(": ").append(value == null ? "" : value).append('\n');
  }
}
