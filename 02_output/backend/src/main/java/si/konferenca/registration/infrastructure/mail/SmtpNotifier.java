package si.konferenca.registration.infrastructure.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import si.konferenca.registration.application.Notifier;
import si.konferenca.registration.domain.ConferenceConfiguration;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.infrastructure.config.AppSettings;

/**
 * Sends the emails through SMTP. Each message is sent on its own; a failure is logged with the
 * registration id only and never changes the accepted registration (OQ-03, ES-07).
 */
@Component
public class SmtpNotifier implements Notifier {

  private static final Logger LOG = LoggerFactory.getLogger(SmtpNotifier.class);

  private final JavaMailSender sender;
  private final AppSettings settings;
  private final ConferenceConfiguration configuration;

  public SmtpNotifier(
      JavaMailSender sender, AppSettings settings, ConferenceConfiguration configuration) {
    this.sender = sender;
    this.settings = settings;
    this.configuration = configuration;
  }

  @Override
  public void sendConfirmation(Registration registration) {
    List<String> optionNames =
        registration.options().stream()
            .map(id -> configuration.option(id).map(ConferenceOption::name).orElse(id))
            .toList();
    try {
      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
      helper.setFrom(settings.mailFrom());
      helper.setTo(registration.email());
      helper.setSubject("Registration confirmation: " + settings.conferenceName());
      helper.setText(
          MailTexts.confirmation(settings.conferenceName(), registration, optionNames), false);
      sender.send(message);
    } catch (MessagingException | MailException e) {
      LOG.warn("Email confirmation failed for registration {}", registration.id());
    }
  }

  @Override
  public void notifyOrganizers(Registration registration, byte[] jsonCopy) {
    String text = MailTexts.organizerNotification(settings.conferenceName(), registration);
    for (String organizer : settings.organizer().emailList()) {
      try {
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(settings.mailFrom());
        helper.setTo(organizer);
        helper.setSubject("New registration " + registration.id());
        helper.setText(text, false);
        helper.addAttachment(
            "registration-" + registration.id() + ".json",
            new ByteArrayResource(jsonCopy),
            "application/json");
        sender.send(message);
      } catch (MessagingException | MailException e) {
        LOG.warn("Email organizer notification failed for registration {}", registration.id());
      }
    }
  }
}
