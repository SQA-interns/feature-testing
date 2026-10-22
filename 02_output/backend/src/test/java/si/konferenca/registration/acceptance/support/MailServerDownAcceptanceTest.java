package si.konferenca.registration.acceptance.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** The backend configured with an SMTP port on which nothing listens. */
public abstract class MailServerDownAcceptanceTest extends AcceptanceTestBase {

  protected static final Path JSON_COPY_DIR = Settings.newDirectory("acceptance-mail-down");

  @DynamicPropertySource
  static void settings(DynamicPropertyRegistry registry) {
    Map<String, String> values = Settings.defaults(JSON_COPY_DIR);
    values.put("SMTP_HOST", "127.0.0.1");
    values.put("SMTP_PORT", String.valueOf(closedPort()));
    Settings.register(registry, values);
  }

  @Override
  protected Path jsonCopyDirectory() {
    return JSON_COPY_DIR;
  }

  private static int closedPort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
