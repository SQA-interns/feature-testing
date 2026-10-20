package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** US-007 organizer notification: AC-007-01, AC-007-02 (Mailpit as the SMTP server). */
class OrganizerNotificationAcceptanceTest extends DefaultContextAcceptanceTest {

  @Test
  @DisplayName("AC-007-01 each organizer address receives the submitted registration data")
  void ac_007_01_eachOrganizerReceivesTheRegistrationData() throws Exception {
    String id = createdId(postRegistration(validExternal()));
    List<JsonNode> all = awaitMessages(1 + ORGANIZER_EMAILS.size());

    for (String organizer : ORGANIZER_EMAILS) {
      List<JsonNode> toOrganizer = messagesTo(all, organizer);
      assertThat(toOrganizer).as("notifications to %s", organizer).hasSize(1);
      String text = message(toOrganizer.get(0)).path("Text").asString();
      assertThat(text)
          .contains(id)
          .contains("EXTERNAL")
          .contains("Ana")
          .contains("Novak")
          .contains("ana.novak@example.org")
          .contains("Example d.o.o.")
          .contains("ws-ai")
          .contains("ev-gala")
          .contains("privacy");
    }
  }

  @Test
  @DisplayName("AC-007-01 the notification of a student registration contains the student fields")
  void ac_007_01_studentNotificationContainsStudentFields() throws Exception {
    String id = createdId(postRegistration(validStudent()));
    List<JsonNode> all = awaitMessages(1 + ORGANIZER_EMAILS.size());

    for (String organizer : ORGANIZER_EMAILS) {
      List<JsonNode> toOrganizer = messagesTo(all, organizer);
      assertThat(toOrganizer).hasSize(1);
      assertThat(message(toOrganizer.get(0)).path("Text").asString())
          .contains(id)
          .contains("STUDENT")
          .contains("Luka")
          .contains("Horvat")
          .contains("luka.horvat@student.example.org")
          .contains("University of Ljubljana")
          .contains("Computer Science")
          .contains("63210001")
          .contains("ev-student");
    }
  }

  @Test
  @DisplayName("AC-007-02 the notification has a JSON attachment equal to the stored JSON copy")
  void ac_007_02_attachmentEqualsStoredJsonCopy() throws Exception {
    String id = createdId(postRegistration(validExternal()));
    List<JsonNode> all = awaitMessages(1 + ORGANIZER_EMAILS.size());
    byte[] stored = Files.readAllBytes(jsonCopy(id));

    for (String organizer : ORGANIZER_EMAILS) {
      List<JsonNode> toOrganizer = messagesTo(all, organizer);
      assertThat(toOrganizer).hasSize(1);
      JsonNode mail = message(toOrganizer.get(0));
      List<JsonNode> jsonAttachments =
          mail.path("Attachments")
              .valueStream()
              .filter(a -> a.path("ContentType").asString().startsWith("application/json"))
              .toList();
      assertThat(jsonAttachments).as("JSON attachments to %s", organizer).hasSize(1);
      assertThat(jsonAttachments.get(0).path("FileName").asString()).endsWith(".json");
      assertThat(attachment(mail, jsonAttachments.get(0))).isEqualTo(stored);
    }
  }
}
