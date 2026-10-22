package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ServerSocket;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.AcceptanceSupport;
import si.konferenca.registration.acceptance.support.RunningApp;

/** US-006 AC-006-03: the SMTP server cannot be reached. */
class Us006MailUnavailableAcceptanceTest extends AcceptanceSupport {

  private static RunningApp app;

  @BeforeAll
  static void startWithUnreachableMailServer() throws Exception {
    int closedPort;
    try (ServerSocket socket = new ServerSocket(0)) {
      closedPort = socket.getLocalPort();
    }
    app =
        RunningApp.start(
            Map.of(
                "spring.mail.host", "127.0.0.1", "spring.mail.port", String.valueOf(closedPort)));
  }

  @AfterAll
  static void stop() {
    app.close();
  }

  @Override
  protected String baseUrl() {
    return app.baseUrl();
  }

  @DisplayName("AC-006-03")
  @Test
  void ac_006_03_registrationIsAcceptedAndStoredWhenMailServerIsUnavailable() throws Exception {
    HttpResponse<byte[]> response = postRegistration(validExternal());

    assertThat(response.statusCode()).as(text(response)).isEqualTo(201);
    String id = json(response).path("id").asString();
    assertThat(registrationRow(id)).as("registration row").isNotEmpty();
    assertThat(jsonCopy(id)).as("JSON copy").exists();
  }
}
