package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import si.konferenca.registration.acceptance.support.AcceptanceTest;
import si.konferenca.registration.acceptance.support.Database;
import si.konferenca.registration.acceptance.support.Submissions;
import si.konferenca.registration.acceptance.support.TestStack;

/**
 * US-005: the JSON copy directory lies below a regular file, so no copy can be written; the
 * registration must then not be accepted and not remain in the database.
 */
@AcceptanceTest
class JsonCopyFailureAcceptanceTest {

  private static final Path UNWRITABLE_DIR;

  static {
    try {
      UNWRITABLE_DIR = Files.createTempFile("not-a-directory", ".txt").resolve("copies");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Autowired private MockMvc mvc;

  private final Database db = TestStack.database();

  @DynamicPropertySource
  static void stack(DynamicPropertyRegistry registry) {
    TestStack.register(registry, Map.of("app.json-copy.directory", UNWRITABLE_DIR.toString()));
  }

  @Test
  void ac_005_03_failedJsonCopyAnswersWithErrorAndLeavesNoRegistration() throws Exception {
    Map<String, Object> body = Submissions.validExternal();

    mvc.perform(Submissions.post(body)).andExpect(status().is5xxServerError());

    assertThat(db.countByEmail((String) body.get("email"))).isZero();
    assertThat(Files.exists(UNWRITABLE_DIR)).isFalse();
  }
}
