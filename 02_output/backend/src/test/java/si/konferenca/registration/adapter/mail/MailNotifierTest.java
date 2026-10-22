package si.konferenca.registration.adapter.mail;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import si.konferenca.registration.domain.ConferenceCatalogue;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.Consent;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

class MailNotifierTest {

  private static final Instant AT = Instant.parse("2026-10-20T10:15:30Z");
  private static final ConferenceCatalogue CATALOGUE =
      new ConferenceCatalogue(
          List.of(
              new ConferenceOption(
                  "ws",
                  "AI workshop",
                  OptionCategory.WORKSHOP,
                  true,
                  EnumSet.allOf(RegistrationType.class))),
          List.of(new Consent("privacy", "I agree.", true)));

  /** Collects messages instead of sending; fails for the addresses in {@code failFor}. */
  private static final class Outbox extends JavaMailSenderImpl {
    final List<MimeMessage> sent = new ArrayList<>();
    final List<String> failFor = new ArrayList<>();

    @Override
    public void send(MimeMessage message) {
      try {
        String to = message.getRecipients(Message.RecipientType.TO)[0].toString();
        if (failFor.contains(to)) {
          throw new MailSendException("down");
        }
        message.saveChanges();
      } catch (jakarta.mail.MessagingException e) {
        throw new IllegalStateException(e);
      }
      sent.add(message);
    }
  }

  private static Registration registration(List<String> options, String organization) {
    return new Registration(
        UUID.fromString("00000000-0000-4000-8000-000000000001"),
        new Participant(
            RegistrationType.EXTERNAL,
            "Ana",
            "Novak",
            "ana@example.org",
            organization,
            null,
            null,
            null),
        options,
        List.of(new GivenConsent("privacy", AT)),
        AT);
  }

  private static String text(MimeMessage message) throws Exception {
    return textOf(message.getContent());
  }

  /** The first text part, however deeply the multiparts nest. */
  private static String textOf(Object content) throws Exception {
    if (content instanceof Multipart multipart) {
      return textOf(multipart.getBodyPart(0).getContent());
    }
    return (String) content;
  }

  @Test
  void sendsConfirmationAndOneNotificationPerOrganizer() throws Exception {
    Outbox outbox = new Outbox();
    MailNotifier notifier =
        new MailNotifier(
            outbox,
            new MailNotifier.Settings(
                "from@konferenca.si", "Konferenca", List.of("o1@x.si", "o2@x.si")),
            CATALOGUE);
    byte[] json = "{\"id\":1}\n".getBytes(StandardCharsets.UTF_8);

    notifier.registrationAccepted(registration(List.of("ws", "gone"), "Org"), json);

    assertThat(outbox.sent).hasSize(3);
    MimeMessage confirmation = outbox.sent.get(0);
    assertThat(confirmation.getSubject()).isEqualTo("Registration confirmed: Konferenca");
    assertThat(confirmation.getFrom()[0].toString()).isEqualTo("from@konferenca.si");
    assertThat(confirmation.getAllRecipients())
        .extracting(Object::toString)
        .containsExactly("ana@example.org");
    assertThat(text(confirmation))
        .contains("Dear Ana Novak,", "Your registration for Konferenca was received.")
        .contains("Registration id: 00000000-0000-4000-8000-000000000001")
        .contains("- AI workshop", "- gone");

    MimeMessage notification = outbox.sent.get(1);
    assertThat(notification.getSubject()).isEqualTo("New registration: Konferenca");
    assertThat(notification.getAllRecipients())
        .extracting(Object::toString)
        .containsExactly("o1@x.si");
    assertThat(outbox.sent.get(2).getAllRecipients())
        .extracting(Object::toString)
        .containsExactly("o2@x.si");
    assertThat(text(notification))
        .contains("Type: EXTERNAL", "Organization / institution: Org", "Options: ws, gone")
        .contains("Consents: privacy", "Registered at: 2026-10-20T10:15:30Z")
        .doesNotContain("Study institution");
    BodyPart attachment = ((Multipart) notification.getContent()).getBodyPart(1);
    assertThat(attachment.getFileName())
        .isEqualTo("registration-00000000-0000-4000-8000-000000000001.json");
    assertThat(attachment.getContentType()).startsWith("application/json");
    assertThat(attachment.getHeader("Content-Transfer-Encoding")).containsExactly("base64");
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    attachment.getDataHandler().writeTo(bytes);
    assertThat(bytes.toByteArray()).isEqualTo(json);
  }

  @Test
  void confirmationWithoutOptionsSaysNone() throws Exception {
    Outbox outbox = new Outbox();
    new MailNotifier(outbox, new MailNotifier.Settings("f@x.si", "K", List.of()), CATALOGUE)
        .registrationAccepted(registration(List.of(), "Org"), new byte[0]);

    assertThat(outbox.sent).hasSize(1);
    assertThat(text(outbox.sent.getFirst())).contains("Selected options:\n- none");
  }

  @Test
  void oneFailedMessageDoesNotStopTheOthers() {
    Outbox outbox = new Outbox();
    outbox.failFor.add("ana@example.org");
    outbox.failFor.add("o1@x.si");
    MailNotifier notifier =
        new MailNotifier(
            outbox,
            new MailNotifier.Settings("f@x.si", "K", List.of("o1@x.si", "o2@x.si")),
            CATALOGUE);

    notifier.registrationAccepted(registration(List.of(), "Org"), new byte[0]);

    assertThat(outbox.sent).hasSize(1);
  }

  @Test
  void invalidRecipientIsSkippedQuietly() {
    Outbox outbox = new Outbox();
    MailNotifier notifier =
        new MailNotifier(
            outbox, new MailNotifier.Settings("f@x.si", "K", List.of("not an address")), CATALOGUE);

    notifier.registrationAccepted(registration(List.of(), "Org"), new byte[0]);

    assertThat(outbox.sent).hasSize(1);
  }

  @Test
  void settingsKeepTheirOwnOrganizerList() {
    List<String> organizers = new ArrayList<>(List.of("o@x.si"));
    MailNotifier.Settings settings = new MailNotifier.Settings("f@x.si", "K", organizers);
    organizers.clear();

    assertThat(settings.organizerEmails()).containsExactly("o@x.si");
  }
}
