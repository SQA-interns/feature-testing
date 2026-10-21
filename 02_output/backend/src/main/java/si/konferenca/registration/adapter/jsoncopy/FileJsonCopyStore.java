package si.konferenca.registration.adapter.jsoncopy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import si.konferenca.registration.application.JsonCopyStore;
import si.konferenca.registration.domain.Registration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes {@code <id>.json} ({@code registration-copy.schema.json}) to the copy directory: first a
 * temporary file in the same directory, then an atomic move (BR-07).
 */
public class FileJsonCopyStore implements JsonCopyStore {

  private static final Logger LOG = LoggerFactory.getLogger(FileJsonCopyStore.class);

  private final Path directory;
  private final JsonMapper json = JsonMapper.builder().build();

  public FileJsonCopyStore(Path directory) {
    this.directory = directory;
  }

  @Override
  public byte[] save(Registration registration) {
    byte[] content = json.writeValueAsBytes(document(registration));
    try {
      Files.createDirectories(directory);
      Path temporary = Files.createTempFile(directory, registration.id().toString(), ".tmp");
      try {
        Files.write(temporary, content);
        Files.move(temporary, file(registration.id()), StandardCopyOption.ATOMIC_MOVE);
      } finally {
        Files.deleteIfExists(temporary);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("JSON copy could not be written", e);
    }
    return content;
  }

  @Override
  public void discard(UUID registrationId) {
    try {
      Files.deleteIfExists(file(registrationId));
    } catch (IOException e) {
      LOG.warn("JSON copy of registration {} could not be removed", registrationId);
    }
  }

  private Path file(UUID registrationId) {
    return directory.resolve(registrationId + ".json");
  }

  private static Map<String, Object> document(Registration r) {
    Map<String, Object> d = new LinkedHashMap<>();
    d.put("id", r.id().toString());
    d.put("type", r.type().name());
    d.put("firstName", r.firstName());
    d.put("lastName", r.lastName());
    d.put("email", r.email());
    d.put("organization", r.organization());
    d.put("studyInstitution", r.studyInstitution());
    d.put("studyProgramme", r.studyProgramme());
    d.put("studentId", r.studentId());
    d.put("options", r.options());
    List<Map<String, String>> consents =
        r.consents().stream()
            .map(c -> Map.of("id", c.id(), "givenAt", c.givenAt().toString()))
            .toList();
    d.put("consents", consents);
    d.put("registeredAt", r.registeredAt().toString());
    return d;
  }
}
