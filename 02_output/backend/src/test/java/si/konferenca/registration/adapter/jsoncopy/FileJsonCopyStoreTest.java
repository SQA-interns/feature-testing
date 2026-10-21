package si.konferenca.registration.adapter.jsoncopy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

class FileJsonCopyStoreTest {

  @TempDir Path dir;

  private static Registration registration() {
    Instant at = Instant.parse("2026-10-21T11:30:00.123Z");
    return new Registration(
        UUID.fromString("0b6f4a8e-1c2d-4e5f-8a9b-0c1d2e3f4a5b"),
        RegistrationType.STUDENT,
        "Žiga",
        "Čeh",
        "ziga@example.org",
        null,
        "UL",
        "CS",
        "6321",
        List.of("ws-ai", "meal-lunch"),
        List.of(new GivenConsent("privacy", at)),
        at);
  }

  @Test
  void writesTheCopyNamedAfterTheIdAndReturnsItsBytes() throws IOException {
    Path copies = dir.resolve("nested/copies");
    FileJsonCopyStore store = new FileJsonCopyStore(copies);

    byte[] written = store.save(registration());

    Path file = copies.resolve("0b6f4a8e-1c2d-4e5f-8a9b-0c1d2e3f4a5b.json");
    assertThat(file).exists();
    assertThat(Files.readAllBytes(file)).isEqualTo(written);
    assertThat(new String(written, StandardCharsets.UTF_8))
        .isEqualTo(
            "{\"id\":\"0b6f4a8e-1c2d-4e5f-8a9b-0c1d2e3f4a5b\",\"type\":\"STUDENT\","
                + "\"firstName\":\"Žiga\",\"lastName\":\"Čeh\",\"email\":\"ziga@example.org\","
                + "\"organization\":null,\"studyInstitution\":\"UL\",\"studyProgramme\":\"CS\","
                + "\"studentId\":\"6321\",\"options\":[\"ws-ai\",\"meal-lunch\"],"
                + "\"consents\":[{\"id\":\"privacy\",\"givenAt\":\"2026-10-21T11:30:00.123Z\"}],"
                + "\"registeredAt\":\"2026-10-21T11:30:00.123Z\"}");
    try (Stream<Path> files = Files.list(copies)) {
      assertThat(files).containsExactly(file);
    }
  }

  @Test
  void discardRemovesTheCopyAndIgnoresAMissingOne() {
    FileJsonCopyStore store = new FileJsonCopyStore(dir);
    Registration r = registration();
    store.save(r);

    store.discard(r.id());

    assertThat(dir.resolve(r.id() + ".json")).doesNotExist();
    assertThatCode(() -> store.discard(r.id())).doesNotThrowAnyException();
  }

  @Test
  void discardThatCannotDeleteOnlyLogs() throws IOException {
    Registration r = registration();
    Path blocking = Files.createDirectories(dir.resolve(r.id() + ".json"));
    Files.writeString(blocking.resolve("inside.txt"), "x");

    assertThatCode(() -> new FileJsonCopyStore(dir).discard(r.id())).doesNotThrowAnyException();
    assertThat(blocking).exists();
  }

  @Test
  void failsWhenTheDirectoryCannotBeCreated() throws IOException {
    Path file = Files.writeString(dir.resolve("plain-file"), "x");

    assertThatThrownBy(() -> new FileJsonCopyStore(file.resolve("copies")).save(registration()))
        .isInstanceOf(UncheckedIOException.class)
        .hasMessage("JSON copy could not be written");
  }
}
