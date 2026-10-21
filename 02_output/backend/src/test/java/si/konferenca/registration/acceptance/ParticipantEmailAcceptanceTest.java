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
import si.konferenca.registration.acceptance.support.Mailpit;
import si.konferenca.registration.acceptance.support.Submissions;
import si.konferenca.registration.acceptance.support.TestStack;

/** US-006: the participant receives one confirmation email, generated safely. */
@AcceptanceTest
class ParticipantEmailAcceptanceTest {

  private static final String INJECTED = "injected@example.org";

  @Autowired private MockMvc mvc;

  private final Mailpit mailpit = TestStack.mailpit();

  @DynamicPropertySource
  static void stack(DynamicPropertyRegistry registry) {
    TestStack.register(registry);
  }

  @Test
  void ac_006_01_participantReceivesOneEmailNamingConferenceParticipantAndOptions()
      throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    String address = (String) body.get("email");

    mvc.perform(Submissions.post(body)).andExpect(status().isCreated());

    mailpit.awaitMessagesTo(address, "", 1);
    Thread.sleep(500);
    List<Mailpit.Message> messages = mailpit.messagesTo(address);
    assertThat(messages).hasSize(1);
    Mailpit.Message mail = messages.get(0);
    assertThat(mail.to()).containsExactly(address);
    assertThat(mail.subject() + "\n" + mail.text()).contains(TestStack.CONFERENCE_NAME);
    assertThat(mail.text()).contains("Ana", "Novak", "AI workshop", "Gala evening");
  }

  @Test
  void ac_006_02_lineBreaksHeaderTextAndMarkupCannotInjectHeadersRecipientsOrHtml()
      throws Exception {
    Map<String, Object> body = Submissions.validExternal();
    String address = (String) body.get("email");
    body.put("firstName", "Ana\r\nBcc: " + INJECTED);
    body.put("lastName", "<b>Novak</b><script>alert(1)</script>");
    body.put("organization", "Example\nSubject: hacked\nTo: " + INJECTED);

    mvc.perform(Submissions.post(body)).andExpect(status().isCreated());

    mailpit.awaitMessagesTo(address, "", 1);
    Thread.sleep(500);
    List<Mailpit.Message> messages = mailpit.messagesTo(address);
    assertThat(messages).hasSize(1);
    Mailpit.Message mail = messages.get(0);
    assertThat(mail.to()).containsExactly(address);
    assertThat(mail.cc()).isEmpty();
    assertThat(mail.bcc()).isEmpty();
    assertThat(mail.headers()).doesNotContainKeys("Bcc", "Cc");
    assertThat(mail.headers().get("To")).hasSize(1);
    assertThat(mail.headers().get("Subject")).hasSize(1);
    assertThat(mail.subject()).doesNotContain("hacked");
    assertThat(mail.html()).isEmpty();
    assertThat(mail.text()).contains("<b>Novak</b><script>alert(1)</script>");
    assertThat(mailpit.messagesTo(INJECTED)).isEmpty();
  }
}
