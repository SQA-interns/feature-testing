package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Mailpit;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** US-006 Participant email confirmation (AC-006-01, 02; AC-006-03 in its own class). */
class Us006ParticipantEmailAcceptanceTest extends AcceptanceTestBase {

  private static final String INJECTED = "injected@evil.example";

  @DisplayName("AC-006-01")
  @Test
  void ac_006_01_oneEmailToParticipantNamesConferenceParticipantAndOptions() {
    ObjectNode body = options(validExternal(), "ws-ai", "ev-gala");
    String email = body.path("email").asString();

    register(body);

    List<JsonNode> messages = MAIL.awaitTo(email, 1);
    assertThat(messages).as("emails to " + email).hasSize(1);
    sleep(Duration.ofSeconds(1));
    assertThat(MAIL.messagesTo(email)).as("exactly one email to the participant").hasSize(1);
    JsonNode message = MAIL.message(messages.get(0).path("ID").asString());
    assertThat(Mailpit.recipients(message)).containsExactly(email.toLowerCase());
    String text = message.path("Text").asString();
    assertThat(text).contains(CONFERENCE_NAME, "Ana", "Novak", "AI workshop", "Gala evening");
  }

  @DisplayName("AC-006-01")
  @Test
  void ac_006_01_studentReceivesConfirmationWithOptions() {
    ObjectNode body = options(validStudent(), "meal-lunch", "other-tour");
    String email = body.path("email").asString();

    register(body);

    List<JsonNode> messages = MAIL.awaitTo(email, 1);
    assertThat(messages).hasSize(1);
    String text = MAIL.message(messages.get(0).path("ID").asString()).path("Text").asString();
    assertThat(text).contains(CONFERENCE_NAME, "Luka", "Kranjc", "Lunch", "City tour");
  }

  private static List<String> headerNames(JsonNode headers) {
    List<String> names = new ArrayList<>();
    headers.propertyNames().forEach(n -> names.add(n.toLowerCase()));
    return names;
  }

  @DisplayName("AC-006-02")
  @Test
  void ac_006_02_lineBreaksHeaderTextAndMarkupCannotInjectHeadersRecipientsOrMarkup() {
    ObjectNode body = validExternal();
    String email = body.path("email").asString();
    body.put("firstName", "Ana\r\nBcc: " + INJECTED + "\r\nX-Injected: first");
    body.put("lastName", "<b>Novak</b><script>alert(1)</script>");
    body.put("organization", "Org\nX-Injected: org\n\n<img src=x onerror=alert(2)>");

    String id = register(body);

    JsonNode message =
        MAIL.awaitMessageTo(email, m -> m.path("Text").asString().contains("<b>Novak</b>"));
    assertThat(message).as("participant email with the literal markup in its text").isNotNull();
    assertThat(Mailpit.recipients(message)).containsExactly(email.toLowerCase());
    assertThat(message.path("Cc")).isEmpty();
    assertThat(message.path("Bcc")).isEmpty();
    String html = message.path("HTML").asString();
    assertThat(html).doesNotContain("<b>Novak</b>", "<script>", "<img");
    assertThat(headerNames(MAIL.headers(message.path("ID").asString())))
        .doesNotContain("x-injected", "bcc");

    JsonNode organizerMessage =
        MAIL.awaitMessageTo(ORGANIZER_EMAIL_1, m -> m.path("Text").asString().contains(email));
    assertThat(organizerMessage).as("organizer notification").isNotNull();
    assertThat(organizerMessage.path("HTML").asString()).doesNotContain("<script>", "<img");
    assertThat(headerNames(MAIL.headers(organizerMessage.path("ID").asString())))
        .doesNotContain("x-injected", "bcc");

    assertThat(MAIL.messagesTo(INJECTED)).as("emails to the injected address").isEmpty();
    assertThat(id).isNotBlank();
  }
}
