package si.konferenca.registration.adapter.jsoncopy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import si.konferenca.registration.application.JsonCopyStore;
import si.konferenca.registration.domain.Registration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Raw JSON copies on persistent storage: {@code <dir>/<id>.json}, UTF-8, the fields of
 * docs/02_contracts/registration-copy.schema.json (BR-07, AC-005-02). The file is written to a
 * temporary name in the same directory and then moved, so a copy is either complete or absent.
 */
public class FileJsonCopyStore implements JsonCopyStore {

  private static final Logger LOG = LoggerFactory.getLogger(FileJsonCopyStore.class);
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private final Path directory;

  public FileJsonCopyStore(Path directory) {
    this.directory = directory;
  }

  @Override
  public byte[] write(Registration registration) {
    byte[] content = MAPPER.writeValueAsBytes(JsonCopy.of(registration));
    Path target = file(registration.id());
    Path temporary = null;
    try {
      Files.createDirectories(directory);
      temporary = Files.createTempFile(directory, registration.id().toString(), ".tmp");
      Files.write(temporary, content);
      Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
      return content;
    } catch (IOException e) {
      throw new UncheckedIOException("JSON copy could not be written", e);
    } finally {
      deleteQuietly(temporary);
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
      LOG.warn("could not remove {} ({})", path.getFileName(), e.getClass().getSimpleName());
    }
  }

  private static String time(Instant instant) {
    return DateTimeFormatter.ISO_INSTANT.format(instant);
  }

  /** The document written to the file, in the field order of the schema. */
  record JsonCopy(
      String id,
      String type,
      String firstName,
      String lastName,
      String email,
      String organization,
      String studyInstitution,
      String studyProgramme,
      String studentId,
      List<String> options,
      List<Consent> consents,
      String registeredAt) {

    static JsonCopy of(Registration r) {
      return new JsonCopy(
          r.id().toString(),
          r.type().name(),
          r.firstName(),
          r.lastName(),
          r.email(),
          r.organization(),
          r.studyInstitution(),
          r.studyProgramme(),
          r.studentId(),
          r.options(),
          r.consents().stream().map(c -> new Consent(c.id(), time(c.givenAt()))).toList(),
          time(r.registeredAt()));
    }
  }

  record Consent(String id, String givenAt) {}
}
