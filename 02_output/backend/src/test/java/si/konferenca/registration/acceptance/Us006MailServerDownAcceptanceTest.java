package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static si.konferenca.registration.acceptance.support.Submissions.validExternal;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.HttpApi;
import si.konferenca.registration.acceptance.support.MailServerDownAcceptanceTest;

/** US-006 when the mail server cannot be reached (OQ-03). */
class Us006MailServerDownAcceptanceTest extends MailServerDownAcceptanceTest {

  @Test
  @DisplayName("AC-006-03 with the mail server unavailable the registration is accepted and stored")
  void ac006_03_registrationIsAcceptedWhenMailFails() {
    HttpApi.Response response = register(validExternal());

    assertThat(response.status()).as("response %s", response).isEqualTo(201);
    UUID id = UUID.fromString(response.json().path("id").asString());
    assertThat(db.row(id).lastName()).isEqualTo("Novak");
    assertThat(copies.files()).containsExactly(id + ".json");
  }
}
