package si.konferenca.registration.adapter.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

class SmtpRegistrationNotifierTest {

  private static final Instant AT = Instant.parse("2026-10-21T10:00:00.5Z");
  private static final UUID ID = UUID.fromString("0b6f4a8e-1c2d-4e5f-8a9b-0c1d2e3f4a5b");

  /** Captures messages instead of sending; can fail for chosen recipients. */
  private static final class CapturingSender extends JavaMailSenderImpl {
    final List<MimeMessage> sent = new ArrayList<>();
    final Set<String> failFor;

    CapturingSender(String... failFor) {
      this.failFor = Set.of(failFor);
    }

    @Override
    public void send(MimeMessage message) {
      try {
        String to = message.getRecipients(Message.RecipientType.TO)[0].toString();
        if (failFor.contains(to)) {
          throw new MailSendException("refused");
        }
        message.saveChanges();
        sent.add(message);
      } catch (jakarta.mail.MessagingException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  private final ConferenceCatalog catalog =
      ConferenceCatalog.of(
          List.of(
              new ConferenceOption(
                  "ws-ai",
                  "AI workshop",
                  OptionCategory.WORKSHOP,
                  false,
                  Set.of(RegistrationType.values()))),
          List.of());

  private SmtpRegistrationNotifier notifier(CapturingSender sender) {
    return new SmtpRegistrationNotifier(
        sender, "from@konferenca.si", "Konferenca", List.of("o1@x.si", "o2@x.si"), catalog);
  }

  private static Registration external(List<String> options) {
    return new Registration(
        ID,
        RegistrationType.EXTERNAL,
        "Ana",
        "Novak",
        "ana@example.org",
        "Org",
        null,
        null,
        null,
        options,
        List.of(new GivenConsent("privacy", AT), new GivenConsent("photos", AT)),
        AT);
  }

  private static Registration student() {
    return new Registration(
        ID,
        RegistrationType.STUDENT,
        "Luka",
        "Kranjc",
        "luka@uni.si",
        null,
        "UL",
        "CS",
        "6321",
        List.of(),
        List.of(new GivenConsent("privacy", AT)),
        AT);
  }

  private static String text(MimeMessage message) throws Exception {
    Object content = message.getContent();
    if (content instanceof Multipart multipart) {
      BodyPart first = multipart.getBodyPart(0);
      Object inner = first.getContent();
      if (inner instanceof Multipart nested) {
        return String.valueOf(nested.getBodyPart(0).getContent());
      }
      return String.valueOf(inner);
    }
    return String.valueOf(content);
  }

  @Test
  void participantGetsOnePlainTextMessageWithOptionNames() throws Exception {
    CapturingSender sender = new CapturingSender();

    notifier(sender).notifyParticipant(external(List.of("ws-ai", "unknown-id")));

    assertThat(sender.sent).hasSize(1);
    MimeMessage m = sender.sent.get(0);
    assertThat(Arrays.stream(m.getRecipients(Message.RecipientType.TO)).map(Address::toString))
        .containsExactly("ana@example.org");
    assertThat(m.getFrom()[0].toString()).isEqualTo("from@konferenca.si");
    assertThat(m.getSubject()).isEqualTo("Registration confirmed: Konferenca");
    assertThat(m.getContentType()).startsWith("text/plain").containsIgnoringCase("UTF-8");
    assertThat(text(m))
        .isEqualTo(
            "Dear Ana Novak,\n\nthank you for registering for Konferenca.\n"
                + "Registration type: External participant\nSelected options:\n"
                + "- AI workshop\n- unknown-id\n\nRegistration id: "
                + ID
                + "\n");
  }

  @Test
  void participantWithoutOptionsReadsNone() throws Exception {
    CapturingSender sender = new CapturingSender();

    notifier(sender).notifyParticipant(student());

    assertThat(text(sender.sent.get(0)))
        .contains("Registration type: Student\nSelected options:\n- none\n");
  }

  @Test
  void eachOrganizerGetsTheDataAndTheJsonCopyAttached() throws Exception {
    CapturingSender sender = new CapturingSender();
    byte[] copy = "{\"id\":\"x\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    notifier(sender).notifyOrganizers(external(List.of("ws-ai", "ev-x")), copy);

    assertThat(sender.sent).hasSize(2);
    assertThat(sender.sent.get(1).getRecipients(Message.RecipientType.TO)[0].toString())
        .isEqualTo("o2@x.si");
    MimeMessage m = sender.sent.get(0);
    assertThat(m.getSubject()).isEqualTo("New registration: Konferenca");
    assertThat(text(m))
        .isEqualTo(
            "A new registration was received.\n\nId: "
                + ID
                + "\nType: EXTERNAL\nFirst name: Ana\nLast name: Novak\nEmail: ana@example.org\n"
                + "Organization: Org\nOptions: ws-ai, ev-x\nConsents: privacy, photos\n"
                + "Registered at: 2026-10-21T10:00:00.500Z\n");
    Multipart multipart = (Multipart) m.getContent();
    BodyPart attachment = multipart.getBodyPart(1);
    assertThat(attachment.getFileName()).isEqualTo("registration-" + ID + ".json");
    assertThat(attachment.getContentType()).startsWith("application/json");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    attachment.getDataHandler().writeTo(out);
    assertThat(out.toByteArray()).isEqualTo(copy);
  }

  @Test
  void studentNotificationListsTheStudyFields() throws Exception {
    CapturingSender sender = new CapturingSender();

    notifier(sender).notifyOrganizers(student(), new byte[] {'{', '}'});

    assertThat(text(sender.sent.get(0)))
        .contains(
            "Type: STUDENT\n",
            "Study institution: UL\nStudy programme: CS\nStudent ID: 6321\nOptions: \n")
        .doesNotContain("Organization");
  }

  @Test
  void failedOrganizerMessagesAreCountedAfterTryingEveryAddress() {
    CapturingSender sender = new CapturingSender("o1@x.si");

    assertThatThrownBy(() -> notifier(sender).notifyOrganizers(student(), new byte[] {1}))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("1 organizer notification(s) failed");
    assertThat(sender.sent).hasSize(1);
  }

  @Test
  void allOrganizerMessagesFailing() {
    CapturingSender sender = new CapturingSender("o1@x.si", "o2@x.si");

    assertThatThrownBy(() -> notifier(sender).notifyOrganizers(student(), new byte[] {1}))
        .hasMessage("2 organizer notification(s) failed");
  }
}
