package si.konferenca.registration.adapter.jsoncopy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import si.konferenca.registration.application.port.JsonCopyStore;
import si.konferenca.registration.config.AppProperties;
import si.konferenca.registration.domain.Registration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes {@code <id>.json} to the JSON copy directory: UTF-8, to a temporary file that is forced to
 * disk and then moved atomically (specification S4.6).
 */
@Component
public class FileJsonCopyStore implements JsonCopyStore {

  private static final Logger LOG = LoggerFactory.getLogger(FileJsonCopyStore.class);

  private final Path directory;
  private final JsonMapper json = JsonMapper.builder().build();

  public FileJsonCopyStore(AppProperties properties) {
    this.directory = properties.jsonCopyDir();
  }

  @Override
  public byte[] write(Registration registration) {
    byte[] bytes = json.writeValueAsBytes(RegistrationCopy.of(registration));
    Path temporary = directory.resolve(registration.id() + ".tmp");
    try {
      try (FileChannel channel =
          FileChannel.open(temporary, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        while (buffer.hasRemaining()) {
          channel.write(buffer);
        }
        channel.force(true);
      }
      Files.move(temporary, target(registration.id()), StandardCopyOption.ATOMIC_MOVE);
      return bytes;
    } catch (IOException e) {
      deleteQuietly(temporary);
      throw new UncheckedIOException("JSON copy not written for " + registration.id(), e);
    }
  }

  @Override
  public void delete(UUID registrationId) {
    deleteQuietly(target(registrationId));
    deleteQuietly(directory.resolve(registrationId + ".tmp"));
  }

  private Path target(UUID registrationId) {
    return directory.resolve(registrationId + ".json");
  }

  private static void deleteQuietly(Path file) {
    try {
      Files.deleteIfExists(file);
    } catch (IOException e) {
      LOG.warn("could not delete {}", file.getFileName());
    }
  }
}
