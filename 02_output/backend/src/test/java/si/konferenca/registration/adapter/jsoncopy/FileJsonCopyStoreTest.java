package si.konferenca.registration.adapter.jsoncopy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import si.konferenca.registration.config.AppProperties;
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.domain.SelectedOption;

class FileJsonCopyStoreTest {

  private static final UUID ID = UUID.fromString("0b6a8f3e-1c2d-4e5f-8a9b-0c1d2e3f4a5b");
  private static final Instant AT = Instant.parse("2026-10-27T10:00:00.120Z");

  @TempDir Path dir;

  private FileJsonCopyStore store(Path directory) {
    return new FileJsonCopyStore(
        new AppProperties("test", null, directory, null, null, null, null, null, 0));
  }

  private static Registration registration() {
    return new Registration(
        ID,
        RegistrationType.EXTERNAL,
        AT,
        new Participant("Črt", "Žagar", "crt@example.org", "Inštitut", null, null, null),
        List.of(new SelectedOption("ws-a", "Delavnica", OptionCategory.WORKSHOP)),
        List.of(new GivenConsent("privacy", "Strinjam se", AT)));
  }

  @Test
  void writesTheCopyAtomicallyAndReturnsItsBytes() throws IOException {
    byte[] bytes = store(dir).write(registration());

    Path file = dir.resolve(ID + ".json");
    assertThat(Files.readAllBytes(file)).isEqualTo(bytes);
    assertThat(new String(bytes, StandardCharsets.UTF_8))
        .isEqualTo(
            "{\"schemaVersion\":1,\"id\":\""
                + ID
                + "\",\"type\":\"EXTERNAL\","
                + "\"registeredAt\":\"2026-10-27T10:00:00.120Z\","
                + "\"participant\":{\"firstName\":\"Črt\",\"lastName\":\"Žagar\","
                + "\"email\":\"crt@example.org\",\"organization\":\"Inštitut\"},"
                + "\"options\":[{\"id\":\"ws-a\",\"name\":\"Delavnica\",\"category\":\"WORKSHOP\"}],"
                + "\"consents\":[{\"id\":\"privacy\",\"text\":\"Strinjam se\","
                + "\"givenAt\":\"2026-10-27T10:00:00.120Z\"}]}");
    try (var files = Files.list(dir)) {
      assertThat(files).containsExactly(file);
    }
  }

  @Test
  void failedWriteRemovesTheTemporaryFile() throws IOException {
    Path missing = dir.resolve("missing");

    assertThatThrownBy(() -> store(missing).write(registration()))
        .isInstanceOf(UncheckedIOException.class)
        .hasMessage("JSON copy not written for " + ID);

    Path blocked = Files.createDirectory(dir.resolve("blocked"));
    Files.createDirectory(blocked.resolve(ID + ".json"));
    Files.writeString(blocked.resolve(ID + ".json").resolve("x"), "x");
    assertThatThrownBy(() -> store(blocked).write(registration()))
        .isInstanceOf(UncheckedIOException.class);
    assertThat(blocked.resolve(ID + ".tmp")).doesNotExist();
  }

  @Test
  void deleteRemovesTheCopyAndAnyTemporaryFile() throws IOException {
    Files.writeString(dir.resolve(ID + ".json"), "{}");
    Files.writeString(dir.resolve(ID + ".tmp"), "{");
    Files.writeString(dir.resolve("other.json"), "{}");

    store(dir).delete(ID);
    store(dir).delete(ID);

    try (var files = Files.list(dir)) {
      assertThat(files).containsExactly(dir.resolve("other.json"));
    }
  }

  @Test
  void deleteOfANonEmptyDirectoryOnlyLogs() throws IOException {
    Path occupied = dir.resolve(ID + ".json");
    Files.createDirectory(occupied);
    Files.writeString(occupied.resolve("x"), "x");

    store(dir).delete(ID);

    assertThat(occupied).exists();
  }

  @Test
  void studentCopyLeavesOutTheOrganization() {
    Registration student =
        new Registration(
            ID,
            RegistrationType.STUDENT,
            AT,
            new Participant("A", "B", "a@b.si", null, "U", "P", "1"),
            List.of(),
            List.of());

    RegistrationCopy copy = RegistrationCopy.of(student);

    assertThat(copy.participant())
        .isEqualTo(new RegistrationCopy.Participant("A", "B", "a@b.si", null, "U", "P", "1"));
    assertThat(copy.type()).isEqualTo("STUDENT");
    assertThat(copy.options()).isEmpty();
  }
}
