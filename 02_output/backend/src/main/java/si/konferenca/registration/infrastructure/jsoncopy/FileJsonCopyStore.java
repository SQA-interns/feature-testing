package si.konferenca.registration.infrastructure.jsoncopy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import si.konferenca.registration.application.JsonCopyStore;
import si.konferenca.registration.application.StorageException;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.infrastructure.config.AppSettings;

/**
 * Writes {@code <id>.json} to the JSON copy directory: first to a temporary file in the same
 * directory, then an atomic move, so a copy is never seen half written (BR-07).
 */
@Component
public class FileJsonCopyStore implements JsonCopyStore {

  private static final Logger LOG = LoggerFactory.getLogger(FileJsonCopyStore.class);

  private final Path directory;

  public FileJsonCopyStore(AppSettings settings) {
    this.directory = Path.of(settings.jsonCopyDir());
    try {
      Files.createDirectories(directory);
    } catch (IOException e) {
      LOG.warn("JSON copy directory cannot be created; registrations will fail until it exists");
    }
  }

  @Override
  public byte[] write(Registration registration) {
    byte[] bytes = RegistrationJson.toBytes(registration);
    Path temporary = null;
    try {
      temporary = Files.createTempFile(directory, registration.id() + "-", ".tmp");
      Files.write(temporary, bytes);
      Files.move(temporary, file(registration.id()), StandardCopyOption.ATOMIC_MOVE);
      return bytes;
    } catch (IOException e) {
      deleteQuietly(temporary);
      throw new StorageException("JSON copy of registration " + registration.id() + " failed", e);
    }
  }

  @Override
  public void delete(UUID id) {
    deleteQuietly(file(id));
  }

  private Path file(UUID id) {
    return directory.resolve(id + ".json");
  }

  private static void deleteQuietly(Path path) {
    if (path == null) {
      return;
    }
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      LOG.warn("Could not delete {}", path.getFileName());
    }
  }
}
