package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Mailpit;
import si.konferenca.registration.acceptance.support.Submissions;

/** US-007 Organizer notification, observed in Mailpit. */
class OrganizerNotificationAcceptanceTest extends AcceptanceTestBase {

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry registry) {
    register(registry, Map.of());
  }

  private Mailpit.Message awaitSingleMailTo(String address) {
    Awaitility.await()
        .atMost(Duration.ofSeconds(15))
        .pollInterval(Duration.ofMillis(250))
        .until(() -> !mailpit.messagesTo(address).isEmpty());
    List<Mailpit.Message> mails = mailpit.messagesTo(address);
    assertThat(mails).as("notifications to %s", address).hasSize(1);
    return mails.get(0);
  }

  @Test
  @DisplayName("AC-007-01 each organizer address receives the submitted registration data")
  void ac_007_01_eachOrganizerReceivesTheRegistrationData() {
    Map<String, Object> body = Submissions.validStudent();

    HttpResponse<String> response = postRegistration(body);

    assertThat(response.statusCode()).as("body: %s", response.body()).isEqualTo(201);
    for (String organizer : ORGANIZER_EMAILS) {
      Mailpit.Message mail = awaitSingleMailTo(organizer);
      assertThat(mail.text())
          .contains("Luka")
          .contains("Kranjc")
          .contains("luka.kranjc@student.example.org")
          .contains("University of Ljubljana")
          .contains("Computer Science")
          .contains("63200001")
          .contains("Workshop: Testing in practice")
          .contains("Lunch (vegetarian)");
    }
  }

  @Test
  @DisplayName("AC-007-02 the notification attaches JSON equal to the stored JSON copy")
  void ac_007_02_notificationAttachmentEqualsStoredJsonCopy() throws Exception {
    HttpResponse<String> response = postRegistration(Submissions.validExternal());

    assertThat(response.statusCode()).as("body: %s", response.body()).isEqualTo(201);
    String id = json(response).path("id").asString();
    byte[] stored = Files.readAllBytes(JSON_COPY_DIR.resolve(id + ".json"));
    for (String organizer : ORGANIZER_EMAILS) {
      Mailpit.Message mail = awaitSingleMailTo(organizer);
      List<Mailpit.Attachment> json =
          mail.attachments().stream().filter(a -> a.fileName().endsWith(".json")).toList();
      assertThat(json).as("JSON attachments").hasSize(1);
      assertThat(json.get(0).contentType()).startsWith("application/json");
      assertThat(mailpit.attachment(mail.id(), json.get(0).partId())).isEqualTo(stored);
    }
  }
}
