package si.konferenca.registration.adapter.mail;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import si.konferenca.registration.domain.ConferenceConfig;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

class MailRegistrationNotifierTest {

  static final Instant AT = Instant.parse("2026-10-20T10:00:00Z");
  static final UUID ID = UUID.fromString("11111111-2222-4333-8444-555555555555");
  static final ConferenceConfig CONFIG =
      new ConferenceConfig(
          "Konferenca 2026",
          List.of(
              new ConferenceOption(
                  "ws",
                  "AI workshop",
                  OptionCategory.WORKSHOP,
                  true,
                  Set.of(RegistrationType.EXTERNAL))),
          List.of(new ConsentDefinition("privacy", "p", true)));

  static class CapturingSender extends JavaMailSenderImpl {
    final List<MimeMessage> sent = new ArrayList<>();

    @Override
    public void send(MimeMessage... messages) {
      sent.addAll(Arrays.asList(messages));
    }
  }

  final CapturingSender sender = new CapturingSender();
  final MailRegistrationNotifier notifier =
      new MailRegistrationNotifier(
          sender, CONFIG, "registration@konferenca.local", List.of("o1@x.org", "o2@x.org"));

  static Registration external(List<String> options) {
    return new Registration(
        ID,
        RegistrationType.EXTERNAL,
        "Ana\r\nBcc: evil@x.org",
        "<b>Novak</b>",
        "ana@x.org",
        "Org",
        null,
        null,
        null,
        options,
        List.of(new GivenConsent("privacy", AT)),
        AT);
  }

  static Registration student() {
    return new Registration(
        ID,
        RegistrationType.STUDENT,
        "Luka",
        "Horvat",
        "l@x.org",
        null,
        "Uni",
        "CS",
        "123",
        List.of("unknown-id"),
        List.of(),
        AT);
  }

  static List<String> addresses(Address[] addresses) {
    return Arrays.stream(addresses).map(Address::toString).toList();
  }

  @Test
  void participantGetsOnePlainTextMailWithOnlyTheirAddress() throws Exception {
    notifier.sendParticipantConfirmation(external(List.of("ws")));

    assertThat(sender.sent).hasSize(1);
    MimeMessage mail = sender.sent.get(0);
    assertThat(addresses(mail.getAllRecipients())).containsExactly("ana@x.org");
    assertThat(mail.getRecipients(Message.RecipientType.BCC)).isNull();
    assertThat(addresses(mail.getFrom())).containsExactly("registration@konferenca.local");
    assertThat(mail.getSubject()).isEqualTo("Registration confirmed: Konferenca 2026");
    mail.saveChanges();
    assertThat(mail.getContentType()).startsWith("text/plain").containsIgnoringCase("UTF-8");
    assertThat((String) mail.getContent())
        .contains("Dear Ana\r\nBcc: evil@x.org <b>Novak</b>,")
        .contains("thank you for registering for Konferenca 2026.")
        .contains("Registration id: " + ID)
        .contains("Selected options:\n- AI workshop\n");
  }

  @Test
  void participantTextSaysNoneWithoutOptions() {
    assertThat(notifier.participantText(external(List.of())))
        .contains("Selected options:\n- none\n");
  }

  @Test
  void eachOrganizerGetsTheDataAndTheJsonAttachment() throws Exception {
    byte[] json = "{\"id\":\"x\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    notifier.sendOrganizerNotification(external(List.of("ws")), json);
    json[0] = 'X';

    assertThat(sender.sent).hasSize(2);
    assertThat(addresses(sender.sent.get(0).getAllRecipients())).containsExactly("o1@x.org");
    assertThat(addresses(sender.sent.get(1).getAllRecipients())).containsExactly("o2@x.org");
    for (MimeMessage mail : sender.sent) {
      mail.saveChanges();
      assertThat(addresses(mail.getFrom())).containsExactly("registration@konferenca.local");
      assertThat(mail.getSubject()).isEqualTo("New registration: Konferenca 2026");
      Part attachment = findAttachment(mail);
      assertThat(attachment.getFileName()).isEqualTo("registration-" + ID + ".json");
      assertThat(attachment.getContentType()).startsWith("application/json");
      assertThat(attachment.getInputStream().readAllBytes()).isEqualTo("{\"id\":\"x\"}".getBytes());
    }
  }

  @Test
  void organizerTextListsExternalFields() {
    assertThat(notifier.organizerText(external(List.of("ws"))))
        .isEqualTo(
            "New registration for Konferenca 2026\n\n"
                + "id: "
                + ID
                + "\n"
                + "type: EXTERNAL\n"
                + "firstName: Ana\r\nBcc: evil@x.org\n"
                + "lastName: <b>Novak</b>\n"
                + "email: ana@x.org\n"
                + "organization: Org\n"
                + "options: ws (AI workshop)\n"
                + "consents: privacy (2026-10-20T10:00:00Z)\n"
                + "registeredAt: 2026-10-20T10:00:00Z\n"
                + "\nThe registration as stored is attached as JSON.\n");
  }

  @Test
  void organizerTextListsStudentFieldsAndUnknownOptionIds() {
    assertThat(notifier.organizerText(student()))
        .contains("studyInstitution: Uni\nstudyProgramme: CS\nstudentId: 123\n")
        .contains("options: unknown-id (unknown-id)\n")
        .contains("consents: \n")
        .doesNotContain("organization:");
  }

  @Test
  void missingValuesAreWrittenEmpty() {
    Registration noOrganization =
        new Registration(
            ID,
            RegistrationType.EXTERNAL,
            "A",
            "B",
            "a@x.org",
            null,
            null,
            null,
            null,
            List.of(),
            List.of(),
            AT);
    assertThat(notifier.organizerText(noOrganization)).contains("organization: \n");
  }

  static Part findAttachment(Part part) throws Exception {
    if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
      return part;
    }
    if (part.getContent() instanceof Multipart multipart) {
      for (int i = 0; i < multipart.getCount(); i++) {
        BodyPart child = multipart.getBodyPart(i);
        Part found = findAttachment(child);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }
}
