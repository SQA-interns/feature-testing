package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** AC-006-03: with no SMTP server listening, a valid registration is still accepted and stored. */
class MailServerDownAcceptanceTest extends AcceptanceTestSupport {

  static final int CLOSED_PORT = freePort();

  static int freePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @DynamicPropertySource
  static void settings(DynamicPropertyRegistry registry) {
    registerSettings(
        registry, Map.of("SMTP_HOST", "127.0.0.1", "SMTP_PORT", String.valueOf(CLOSED_PORT)));
  }

  @Test
  @DisplayName("AC-006-03 the registration is accepted (201) and stored when mail is unavailable")
  void ac_006_03_registrationIsAcceptedWhenMailServerIsDown() throws Exception {
    String id = createdId(postRegistration(validExternal()));

    assertThat(registrationCount()).isEqualTo(1);
    assertThat(registrationRow(id).get("email")).isEqualTo("ana.novak@example.org");
    assertThat(jsonCopy(id)).isRegularFile();
  }

  @Test
  @DisplayName("AC-006-03 a student registration is accepted and stored when mail is unavailable")
  void ac_006_03_studentRegistrationIsAcceptedWhenMailServerIsDown() throws Exception {
    String id = createdId(postRegistration(validStudent()));

    assertThat(registrationRow(id).get("type")).isEqualTo("STUDENT");
    assertThat(jsonCopy(id)).isRegularFile();
  }
}
