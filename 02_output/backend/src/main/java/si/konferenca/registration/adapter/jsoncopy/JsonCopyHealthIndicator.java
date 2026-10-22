package si.konferenca.registration.adapter.jsoncopy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/** Ready only while the JSON copy directory exists and is writable (ES-09, NFR-04). */
public class JsonCopyHealthIndicator implements HealthIndicator {

  private final Path directory;

  public JsonCopyHealthIndicator(Path directory) {
    this.directory = directory;
  }

  @Override
  public Health health() {
    boolean writable = Files.isDirectory(directory) && Files.isWritable(directory);
    return writable ? Health.up().build() : Health.down().build();
  }
}
