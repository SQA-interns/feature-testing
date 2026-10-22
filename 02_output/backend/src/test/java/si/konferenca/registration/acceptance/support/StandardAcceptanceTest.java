package si.konferenca.registration.acceptance.support;

import java.nio.file.Path;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** The backend with the default acceptance settings (captcha test mode, Mailpit reachable). */
public abstract class StandardAcceptanceTest extends AcceptanceTestBase {

  protected static final Path JSON_COPY_DIR = Settings.newDirectory("acceptance-json-copies");

  @DynamicPropertySource
  static void settings(DynamicPropertyRegistry registry) {
    Settings.register(registry, Settings.defaults(JSON_COPY_DIR));
  }

  @Override
  protected Path jsonCopyDirectory() {
    return JSON_COPY_DIR;
  }
}
