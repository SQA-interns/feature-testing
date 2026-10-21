package si.konferenca.registration.adapter.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import si.konferenca.registration.application.RegistrationNotifier;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

/**
 * Sends the emails of {@code emails.json}: text/plain UTF-8 only, fixed subjects, recipients only
 * from the validated email or the configured organizer list (SR-05, SR-07).
 */
public class SmtpRegistrationNotifier implements RegistrationNotifier {

  private static final String UTF_8 = "UTF-8";

  private final JavaMailSender sender;
  private final String from;
  private final String conferenceName;
  private final List<String> organizerEmails;
  private final ConferenceCatalog catalog;

  public SmtpRegistrationNotifier(
      JavaMailSender sender,
      String from,
      String conferenceName,
      List<String> organizerEmails,
      ConferenceCatalog catalog) {
    this.sender = sender;
    this.from = from;
    this.conferenceName = conferenceName;
    this.organizerEmails = List.copyOf(organizerEmails);
    this.catalog = catalog;
  }

  @Override
  public void notifyParticipant(Registration registration) {
    try {
      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, UTF_8);
      helper.setFrom(from);
      helper.setTo(registration.email());
      helper.setSubject("Registration confirmed: " + conferenceName);
      helper.setText(participantText(registration), false);
      sender.send(message);
    } catch (MessagingException e) {
      throw new IllegalStateException("Participant email could not be built", e);
    }
  }

  @Override
  public void notifyOrganizers(Registration registration, byte[] jsonCopy) {
    int failed = 0;
    for (String organizer : organizerEmails) {
      try {
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, UTF_8);
        helper.setFrom(from);
        helper.setTo(organizer);
        helper.setSubject("New registration: " + conferenceName);
        helper.setText(organizerText(registration), false);
        helper.addAttachment(
            "registration-" + registration.id() + ".json",
            new ByteArrayResource(jsonCopy.clone()),
            "application/json");
        sender.send(message);
      } catch (MessagingException | MailException e) {
        failed++;
      }
    }
    if (failed > 0) {
      throw new IllegalStateException(failed + " organizer notification(s) failed");
    }
  }

  private String participantText(Registration r) {
    StringBuilder text = new StringBuilder(256);
    text.append("Dear ")
        .append(r.firstName())
        .append(' ')
        .append(r.lastName())
        .append(",\n\nthank you for registering for ")
        .append(conferenceName)
        .append(".\nRegistration type: ")
        .append(r.type() == RegistrationType.STUDENT ? "Student" : "External participant")
        .append("\nSelected options:\n");
    if (r.options().isEmpty()) {
      text.append("- none\n");
    }
    for (String option : r.options()) {
      text.append("- ").append(optionName(option)).append('\n');
    }
    text.append("\nRegistration id: ").append(r.id()).append('\n');
    return text.toString();
  }

  private static String organizerText(Registration r) {
    StringBuilder text = new StringBuilder(512);
    text.append("A new registration was received.\n\n");
    line(text, "Id", r.id().toString());
    line(text, "Type", r.type().name());
    line(text, "First name", r.firstName());
    line(text, "Last name", r.lastName());
    line(text, "Email", r.email());
    if (r.type() == RegistrationType.EXTERNAL) {
      line(text, "Organization", r.organization());
    } else {
      line(text, "Study institution", r.studyInstitution());
      line(text, "Study programme", r.studyProgramme());
      line(text, "Student ID", r.studentId());
    }
    line(text, "Options", String.join(", ", r.options()));
    line(text, "Consents", String.join(", ", r.consents().stream().map(GivenConsent::id).toList()));
    line(text, "Registered at", r.registeredAt().toString());
    return text.toString();
  }

  private static void line(StringBuilder text, String label, String value) {
    text.append(label).append(": ").append(value).append('\n');
  }

  private String optionName(String id) {
    return catalog.option(id).map(ConferenceOption::name).orElse(id);
  }
}
