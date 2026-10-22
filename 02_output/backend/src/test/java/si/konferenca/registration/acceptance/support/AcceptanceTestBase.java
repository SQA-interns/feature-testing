package si.konferenca.registration.acceptance.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/**
 * Starts the backend on a random port against the shared containers and gives each test an empty
 * database, JSON copy directory and mailbox. Subclasses choose the settings (one Spring context per
 * family of settings).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTestBase {

  protected final Database db = new Database();
  protected final Mailpit mailpit = new Mailpit();
  protected HttpApi api;
  protected JsonCopies copies;

  @Autowired private Environment environment;

  protected abstract Path jsonCopyDirectory();

  @BeforeEach
  void resetState() {
    api = new HttpApi(environment.getRequiredProperty("local.server.port", Integer.class));
    copies = new JsonCopies(jsonCopyDirectory());
    db.clear();
    copies.clear();
    mailpit.deleteAll();
  }

  protected HttpApi.Response register(Map<String, Object> submission) {
    return api.postJson("/api/registrations", submission);
  }

  /** Registers and asserts acceptance (HTTP 201); returns the new registration id. */
  protected UUID registerAccepted(Map<String, Object> submission) {
    HttpApi.Response response = register(submission);
    assertThat(response.status()).as("registration response %s", response).isEqualTo(201);
    return UUID.fromString(response.json().path("id").asString());
  }

  /** "Rejected": HTTP 400 with an error for the field, and nothing stored or sent. */
  protected void assertRejected(HttpApi.Response response, String field) {
    assertThat(response.status()).as("response %s", response).isEqualTo(400);
    assertThat(response.errorFields()).as("error fields of %s", response).contains(field);
    assertNothingStored();
  }

  protected void assertNothingStored() {
    assertThat(db.countRegistrations()).as("registrations in the database").isZero();
    assertThat(copies.files()).as("JSON copies").isEmpty();
    assertThat(mailpit.messages()).as("emails sent").isEmpty();
  }
}
