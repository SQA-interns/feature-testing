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
import si.konferenca.registration.domain.GivenConsent;
import si.konferenca.registration.domain.Participant;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;

class FileJsonCopyStoreTest {

  @TempDir Path temp;

  private static final Instant AT = Instant.parse("2026-10-20T10:15:30.123456Z");

  static Registration registration() {
    return new Registration(
        UUID.fromString("0a1b2c3d-0000-4000-8000-000000000001"),
        new Participant(
            RegistrationType.STUDENT, "Žan", "Kovač", "zan@uni.si", null, "Uni", "CS", "63"),
        List.of("ws", "meal"),
        List.of(new GivenConsent("privacy", AT)),
        AT);
  }

  @Test
  void writesTheContractDocumentAsCompactUtf8() throws IOException {
    FileJsonCopyStore store = new FileJsonCopyStore(temp.resolve("copies"));

    byte[] written = store.write(registration());

    Path file = temp.resolve("copies/0a1b2c3d-0000-4000-8000-000000000001.json");
    assertThat(Files.readAllBytes(file)).isEqualTo(written);
    assertThat(new String(written, StandardCharsets.UTF_8))
        .isEqualTo(
            "{\"id\":\"0a1b2c3d-0000-4000-8000-000000000001\",\"type\":\"STUDENT\",\"firstName\":\"Žan\","
                + "\"lastName\":\"Kovač\",\"email\":\"zan@uni.si\",\"organization\":null,"
                + "\"studyInstitution\":\"Uni\",\"studyProgramme\":\"CS\",\"studentId\":\"63\","
                + "\"options\":[\"ws\",\"meal\"],\"consents\":[{\"id\":\"privacy\","
                + "\"givenAt\":\"2026-10-20T10:15:30.123456Z\"}],"
                + "\"registeredAt\":\"2026-10-20T10:15:30.123456Z\"}");
    assertThat(Files.list(temp.resolve("copies"))).hasSize(1);
    assertThat(store.directory()).isEqualTo(temp.resolve("copies"));
  }

  @Test
  void writingTwiceReplacesTheCopy() {
    FileJsonCopyStore store = new FileJsonCopyStore(temp);

    store.write(registration());
    store.write(registration());

    assertThat(temp.resolve("0a1b2c3d-0000-4000-8000-000000000001.json")).exists();
  }

  @Test
  void unwritableDirectoryFailsAndLeavesNothing() throws IOException {
    Path blocker = Files.createFile(temp.resolve("blocker"));
    FileJsonCopyStore store = new FileJsonCopyStore(blocker.resolve("copies"));

    assertThatThrownBy(() -> store.write(registration()))
        .isInstanceOf(UncheckedIOException.class)
        .hasMessageContaining("0a1b2c3d-0000-4000-8000-000000000001");
    assertThat(Files.list(temp)).containsExactly(blocker);
  }

  @Test
  void deleteRemovesTheCopyAndToleratesMissingFiles() {
    FileJsonCopyStore store = new FileJsonCopyStore(temp);
    store.write(registration());
    UUID id = registration().id();

    store.delete(id);
    store.delete(id);

    assertThat(temp.resolve(id + ".json")).doesNotExist();
  }

  @Test
  void deleteNeverThrows() throws IOException {
    UUID id = registration().id();
    Path nonEmpty = Files.createDirectories(temp.resolve(id + ".json"));
    Files.createFile(nonEmpty.resolve("keep"));

    new FileJsonCopyStore(temp).delete(id);

    assertThat(nonEmpty).exists();
  }

  @Test
  void healthIsUpOnlyForAWritableDirectory() throws IOException {
    assertThat(new JsonCopyHealthIndicator(temp).health().getStatus().getCode()).isEqualTo("UP");
    Path file = Files.createFile(temp.resolve("file"));
    assertThat(new JsonCopyHealthIndicator(file).health().getStatus().getCode()).isEqualTo("DOWN");
    assertThat(new JsonCopyHealthIndicator(temp.resolve("missing")).health().getStatus().getCode())
        .isEqualTo("DOWN");
  }
}
