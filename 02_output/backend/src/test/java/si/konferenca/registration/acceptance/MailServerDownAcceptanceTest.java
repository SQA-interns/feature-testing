package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.Submissions;

/** US-006, OQ-03: the mail server is unavailable (nothing listens on the SMTP port). */
class MailServerDownAcceptanceTest extends AcceptanceTestBase {

  @DynamicPropertySource
  static void smtpUnavailable(DynamicPropertyRegistry registry) {
    int closedPort;
    try (ServerSocket socket = new ServerSocket(0)) {
      closedPort = socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    registry.add("SMTP_HOST", () -> "127.0.0.1");
    registry.add("SMTP_PORT", () -> String.valueOf(closedPort));
  }

  @Test
  @DisplayName("AC-006-03 with the mail server unavailable the registration is accepted and stored")
  void ac_006_03_registrationIsAcceptedAndStoredWhenMailFails() {
    HttpResponse<String> response = postRegistration(Submissions.validExternal());

    assertThat(response.statusCode()).as("body: %s", response.body()).isEqualTo(201);
    String id = json(response).path("id").asString();
    assertThat(queryLong("SELECT count(*) FROM registration WHERE id = ?", UUID.fromString(id)))
        .isEqualTo(1);
    assertThat(JSON_COPY_DIR.resolve(id + ".json")).exists();
  }
}
