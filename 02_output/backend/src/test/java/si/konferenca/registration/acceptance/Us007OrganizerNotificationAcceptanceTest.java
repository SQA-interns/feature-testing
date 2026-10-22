package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.acceptance.support.Submissions.validExternal;
import static si.konferenca.registration.acceptance.support.Submissions.validStudent;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.Mailpit;
import si.konferenca.registration.acceptance.support.Settings;
import si.konferenca.registration.acceptance.support.StandardAcceptanceTest;
import tools.jackson.databind.JsonNode;

/** US-007 Organizer notification with the raw JSON attached, observed in Mailpit. */
class Us007OrganizerNotificationAcceptanceTest extends StandardAcceptanceTest {

  private static final int MESSAGES_PER_REGISTRATION = 1 + Settings.ORGANIZER_EMAILS.size();

  @Test
  @DisplayName("AC-007-01 every organizer address receives a notification with the submitted data")
  void ac007_01_everyOrganizerIsNotified() {
    registerAccepted(validExternal());
    mailpit.awaitMessages(MESSAGES_PER_REGISTRATION);

    for (String organizer : Settings.ORGANIZER_EMAILS) {
      List<JsonNode> received = messagesTo(organizer);
      assertThat(received).as("notifications to %s", organizer).hasSize(1);
      String text =
          mailpit.message(received.getFirst().path("ID").asString()).path("Text").asString();
      assertThat(text).contains("Ana", "Novak", "ana.novak@example.org", "Example d.o.o.");
    }
  }

  @Test
  @DisplayName("AC-007-01 a student notification contains the student's data")
  void ac007_01_studentDataIsInTheNotification() {
    registerAccepted(validStudent());
    mailpit.awaitMessages(MESSAGES_PER_REGISTRATION);

    List<JsonNode> received = messagesTo(Settings.ORGANIZER_EMAILS.getFirst());
    assertThat(received).hasSize(1);
    String text =
        mailpit.message(received.getFirst().path("ID").asString()).path("Text").asString();
    assertThat(text)
        .contains("Luka", "Horvat", "luka.horvat@student.example.org", "Univerza v Ljubljani")
        .contains("Computer Science", "63210001");
  }

  @Test
  @DisplayName("AC-007-02 the notification's JSON attachment equals the stored JSON copy")
  void ac007_02_attachmentEqualsTheJsonCopy() {
    UUID id = registerAccepted(validExternal());
    mailpit.awaitMessages(MESSAGES_PER_REGISTRATION);
    byte[] copy = copies.bytes(id);

    for (String organizer : Settings.ORGANIZER_EMAILS) {
      JsonNode message = mailpit.message(messagesTo(organizer).getFirst().path("ID").asString());
      List<JsonNode> jsonAttachments =
          message
              .path("Attachments")
              .valueStream()
              .filter(
                  a ->
                      a.path("ContentType").asString().contains("json")
                          || a.path("FileName").asString().endsWith(".json"))
              .toList();
      assertThat(jsonAttachments).as("JSON attachments to %s", organizer).hasSize(1);
      byte[] attachment =
          mailpit.part(
              message.path("ID").asString(), jsonAttachments.getFirst().path("PartID").asString());
      assertThat(attachment).isEqualTo(copy);
    }
  }

  private List<JsonNode> messagesTo(String address) {
    return mailpit.messages().stream()
        .filter(m -> Mailpit.recipients(m).contains(address))
        .toList();
  }
}
