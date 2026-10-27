package si.konferenca.registration.adapter.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import si.konferenca.registration.config.AppProperties;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.SelectedOption;

class MailAdapterTest {

  private static final UUID ID = UUID.fromString("0b6a8f3e-1c2d-4e5f-8a9b-0c1d2e3f4a5b");
  private static final Instant AT = Instant.parse("2026-10-27T10:00:00.120Z");

  private static Registration external(List<SelectedOption> options) {
    return new Registration(
        ID,
        RegistrationType.EXTERNAL,
        AT,
        new Participant("Ana\r\nBcc: x", "Novak", "ana@example.org", "Inštitut", null, null, null),
        options,
        List.of(new GivenConsent("privacy", "I agree\nfully", AT)));
  }

  private static final List<SelectedOption> OPTIONS =
      List.of(
          new SelectedOption("m", "Lunch", OptionCategory.MEAL),
          new SelectedOption("w1", "Testing", OptionCategory.WORKSHOP),
          new SelectedOption("o", "Tour", OptionCategory.OTHER),
          new SelectedOption("e", "Dinner", OptionCategory.EVENT),
          new SelectedOption("w2", "Security", OptionCategory.WORKSHOP));

  @Test
  void participantTextKeepsUserValuesOnOneLine() {
    assertThat(MailTexts.participant(external(OPTIONS), "Conf 2026"))
        .isEqualTo(
            "Conf 2026\n\nDear Ana  Bcc: x Novak,\n\nyour registration has been received.\n\n"
                + "Registration id: "
                + ID
                + "\nRegistration type: External participant\n\n"
                + "Selected options:\nWorkshops: Testing; Security\nEvents: Dinner\n"
                + "Meals: Lunch\nOther activities: Tour\n");
  }

  @Test
  void organizerTextListsSetFieldsAndConsents() {
    Registration student =
        new Registration(
            ID,
            RegistrationType.STUDENT,
            AT,
            new Participant("Žiga", "Kos", "z@x.si", null, "UL", "FRI", "63\n1"),
            List.of(),
            List.of(new GivenConsent("privacy", "I agree\nfully", AT)));

    assertThat(MailTexts.organizer(student, "Conf"))
        .isEqualTo(
            "New registration for Conf\n\nRegistration id: "
                + ID
                + "\nType: Student\n"
                + "Registered at: 2026-10-27T10:00:00.120Z\nFirst name: Žiga\nLast name: Kos\n"
                + "Email: z@x.si\nStudy institution: UL\nStudy programme: FRI\n"
                + "Student ID: 63 1\n\nSelected options:\nNo options selected.\n\nConsents:\n"
                + "- I agree fully (privacy, given 2026-10-27T10:00:00.120Z)\n");
    assertThat(MailTexts.organizer(external(List.of()), "Conf"))
        .contains("Type: External participant\n")
        .contains("Organization / institution: Inštitut\n")
        .doesNotContain("Study institution");
  }

  private static AppProperties properties(String from, String organizers, String user) {
    return new AppProperties(
        "test",
        new AppProperties.Conference("Conf", Path.of("c.json")),
        Path.of("c"),
        null,
        new AppProperties.Organizer("o", "p", organizers, false),
        new AppProperties.Mail("smtp.local", 2525, true, user, "pw", from),
        null,
        null,
        0);
  }

  private static JavaMailSender sender() {
    JavaMailSender sender = mock(JavaMailSender.class);
    when(sender.createMimeMessage())
        .thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
    return sender;
  }

  @Test
  void participantConfirmationGoesToTheParticipantOnly() throws Exception {
    JavaMailSender sender = sender();

    new MailRegistrationNotifier(sender, properties("reg@x.si", "a@x.si", null))
        .sendParticipantConfirmation(external(OPTIONS));

    ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
    verify(sender).send(sent.capture());
    MimeMessage message = sent.getValue();
    assertThat(message.getSubject()).isEqualTo("Registration confirmed: Conf");
    assertThat(message.getFrom()[0].toString()).isEqualTo("reg@x.si");
    assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString())
        .isEqualTo("ana@example.org");
    assertThat(message.getRecipients(Message.RecipientType.CC)).isNull();
    assertThat(message.getRecipients(Message.RecipientType.BCC)).isNull();
    assertThat(message.getContent()).isEqualTo(MailTexts.participant(external(OPTIONS), "Conf"));
  }

  @Test
  void invalidSenderIsAPreparationError() {
    JavaMailSender sender = sender();
    MailRegistrationNotifier notifier =
        new MailRegistrationNotifier(sender, properties("not an address", "a@x.si", null));

    assertThatThrownBy(() -> notifier.sendParticipantConfirmation(external(OPTIONS)))
        .isInstanceOf(MailPreparationException.class)
        .hasMessageContaining("participant email not prepared");
    verify(sender, never()).send(any(MimeMessage.class));
  }

  @Test
  void organizersGetOneMessageEachWithTheCopyAttached() throws Exception {
    JavaMailSender sender = sender();
    byte[] copy = "{\"a\":\"č\"}\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    new MailRegistrationNotifier(sender, properties("reg@x.si", "o1@x.si, o2@x.si", null))
        .sendOrganizerNotifications(external(OPTIONS), copy);

    ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
    verify(sender, times(2)).send(sent.capture());
    assertThat(sent.getAllValues())
        .extracting(m -> m.getRecipients(Message.RecipientType.TO)[0].toString())
        .containsExactly("o1@x.si", "o2@x.si");
    MimeMessage message = sent.getAllValues().get(0);
    message.saveChanges();
    assertThat(message.getSubject()).isEqualTo("New registration: Conf");
    MimeMultipart parts = (MimeMultipart) message.getContent();
    assertThat(parts.getCount()).isEqualTo(2);
    assertThat(parts.getBodyPart(0).getContent())
        .isEqualTo(MailTexts.organizer(external(OPTIONS), "Conf"));
    var attachment = parts.getBodyPart(1);
    assertThat(attachment.getFileName()).isEqualTo("registration-" + ID + ".json");
    assertThat(attachment.getContentType()).startsWith("application/json");
    assertThat(attachment.getHeader("Content-Transfer-Encoding")).containsExactly("base64");
    assertThat(attachment.getInputStream().readAllBytes()).isEqualTo(copy);
  }

  @Test
  void aFailingOrganizerAddressDoesNotStopTheOthers() {
    JavaMailSender sender = sender();
    doThrow(new MailSendException("down")).doNothing().when(sender).send(any(MimeMessage.class));

    new MailRegistrationNotifier(
            sender, properties("reg@x.si", "o1@x.si,bad address,o3@x.si", null))
        .sendOrganizerNotifications(external(OPTIONS), new byte[] {1});

    verify(sender, times(2)).send(any(MimeMessage.class));
  }

  @Test
  void senderUsesTheSettingsAndTimeouts() {
    JavaMailSenderImpl withAuth =
        (JavaMailSenderImpl)
            new MailConfiguration().javaMailSender(properties("r@x.si", "a@x.si", "user"));
    JavaMailSenderImpl withoutAuth =
        (JavaMailSenderImpl)
            new MailConfiguration().javaMailSender(properties("r@x.si", "a@x.si", " "));

    assertThat(withAuth.getHost()).isEqualTo("smtp.local");
    assertThat(withAuth.getPort()).isEqualTo(2525);
    assertThat(withAuth.getDefaultEncoding()).isEqualTo("UTF-8");
    assertThat(withAuth.getUsername()).isEqualTo("user");
    assertThat(withAuth.getPassword()).isEqualTo("pw");
    assertThat(withAuth.getJavaMailProperties())
        .containsEntry("mail.smtp.auth", "true")
        .containsEntry("mail.smtp.starttls.enable", "true")
        .containsEntry("mail.smtp.starttls.required", "true")
        .containsEntry("mail.smtp.connectiontimeout", "5000")
        .containsEntry("mail.smtp.timeout", "5000")
        .containsEntry("mail.smtp.writetimeout", "5000");
    assertThat(withoutAuth.getUsername()).isNull();
    assertThat(withoutAuth.getPassword()).isNull();
    assertThat(withoutAuth.getJavaMailProperties()).containsEntry("mail.smtp.auth", "false");
    JavaMailSenderImpl noUser =
        (JavaMailSenderImpl)
            new MailConfiguration().javaMailSender(properties("r@x.si", "a@x.si", null));
    assertThat(noUser.getJavaMailProperties()).containsEntry("mail.smtp.auth", "false");
  }
}
