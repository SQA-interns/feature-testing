package si.konferenca.registration.acceptance.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Reads the JSON copy directory ({@code docs/02_contracts/registration-copy.schema.json}). */
public final class JsonCopies {

  private final Path directory;
  private final JsonMapper json = JsonMapper.builder().build();

  JsonCopies(Path directory) {
    this.directory = directory;
  }

  public Path file(String registrationId) {
    return directory.resolve(registrationId + ".json");
  }

  public byte[] bytes(String registrationId) {
    try {
      return Files.readAllBytes(file(registrationId));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public JsonNode read(String registrationId) {
    return json.readTree(new String(bytes(registrationId), StandardCharsets.UTF_8));
  }

  public JsonNode parse(byte[] content) {
    return json.readTree(new String(content, StandardCharsets.UTF_8));
  }

  /** Files in the directory whose content contains the value. */
  public List<Path> filesMentioning(String value) {
    if (!Files.isDirectory(directory)) {
      return List.of();
    }
    try (Stream<Path> files = Files.list(directory)) {
      return files.filter(Files::isRegularFile).filter(f -> contains(f, value)).toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static boolean contains(Path file, String value) {
    try {
      return Files.readString(file, StandardCharsets.UTF_8).contains(value);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
