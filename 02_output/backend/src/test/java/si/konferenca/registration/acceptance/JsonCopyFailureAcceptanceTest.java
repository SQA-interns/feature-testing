package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * AC-005-03: when the JSON copy cannot be written, the API does not answer 201 and no registration
 * remains. The JSON copy directory is replaced by a regular file while the backend runs, so no file
 * can be created in it; it is restored afterwards.
 */
class JsonCopyFailureAcceptanceTest extends DefaultContextAcceptanceTest {

  @Test
  @DisplayName("AC-005-03 an unwritable JSON copy answers an error and leaves no registration")
  void ac_005_03_unwritableJsonCopyLeavesNoRegistration() throws Exception {
    Path dir = JSON_COPY_DIR;
    try (Stream<Path> files = Files.list(dir)) {
      assertThat(files.count()).isZero();
    }
    Files.delete(dir);
    Files.writeString(dir, "not a directory");
    try {
      HttpResponse<String> response = postRegistration(validExternal());

      assertThat(response.statusCode()).as("status, body: %s", response.body()).isNotEqualTo(201);
      assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
      assertThat(response.body()).doesNotContain("Exception").doesNotContain(dir.toString());
      assertThat(registrationCount()).as("registrations in the database").isZero();
      assertThat(Files.readString(dir)).isEqualTo("not a directory");
      Thread.sleep(500);
      assertThat(messages()).as("emails").isEmpty();
    } finally {
      Files.deleteIfExists(dir);
      Files.createDirectories(dir);
    }
  }
}
