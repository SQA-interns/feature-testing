package si.konferenca.registration.adapter.mail;

import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import si.konferenca.registration.application.port.RegistrationNotifier;
import si.konferenca.registration.config.AppProperties;
import si.konferenca.registration.domain.Registration;

/**
 * Sends the participant confirmation and the organizer notifications as plain text (emails.md,
 * SR-05): fixed subjects, one parsed recipient per message, no Cc, Bcc or Reply-To.
 */
@Component
public class MailRegistrationNotifier implements RegistrationNotifier {

  private static final Logger LOG = LoggerFactory.getLogger(MailRegistrationNotifier.class);
  private static final String UTF_8 = "UTF-8";

  private final JavaMailSender sender;
  private final AppProperties properties;

  public MailRegistrationNotifier(JavaMailSender sender, AppProperties properties) {
    this.sender = sender;
    this.properties = properties;
  }

  @Override
  public void sendParticipantConfirmation(Registration registration) {
    String conference = properties.conference().name();
    try {
      MimeMessage message = newMessage(registration.participant().email());
      message.setSubject("Registration confirmed: " + conference, UTF_8);
      message.setText(MailTexts.participant(registration, conference), UTF_8);
      sender.send(message);
    } catch (MessagingException e) {
      throw new MailPreparationException("participant email not prepared", e);
    }
  }

  @Override
  public void sendOrganizerNotifications(Registration registration, byte[] jsonCopy) {
    for (String organizer : properties.organizer().emailList()) {
      try {
        sender.send(organizerMessage(organizer, registration, jsonCopy));
      } catch (MessagingException | MailException e) {
        LOG.warn("registration {}: organizer email not sent", registration.id());
      }
    }
  }

  private MimeMessage organizerMessage(String to, Registration registration, byte[] jsonCopy)
      throws MessagingException {
    String conference = properties.conference().name();
    MimeMessage message = newMessage(to);
    message.setSubject("New registration: " + conference, UTF_8);
    MimeBodyPart text = new MimeBodyPart();
    text.setText(MailTexts.organizer(registration, conference), UTF_8);
    MimeBodyPart attachment = new MimeBodyPart();
    attachment.setDataHandler(
        new DataHandler(new ByteArrayDataSource(jsonCopy, "application/json; charset=UTF-8")));
    attachment.setFileName("registration-" + registration.id() + ".json");
    attachment.setHeader("Content-Transfer-Encoding", "base64");
    MimeMultipart mixed = new MimeMultipart("mixed");
    mixed.addBodyPart(text);
    mixed.addBodyPart(attachment);
    message.setContent(mixed);
    return message;
  }

  private MimeMessage newMessage(String to) throws MessagingException {
    MimeMessage message = sender.createMimeMessage();
    message.setFrom(new InternetAddress(properties.mail().from(), true));
    message.setRecipient(Message.RecipientType.TO, new InternetAddress(to, true));
    return message;
  }
}
