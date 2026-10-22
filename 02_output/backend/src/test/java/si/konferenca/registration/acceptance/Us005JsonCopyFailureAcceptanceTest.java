package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.AcceptanceSupport;
import si.konferenca.registration.acceptance.support.RunningApp;

/** US-005 AC-005-03: the JSON copy directory is a regular file, so no JSON copy can be written. */
class Us005JsonCopyFailureAcceptanceTest extends AcceptanceSupport {

  private static RunningApp app;

  @BeforeAll
  static void startWithUnwritableJsonCopyDirectory() throws Exception {
    Path notADirectory = Files.createTempFile(WORK_DIR, "not-a-directory", ".txt");
    app = RunningApp.start(Map.of("JSON_COPY_DIR", notADirectory.toString()));
  }

  @AfterAll
  static void stop() {
    app.close();
  }

  @Override
  protected String baseUrl() {
    return app.baseUrl();
  }

  @DisplayName("AC-005-03")
  @Test
  void ac_005_03_failedJsonCopyAnswersWithErrorAndLeavesNoRegistration() {
    long rowsBefore = registrationRowCount();

    HttpResponse<byte[]> response = postRegistration(validExternal());

    assertThat(response.statusCode()).as(text(response)).isNotEqualTo(201).isBetween(500, 599);
    assertThat(registrationRowCount()).as("registration rows").isEqualTo(rowsBefore);
  }

  @DisplayName("AC-005-03")
  @Test
  void ac_005_03_failedJsonCopyForStudentLeavesNoRegistration() {
    long rowsBefore = registrationRowCount();

    HttpResponse<byte[]> response = postRegistration(validStudent());

    assertThat(response.statusCode()).as(text(response)).isNotEqualTo(201).isBetween(500, 599);
    assertThat(registrationRowCount()).as("registration rows").isEqualTo(rowsBefore);
  }
}
