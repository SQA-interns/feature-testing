package si.konferenca.registration.adapter.jsoncopy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import si.konferenca.registration.application.JsonCopyStore;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes {@code <directory>/<id>.json} ({@code docs/02_contracts/registration-copy.schema.json}):
 * to a temporary file first, synced, then renamed, so a copy is either complete or absent.
 */
public class FileJsonCopyStore implements JsonCopyStore {

  private static final Logger LOG = LoggerFactory.getLogger(FileJsonCopyStore.class);
  private static final JsonMapper JSON =
      JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

  private final Path directory;

  public FileJsonCopyStore(Path directory) {
    this.directory = directory;
    try {
      Files.createDirectories(directory);
    } catch (IOException e) {
      LOG.warn("JSON copy directory cannot be created; registrations will fail until it can");
    }
  }

  public Path directory() {
    return directory;
  }

  @Override
  public byte[] write(Registration registration) {
    byte[] bytes = JSON.writeValueAsBytes(document(registration));
    Path target = directory.resolve(registration.id() + ".json");
    Path temporary = directory.resolve(registration.id() + ".json.tmp");
    try {
      Files.createDirectories(directory);
      try (FileChannel channel =
          FileChannel.open(
              temporary,
              StandardOpenOption.CREATE,
              StandardOpenOption.TRUNCATE_EXISTING,
              StandardOpenOption.WRITE)) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        while (buffer.hasRemaining()) {
          channel.write(buffer);
        }
        channel.force(true);
      }
      try {
        Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
      }
      return bytes;
    } catch (IOException e) {
      deleteQuietly(temporary);
      throw new UncheckedIOException("JSON copy of registration " + registration.id(), e);
    }
  }

  @Override
  public void delete(UUID registrationId) {
    deleteQuietly(directory.resolve(registrationId + ".json"));
    deleteQuietly(directory.resolve(registrationId + ".json.tmp"));
  }

  private static void deleteQuietly(Path file) {
    try {
      Files.deleteIfExists(file);
    } catch (IOException e) {
      LOG.warn("Could not delete {}", file.getFileName());
    }
  }

  static Map<String, Object> document(Registration registration) {
    Participant p = registration.participant();
    Map<String, Object> doc = new LinkedHashMap<>();
    doc.put("id", registration.id().toString());
    doc.put("type", p.type().name());
    doc.put("firstName", p.firstName());
    doc.put("lastName", p.lastName());
    doc.put("email", p.email());
    doc.put("organization", p.organization());
    doc.put("studyInstitution", p.studyInstitution());
    doc.put("studyProgramme", p.studyProgramme());
    doc.put("studentId", p.studentId());
    doc.put("options", registration.options());
    List<Map<String, Object>> consents =
        registration.consents().stream()
            .map(
                c -> {
                  Map<String, Object> consent = new LinkedHashMap<>();
                  consent.put("id", c.id());
                  consent.put("givenAt", c.givenAt().toString());
                  return consent;
                })
            .toList();
    doc.put("consents", consents);
    doc.put("registeredAt", registration.registeredAt().toString());
    return doc;
  }
}
