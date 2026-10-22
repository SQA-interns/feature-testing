package si.konferenca.registration.adapter.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import si.konferenca.registration.application.Notifier;
import si.konferenca.registration.domain.ConferenceCatalogue;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;

/**
 * Plain-text emails ({@code docs/02_contracts/emails.md}): user input only ever goes into a
 * text/plain body, so it cannot add headers, recipients or markup (SR-05). A failed message is
 * logged with the registration id only (OQ-03).
 */
public class MailNotifier implements Notifier {

  private static final Logger LOG = LoggerFactory.getLogger(MailNotifier.class);

  /** Sender, conference name and organizer addresses. */
  public record Settings(String from, String conferenceName, List<String> organizerEmails) {
    public Settings {
      organizerEmails = List.copyOf(organizerEmails);
    }
  }

  private interface Message {
    MimeMessage build() throws MessagingException;
  }

  private final JavaMailSender sender;
  private final Settings settings;
  private final ConferenceCatalogue catalogue;

  public MailNotifier(JavaMailSender sender, Settings settings, ConferenceCatalogue catalogue) {
    this.sender = sender;
    this.settings = settings;
    this.catalogue = catalogue;
  }

  @Override
  public void registrationAccepted(Registration registration, byte[] jsonCopy) {
    send(registration, () -> confirmation(registration));
    for (String organizer : settings.organizerEmails()) {
      send(registration, () -> notification(registration, jsonCopy, organizer));
    }
  }

  private void send(Registration registration, Message message) {
    try {
      sender.send(message.build());
    } catch (MessagingException | MailException e) {
      LOG.warn("Email for registration {} failed", registration.id());
    }
  }

  private MimeMessage confirmation(Registration registration) throws MessagingException {
    Participant p = registration.participant();
    StringBuilder body = new StringBuilder();
    body.append("Dear ").append(p.firstName()).append(' ').append(p.lastName()).append(",\n\n");
    body.append("Your registration for ")
        .append(settings.conferenceName())
        .append(" was received.\n\n");
    body.append("Registration id: ").append(registration.id()).append("\n\n");
    body.append("Selected options:\n");
    if (registration.options().isEmpty()) {
      body.append("- none\n");
    }
    for (String id : registration.options()) {
      body.append("- ").append(optionName(id)).append('\n');
    }
    return message(
        p.email(), "Registration confirmed: " + settings.conferenceName(), body, null, null);
  }

  private MimeMessage notification(Registration registration, byte[] jsonCopy, String organizer)
      throws MessagingException {
    Participant p = registration.participant();
    StringBuilder body = new StringBuilder("A new registration was received.\n\n");
    line(body, "Registration id", registration.id().toString());
    line(body, "Type", p.type().name());
    line(body, "First name", p.firstName());
    line(body, "Last name", p.lastName());
    line(body, "Email", p.email());
    line(body, "Organization / institution", p.organization());
    line(body, "Study institution", p.studyInstitution());
    line(body, "Study programme", p.studyProgramme());
    line(body, "Student ID", p.studentId());
    line(body, "Options", String.join(", ", registration.options()));
    line(body, "Consents", String.join(", ", registration.consentIds()));
    line(body, "Registered at", registration.registeredAt().toString());
    return message(
        organizer,
        "New registration: " + settings.conferenceName(),
        body,
        "registration-" + registration.id() + ".json",
        jsonCopy);
  }

  private MimeMessage message(
      String to, String subject, CharSequence body, String attachmentName, byte[] attachment)
      throws MessagingException {
    MimeMessage message = sender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, attachment != null, "UTF-8");
    helper.setFrom(settings.from());
    helper.setTo(new InternetAddress(to, true));
    helper.setSubject(subject);
    helper.setText(body.toString(), false);
    if (attachment != null) {
      helper.addAttachment(attachmentName, new ByteArrayResource(attachment), "application/json");
    }
    return message;
  }

  private static void line(StringBuilder body, String label, String value) {
    if (value != null) {
      body.append(label).append(": ").append(value).append('\n');
    }
  }

  private String optionName(String id) {
    return catalogue.option(id).map(ConferenceOption::name).orElse(id);
  }
}
