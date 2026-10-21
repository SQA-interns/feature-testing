package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import si.konferenca.registration.acceptance.support.AcceptanceTest;
import si.konferenca.registration.acceptance.support.Database;
import si.konferenca.registration.acceptance.support.JsonCopies;
import si.konferenca.registration.acceptance.support.Submissions;
import si.konferenca.registration.acceptance.support.TestStack;

/** US-006: the SMTP port points to a closed local port, so every email fails. */
@AcceptanceTest
class MailUnavailableAcceptanceTest {

  @Autowired private MockMvc mvc;

  private final Database db = TestStack.database();
  private final JsonCopies copies = TestStack.jsonCopies();

  @DynamicPropertySource
  static void stack(DynamicPropertyRegistry registry) {
    TestStack.register(
        registry,
        Map.of("spring.mail.host", "127.0.0.1", "spring.mail.port", String.valueOf(closedPort())));
  }

  @Test
  void ac_006_03_registrationIsAcceptedAndStoredWhenMailServerIsUnavailable() throws Exception {
    Map<String, Object> body = Submissions.validExternal();

    String id =
        Submissions.id(
            mvc.perform(Submissions.post(body)).andExpect(status().isCreated()).andReturn());

    assertThat(db.registration(id)).containsEntry("email", (String) body.get("email"));
    assertThat(copies.file(id)).exists();
  }

  private static int closedPort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
