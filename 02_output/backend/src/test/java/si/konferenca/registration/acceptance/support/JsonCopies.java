package si.konferenca.registration.acceptance.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import tools.jackson.databind.JsonNode;

/** The JSON copy directory the backend was started with. */
public final class JsonCopies {

  private final Path directory;

  public JsonCopies(Path directory) {
    this.directory = directory;
  }

  public Path directory() {
    return directory;
  }

  /** Names of the files in the directory (none if it does not exist). */
  public List<String> files() {
    if (!Files.isDirectory(directory)) {
      return List.of();
    }
    try (Stream<Path> files = Files.list(directory)) {
      return files.map(p -> p.getFileName().toString()).sorted().toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public Path fileOf(UUID id) {
    return directory.resolve(id + ".json");
  }

  public byte[] bytes(UUID id) {
    try {
      return Files.readAllBytes(fileOf(id));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public JsonNode json(UUID id) {
    return HttpApi.JSON.readTree(bytes(id));
  }

  public void clear() {
    if (!Files.isDirectory(directory)) {
      return;
    }
    try (Stream<Path> files = Files.list(directory)) {
      for (Path file : files.toList()) {
        Files.deleteIfExists(file);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
