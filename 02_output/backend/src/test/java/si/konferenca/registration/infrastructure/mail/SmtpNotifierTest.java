package si.konferenca.registration.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import si.konferenca.registration.domain.ConferenceConfiguration;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.infrastructure.config.AppSettings;

class SmtpNotifierTest {

  private static final Instant AT = Instant.parse("2026-10-20T10:15:00.250Z");
  private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-00000000000a");

  private final JavaMailSender sender = mock(JavaMailSender.class);
  private final List<MimeMessage> sent = new ArrayList<>();

  @BeforeEach
  void captureSentMessages() {
    when(sender.createMimeMessage())
        .thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
    doAnswer(
            i -> {
              MimeMessage message = i.getArgument(0);
              message.saveChanges();
              sent.add(message);
              return null;
            })
        .when(sender)
        .send(any(MimeMessage.class));
  }

  private SmtpNotifier notifier(String organizerEmails) {
    AppSettings settings =
        new AppSettings(
            "local",
            "Konferenca 2026",
            "registration@konferenca.si",
            "",
            "d",
            new AppSettings.Organizer("o", "p", organizerEmails, true),
            new AppSettings.Captcha(true, "", "", "u"),
            "",
            60,
            100);
    ConferenceConfiguration config =
        new ConferenceConfiguration(
            List.of(
                new ConferenceOption(
                    "ws-ai",
                    "AI workshop",
                    OptionCategory.WORKSHOP,
                    true,
                    Set.of(RegistrationType.values()))),
            List.of(new ConsentDefinition("privacy", "I agree.", true)));
    return new SmtpNotifier(sender, settings, config);
  }

  static Registration external(List<String> options) {
    return new Registration(
        ID,
        RegistrationType.EXTERNAL,
        "Ana\r\nBcc: x@evil.example",
        "<b>Novák</b>",
        "ana@example.org",
        "Org",
        null,
        null,
        null,
        options,
        List.of(new Registration.GivenConsent("privacy", AT)),
        AT);
  }

  private static List<String> addresses(Address[] addresses) {
    return addresses == null ? List.of() : Arrays.stream(addresses).map(Address::toString).toList();
  }

  @Test
  void confirmationGoesOnlyToTheParticipantAsPlainText() throws Exception {
    notifier("org@x.y").sendConfirmation(external(List.of("ws-ai", "gone")));

    assertThat(sent).hasSize(1);
    MimeMessage message = sent.get(0);
    assertThat(addresses(message.getRecipients(Message.RecipientType.TO)))
        .containsExactly("ana@example.org");
    assertThat(message.getRecipients(Message.RecipientType.CC)).isNull();
    assertThat(message.getRecipients(Message.RecipientType.BCC)).isNull();
    assertThat(addresses(message.getFrom())).containsExactly("registration@konferenca.si");
    assertThat(message.getSubject()).isEqualTo("Registration confirmation: Konferenca 2026");
    assertThat(message.getContentType()).startsWith("text/plain").contains("UTF-8");
    String text = (String) message.getContent();
    assertThat(text).contains("<b>Novák</b>", "- AI workshop", "- gone", "Konferenca 2026");
    assertThat(message.getHeader("Bcc")).isNull();
  }

  @Test
  void organizersEachGetTheDataWithTheExactJsonCopyAttached() throws Exception {
    byte[] copy = "{\"id\":\"x\",\"n\":\"Č\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    notifier(" a@org.si , b@org.si ").notifyOrganizers(external(List.of("ws-ai")), copy);

    assertThat(sent).hasSize(2);
    assertThat(addresses(sent.get(0).getRecipients(Message.RecipientType.TO)))
        .containsExactly("a@org.si");
    assertThat(addresses(sent.get(1).getRecipients(Message.RecipientType.TO)))
        .containsExactly("b@org.si");
    MimeMessage message = sent.get(1);
    assertThat(message.getSubject()).isEqualTo("New registration " + ID);
    Multipart multipart = (Multipart) message.getContent();
    List<BodyPart> parts = flatten(multipart);
    BodyPart attachment = part(parts, true);
    assertThat(attachment.getFileName()).isEqualTo("registration-" + ID + ".json");
    assertThat(attachment.getContentType()).startsWith("application/json");
    try (InputStream in = attachment.getInputStream()) {
      assertThat(in.readAllBytes()).isEqualTo(copy);
    }
    BodyPart text = part(parts, false);
    assertThat((String) text.getContent()).contains("Email: ana@example.org", "Options: ws-ai");
  }

  /** The first part with (or without) a file name. */
  private static BodyPart part(List<BodyPart> parts, boolean attachment) throws Exception {
    for (BodyPart part : parts) {
      if ((part.getFileName() != null) == attachment) {
        return part;
      }
    }
    throw new AssertionError("no " + (attachment ? "attachment" : "text") + " part");
  }

  private static List<BodyPart> flatten(Multipart multipart) throws Exception {
    List<BodyPart> parts = new ArrayList<>();
    for (int i = 0; i < multipart.getCount(); i++) {
      BodyPart part = multipart.getBodyPart(i);
      if (part.getContent() instanceof Multipart nested) {
        parts.addAll(flatten(nested));
      } else {
        parts.add(part);
      }
    }
    return parts;
  }

  @Test
  void noOrganizerAddressesSendNothing() {
    notifier(" , ").notifyOrganizers(external(List.of()), new byte[0]);

    verify(sender, never()).send(any(MimeMessage.class));
  }

  @Test
  void aFailingMessageDoesNotStopTheOthersOrThrow() {
    doThrow(new MailSendException("down"))
        .doAnswer(i -> sent.add(i.getArgument(0)))
        .when(sender)
        .send(any(MimeMessage.class));

    notifier("a@org.si,b@org.si").notifyOrganizers(external(List.of()), new byte[] {1});

    assertThat(sent).hasSize(1);
  }

  @Test
  void invalidAddressesAndSendFailuresAreSwallowed() {
    notifier("not an address").notifyOrganizers(external(List.of()), new byte[] {1});
    doThrow(new MailSendException("down")).when(sender).send(any(MimeMessage.class));
    notifier("a@org.si").sendConfirmation(external(List.of()));

    assertThat(sent).isEmpty();
  }
}
