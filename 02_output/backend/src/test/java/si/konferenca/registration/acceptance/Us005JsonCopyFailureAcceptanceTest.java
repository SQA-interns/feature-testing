package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.acceptance.support.Submissions.validExternal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.HttpApi;
import si.konferenca.registration.acceptance.support.JsonCopyUnwritableAcceptanceTest;

/** US-005 when the JSON copy cannot be written (AR-05). */
class Us005JsonCopyFailureAcceptanceTest extends JsonCopyUnwritableAcceptanceTest {

  @Test
  @DisplayName("AC-005-03 an unwritable JSON copy gives an error and leaves no database row")
  void ac005_03_unwritableJsonCopyLeavesNoRegistration() {
    assertThat(db.schemaExists()).as("the backend created its schema").isTrue();

    HttpApi.Response response = register(validExternal());

    assertThat(response.status()).as("response %s", response).isNotEqualTo(201);
    assertThat(response.status()).as("an error status").isGreaterThanOrEqualTo(400);
    assertThat(response.text()).doesNotContain("Novak", "ana.novak@example.org");
    assertThat(db.countRegistrations()).isZero();
    assertThat(copies.files()).isEmpty();
  }
}
