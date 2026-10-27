package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
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

/** US-006 Participant email confirmation, observed in Mailpit. */
class ParticipantEmailAcceptanceTest extends AcceptanceTestBase {

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry registry) {
    register(registry, Map.of());
  }

  private List<Mailpit.Message> awaitMailTo(String address) {
    Awaitility.await()
        .atMost(Duration.ofSeconds(15))
        .pollInterval(Duration.ofMillis(250))
        .until(() -> !mailpit.messagesTo(address).isEmpty());
    return mailpit.messagesTo(address);
  }

  @Test
  @DisplayName("AC-006-01 one email to the participant names the conference, participant, options")
  void ac_006_01_participantReceivesOneConfirmationEmail() {
    Map<String, Object> body = Submissions.validExternal();

    HttpResponse<String> response = postRegistration(body);

    assertThat(response.statusCode()).as("body: %s", response.body()).isEqualTo(201);
    List<Mailpit.Message> mails = awaitMailTo("ana.novak@example.org");
    assertThat(mails).hasSize(1);
    Mailpit.Message mail = mails.get(0);
    assertThat(mail.from()).isEqualTo(MAIL_FROM);
    assertThat(mail.subject() + "\n" + mail.text()).contains(CONFERENCE_NAME);
    assertThat(mail.text())
        .contains("Ana")
        .contains("Novak")
        .contains("Workshop: Testing in practice")
        .contains("Conference dinner");
  }

  @Test
  @DisplayName("AC-006-02 line breaks, header-like text and markup cannot inject headers or HTML")
  void ac_006_02_userInputCannotInjectHeadersRecipientsOrMarkup() {
    Map<String, Object> body = Submissions.validExternal();
    body.put("firstName", "Ana\r\nBcc: eve@example.org");
    body.put("lastName", "<b>Novak</b>\nCc: mallory@example.org");
    body.put("organization", "Institute\r\n\r\n<script>alert(1)</script>\nTo: trudy@example.org");

    HttpResponse<String> response = postRegistration(body);

    assertThat(response.statusCode()).as("body: %s", response.body()).isEqualTo(201);
    List<Mailpit.Message> mails = awaitMailTo("ana.novak@example.org");
    assertThat(mails).hasSize(1);
    Mailpit.Message mail = mails.get(0);
    assertThat(mail.to()).containsExactly("ana.novak@example.org");
    assertThat(mail.cc()).isEmpty();
    assertThat(mail.bcc()).isEmpty();
    assertThat(mail.replyTo()).isEmpty();
    assertThat(mailpit.headers(mail.id()).keySet())
        .as("header names")
        .noneMatch(h -> h.equalsIgnoreCase("Bcc") || h.equalsIgnoreCase("Cc"));
    assertThat(mail.html()).as("no HTML part").isEmpty();
    assertThat(mail.text()).contains("<b>Novak</b>");

    for (String injected : List.of("eve@example.org", "mallory@example.org", "trudy@example.org")) {
      assertThat(mailpit.messagesTo(injected)).as("mail to %s", injected).isEmpty();
    }
    assertThat(mailpit.messages())
        .allSatisfy(
            m ->
                assertThat(m.to())
                    .isSubsetOf(
                        java.util.stream.Stream.concat(
                                java.util.stream.Stream.of("ana.novak@example.org"),
                                ORGANIZER_EMAILS.stream())
                            .toList()));
  }
}
