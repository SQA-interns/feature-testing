package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** US-006 participant email: AC-006-01, AC-006-02 (Mailpit as the SMTP server). */
class ParticipantEmailAcceptanceTest extends DefaultContextAcceptanceTest {

  static final String PARTICIPANT = "ana.novak@example.org";

  @Test
  @DisplayName("AC-006-01 one email names the conference, the participant and the options")
  void ac_006_01_participantReceivesOneConfirmation() throws Exception {
    createdId(postRegistration(validExternal()));

    List<JsonNode> toParticipant =
        messagesTo(awaitMessages(1 + ORGANIZER_EMAILS.size()), PARTICIPANT);

    assertThat(toParticipant).hasSize(1);
    JsonNode mail = message(toParticipant.get(0));
    assertThat(recipients(mail)).containsExactly(PARTICIPANT);
    String subjectAndText = mail.path("Subject").asString() + "\n" + mail.path("Text").asString();
    assertThat(subjectAndText).contains(CONFERENCE_NAME);
    assertThat(mail.path("Text").asString())
        .contains("Ana")
        .contains("Novak")
        .contains("AI workshop")
        .contains("Gala dinner");
  }

  @Test
  @DisplayName("AC-006-01 the confirmation of a student names the student and the options")
  void ac_006_01_studentReceivesOneConfirmation() throws Exception {
    createdId(postRegistration(validStudent()));

    List<JsonNode> toStudent =
        messagesTo(awaitMessages(1 + ORGANIZER_EMAILS.size()), "luka.horvat@student.example.org");

    assertThat(toStudent).hasSize(1);
    JsonNode mail = message(toStudent.get(0));
    assertThat(mail.path("Text").asString())
        .contains("Luka")
        .contains("Horvat")
        .contains("AI workshop")
        .contains("Student meetup");
  }

  @Test
  @DisplayName("AC-006-02 line breaks, header-like text and markup inject no header or recipient")
  void ac_006_02_userInputCannotInjectHeadersOrMarkup() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("firstName", "Ana\r\nBcc: injected@evil.example\r\nX-Injected: yes");
    body.put("lastName", "<b>Novak</b><script>alert(1)</script>");
    body.put("organization", "Org\nSubject: hacked\nCc: other@evil.example");

    createdId(postRegistration(body));
    List<JsonNode> all = awaitMessages(1 + ORGANIZER_EMAILS.size());

    List<String> allowed = new ArrayList<>(ORGANIZER_EMAILS);
    allowed.add(PARTICIPANT);
    for (JsonNode summary : all) {
      JsonNode mail = message(summary);
      assertThat(allowed).containsAll(recipients(mail));
      assertThat(mail.path("Subject").asString()).doesNotContain("hacked").doesNotContain("Bcc");
      JsonNode headers = headers(summary);
      assertThat(headers.has("X-Injected")).isFalse();
      for (var entry : headers.properties()) {
        assertThat(entry.getValue().toString())
            .as("header %s", entry.getKey())
            .doesNotContain("evil.example")
            .doesNotContain("hacked");
      }
      String html = mail.path("HTML").asString();
      assertThat(html).doesNotContain("<b>Novak</b>").doesNotContain("<script>");
    }
    assertThat(all.stream().flatMap(m -> recipients(m).stream()))
        .noneMatch(address -> address.contains("evil.example"));

    List<JsonNode> toParticipant = messagesTo(all, PARTICIPANT);
    assertThat(toParticipant).hasSize(1);
    JsonNode confirmation = message(toParticipant.get(0));
    assertThat(confirmation.path("Text").asString()).contains("<b>Novak</b>");
  }
}
