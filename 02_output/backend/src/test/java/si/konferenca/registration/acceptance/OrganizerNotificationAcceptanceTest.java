package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import si.konferenca.registration.acceptance.support.AcceptanceTest;
import si.konferenca.registration.acceptance.support.JsonCopies;
import si.konferenca.registration.acceptance.support.Mailpit;
import si.konferenca.registration.acceptance.support.Submissions;
import si.konferenca.registration.acceptance.support.TestStack;

/** US-007: every configured organizer address is notified, with the JSON copy attached. */
@AcceptanceTest
class OrganizerNotificationAcceptanceTest {

  @Autowired private MockMvc mvc;

  private final Mailpit mailpit = TestStack.mailpit();
  private final JsonCopies copies = TestStack.jsonCopies();

  @DynamicPropertySource
  static void stack(DynamicPropertyRegistry registry) {
    TestStack.register(registry);
  }

  @Test
  void ac_007_01_eachOrganizerAddressReceivesTheSubmittedRegistrationData() throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    String participant = (String) body.get("email");

    register(body);

    for (String organizer : TestStack.ORGANIZER_EMAILS) {
      List<Mailpit.Message> messages = mailpit.awaitMessagesTo(organizer, participant, 1);
      assertThat(messages).hasSize(1);
      assertThat(messages.get(0).text())
          .contains("Ana", "Novak", participant, "Example d.o.o.", "ws-ai", "ev-gala");
    }
  }

  @Test
  void ac_007_01_studentDataIsSentToOrganizers() throws Exception {
    Map<String, Object> body = Submissions.validStudent();
    String participant = (String) body.get("email");

    register(body);

    for (String organizer : TestStack.ORGANIZER_EMAILS) {
      List<Mailpit.Message> messages = mailpit.awaitMessagesTo(organizer, participant, 1);
      assertThat(messages.get(0).text())
          .contains(
              "Luka",
              "Kranjc",
              participant,
              "Univerza v Ljubljani",
              "Računalništvo in informatika",
              "63210001");
    }
  }

  @Test
  void ac_007_02_notificationHasJsonAttachmentEqualToTheStoredJsonCopy() throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    String participant = (String) body.get("email");

    String id = register(body);

    for (String organizer : TestStack.ORGANIZER_EMAILS) {
      Mailpit.Message mail = mailpit.awaitMessagesTo(organizer, participant, 1).get(0);
      List<Mailpit.Attachment> json =
          mail.attachments().stream()
              .filter(
                  a ->
                      a.contentType().startsWith("application/json")
                          || a.fileName().endsWith(".json"))
              .toList();
      assertThat(json).hasSize(1);
      byte[] attached = mailpit.attachment(mail, json.get(0));
      assertThat(copies.parse(attached)).isEqualTo(copies.read(id));
    }
  }

  private String register(Map<String, Object> body) throws Exception {
    return Submissions.id(
        mvc.perform(Submissions.post(body)).andExpect(status().isCreated()).andReturn());
  }
}
