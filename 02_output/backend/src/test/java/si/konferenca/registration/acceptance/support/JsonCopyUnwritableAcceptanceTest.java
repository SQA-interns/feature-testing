package si.konferenca.registration.acceptance.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The backend configured with a JSON copy directory that cannot exist: its parent is a regular
 * file, so no copy can be written on any operating system.
 */
public abstract class JsonCopyUnwritableAcceptanceTest extends AcceptanceTestBase {

  protected static final Path JSON_COPY_DIR = blockedDirectory();

  @DynamicPropertySource
  static void settings(DynamicPropertyRegistry registry) {
    Settings.register(registry, Settings.defaults(JSON_COPY_DIR));
  }

  @Override
  protected Path jsonCopyDirectory() {
    return JSON_COPY_DIR;
  }

  private static Path blockedDirectory() {
    try {
      Path blocker = Files.createTempFile("acceptance-json-blocker", ".txt");
      return blocker.resolve("copies");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
