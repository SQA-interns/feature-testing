package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.acceptance.support.Submissions.validExternal;
import static si.konferenca.registration.acceptance.support.Submissions.with;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.Mailpit;
import si.konferenca.registration.acceptance.support.Settings;
import si.konferenca.registration.acceptance.support.StandardAcceptanceTest;
import tools.jackson.databind.JsonNode;

/** US-006 Participant email confirmation, observed in Mailpit. */
class Us006ParticipantEmailAcceptanceTest extends StandardAcceptanceTest {

  private static final String PARTICIPANT = "ana.novak@example.org";
  private static final int MESSAGES_PER_REGISTRATION = 1 + Settings.ORGANIZER_EMAILS.size();

  @Test
  @DisplayName("AC-006-01 one email names the conference, the participant and the selected options")
  void ac006_01_participantReceivesOneConfirmation() {
    registerAccepted(with(validExternal(), "options", List.of("ws-ai", "ev-dinner")));

    List<JsonNode> toParticipant = messagesTo(PARTICIPANT);

    assertThat(toParticipant).hasSize(1);
    JsonNode message = mailpit.message(toParticipant.getFirst().path("ID").asString());
    String subjectAndText =
        message.path("Subject").asString() + "\n" + message.path("Text").asString();
    assertThat(subjectAndText).contains(Settings.CONFERENCE_NAME);
    assertThat(message.path("Text").asString())
        .contains("Ana", "Novak", "AI workshop", "Conference dinner");
  }

  @Test
  @DisplayName("AC-006-02 line breaks, header-like text and markup add no headers or recipients")
  void ac006_02_userInputCannotInjectHeadersOrMarkup() {
    Map<String, Object> submission =
        with(
            validExternal(),
            "firstName",
            "Ana\r\nBcc: intruder@evil.example",
            "lastName",
            "<b>Novak</b><script>alert(1)</script>",
            "organization",
            "Example\nSubject: hacked\nX-Injected: yes");
    registerAccepted(submission);

    mailpit.awaitMessages(MESSAGES_PER_REGISTRATION);
    List<JsonNode> all = mailpit.messages();
    assertThat(all).hasSize(MESSAGES_PER_REGISTRATION);
    for (JsonNode summary : all) {
      String id = summary.path("ID").asString();
      JsonNode message = mailpit.message(id);
      assertThat(Mailpit.recipients(message)).doesNotContain("intruder@evil.example");
      JsonNode headers = mailpit.headers(id);
      assertThat(headerNames(headers)).doesNotContain("x-injected", "bcc");
      assertThat(headers.path("Subject").toString()).doesNotContain("hacked");
      String html = message.path("HTML").asString();
      assertThat(html)
          .as("markup is never sent as HTML")
          .doesNotContain("<b>Novak</b>", "<script>");
    }

    JsonNode confirmation =
        mailpit.message(messagesTo(PARTICIPANT).getFirst().path("ID").asString());
    assertThat(Mailpit.recipients(confirmation)).containsExactly(PARTICIPANT);
    assertThat(confirmation.path("Text").asString())
        .as("markup appears as literal text")
        .contains("<b>Novak</b><script>alert(1)</script>");
  }

  private List<JsonNode> messagesTo(String address) {
    return mailpit.awaitMessages(MESSAGES_PER_REGISTRATION).stream()
        .filter(m -> Mailpit.recipients(m).contains(address))
        .toList();
  }

  private static List<String> headerNames(JsonNode headers) {
    return headers.propertyNames().stream().map(String::toLowerCase).toList();
  }
}
