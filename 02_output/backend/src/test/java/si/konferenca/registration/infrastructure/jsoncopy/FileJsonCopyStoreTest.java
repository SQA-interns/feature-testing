package si.konferenca.registration.infrastructure.jsoncopy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import si.konferenca.registration.application.StorageException;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationType;
import si.konferenca.registration.infrastructure.config.AppSettings;

class FileJsonCopyStoreTest {

  private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-00000000000c");
  private static final Instant AT = Instant.parse("2026-10-20T10:15:00Z");

  static Registration registration() {
    return new Registration(
        ID,
        RegistrationType.STUDENT,
        "Čedomir",
        "Šžč",
        "c@uni.si",
        null,
        "Uni",
        "CS",
        "123",
        List.of("b", "a"),
        List.of(
            new Registration.GivenConsent("privacy", Instant.parse("2026-10-20T10:15:00.120Z"))),
        AT);
  }

  private static FileJsonCopyStore store(Path dir) {
    return new FileJsonCopyStore(
        new AppSettings(
            "local",
            "C",
            "f",
            "",
            dir.toString(),
            new AppSettings.Organizer("o", "p", "", true),
            new AppSettings.Captcha(true, "", "", "u"),
            "",
            1,
            1));
  }

  @Test
  void jsonHasTheContractFieldsInOrderWithNullsAndIsoTimes() {
    assertThat(new String(RegistrationJson.toBytes(registration()), StandardCharsets.UTF_8))
        .isEqualTo(
            "{\"id\":\"00000000-0000-4000-8000-00000000000c\",\"type\":\"STUDENT\","
                + "\"firstName\":\"Čedomir\",\"lastName\":\"Šžč\",\"email\":\"c@uni.si\","
                + "\"organization\":null,\"studyInstitution\":\"Uni\",\"studyProgramme\":\"CS\","
                + "\"studentId\":\"123\",\"options\":[\"b\",\"a\"],"
                + "\"consents\":[{\"id\":\"privacy\",\"givenAt\":\"2026-10-20T10:15:00.120Z\"}],"
                + "\"registeredAt\":\"2026-10-20T10:15:00Z\"}");
  }

  @Test
  void writeStoresExactlyTheReturnedBytesUnderTheIdWithoutTemporaryFiles(@TempDir Path dir)
      throws Exception {
    Path copies = dir.resolve("nested/copies");
    FileJsonCopyStore store = store(copies);

    byte[] bytes = store.write(registration());

    assertThat(Files.readAllBytes(copies.resolve(ID + ".json"))).isEqualTo(bytes);
    assertThat(bytes).isEqualTo(RegistrationJson.toBytes(registration()));
    try (Stream<Path> files = Files.list(copies)) {
      assertThat(files).containsExactly(copies.resolve(ID + ".json"));
    }
  }

  @Test
  void deleteRemovesTheCopyAndIgnoresMissingOnes(@TempDir Path dir) {
    FileJsonCopyStore store = store(dir);
    store.write(registration());

    store.delete(ID);
    store.delete(ID);

    assertThat(dir.resolve(ID + ".json")).doesNotExist();
  }

  @Test
  void unwritableDirectoryFailsTheWriteButNotTheStart(@TempDir Path dir) throws Exception {
    Path file = Files.createFile(dir.resolve("plain-file"));
    FileJsonCopyStore store = store(file);

    assertThatThrownBy(() -> store.write(registration()))
        .isInstanceOf(StorageException.class)
        .hasMessage("JSON copy of registration " + ID + " failed");
    assertThatCode(() -> store.delete(ID)).doesNotThrowAnyException();
  }

  @Test
  void deleteFailureIsOnlyLogged(@TempDir Path dir) throws Exception {
    Files.createDirectories(dir.resolve(ID + ".json").resolve("child"));

    assertThatCode(() -> store(dir).delete(ID)).doesNotThrowAnyException();
  }
}
