package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** US-007 Organizer notification (AC-007-01, 02). */
class Us007OrganizerNotificationAcceptanceTest extends AcceptanceTestBase {

  private static JsonNode notificationFor(String organizer, String participantEmail) {
    return MAIL.awaitMessageTo(
        organizer, m -> m.path("Text").asString().contains(participantEmail));
  }

  @DisplayName("AC-007-01")
  @Test
  void ac_007_01_eachOrganizerAddressReceivesTheSubmittedData() {
    ObjectNode body = options(validExternal(), "ws-ai", "other-tour");
    body.put("lastName", "Notifyexternal");
    String email = body.path("email").asString();

    register(body);

    for (String organizer : List.of(ORGANIZER_EMAIL_1, ORGANIZER_EMAIL_2)) {
      JsonNode message = notificationFor(organizer, email);
      assertThat(message).as("notification to " + organizer).isNotNull();
      assertThat(message.path("Text").asString())
          .contains("Ana", "Notifyexternal", email, "Example d.o.o.", "ws-ai", "other-tour");
    }
  }

  @DisplayName("AC-007-01")
  @Test
  void ac_007_01_notificationContainsStudentData() {
    ObjectNode body = options(validStudent(), "meal-lunch");
    String email = body.path("email").asString();

    register(body);

    JsonNode message = notificationFor(ORGANIZER_EMAIL_2, email);
    assertThat(message).as("notification to " + ORGANIZER_EMAIL_2).isNotNull();
    assertThat(message.path("Text").asString())
        .contains(
            "Luka",
            "Kranjc",
            email,
            "University of Ljubljana",
            "Computer Science",
            "63200001",
            "meal-lunch");
  }

  @DisplayName("AC-007-02")
  @Test
  void ac_007_02_notificationHasJsonAttachmentEqualToTheStoredJsonCopy() throws Exception {
    ObjectNode body = options(validExternal(), "ev-gala");
    String email = body.path("email").asString();

    String id = register(body);

    for (String organizer : List.of(ORGANIZER_EMAIL_1, ORGANIZER_EMAIL_2)) {
      JsonNode message = notificationFor(organizer, email);
      assertThat(message).as("notification to " + organizer).isNotNull();
      List<JsonNode> jsonAttachments = new ArrayList<>();
      message
          .path("Attachments")
          .forEach(
              a -> {
                if (a.path("FileName").asString().endsWith(".json")) {
                  jsonAttachments.add(a);
                }
              });
      assertThat(jsonAttachments).as("JSON attachments").hasSize(1);
      JsonNode attachment = jsonAttachments.get(0);
      assertThat(attachment.path("ContentType").asString()).startsWith("application/json");
      byte[] content =
          MAIL.part(message.path("ID").asString(), attachment.path("PartID").asString());
      assertThat(content).isEqualTo(Files.readAllBytes(jsonCopy(id)));
    }
  }
}
