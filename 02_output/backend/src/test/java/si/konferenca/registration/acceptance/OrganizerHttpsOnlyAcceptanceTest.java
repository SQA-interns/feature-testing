package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import si.konferenca.registration.acceptance.support.AcceptanceSupport;
import si.konferenca.registration.acceptance.support.RunningApp;
import tools.jackson.databind.node.ObjectNode;

/**
 * AC-008-04 and AC-009-10 (SR-06): organizer HTTPS-only access is on. The test client connects from
 * 127.0.0.1, which is configured as the trusted proxy; a request it forwards for 203.0.113.7 comes
 * from a client that is not on localhost.
 */
class OrganizerHttpsOnlyAcceptanceTest extends AcceptanceSupport {

  private static RunningApp app;
  private static String email;

  @BeforeAll
  static void startWithHttpsOnly() {
    app =
        RunningApp.start(
            Map.of("ORGANIZER_HTTPS_ONLY", "true", "TRUSTED_PROXIES", "127\\.0\\.0\\.1"));
  }

  @AfterAll
  static void stop() {
    app.close();
  }

  @Override
  protected String baseUrl() {
    return app.baseUrl();
  }

  private String registerOne() {
    ObjectNode body = validExternal();
    email = body.path("email").asString();
    return register(body);
  }

  private HttpResponse<byte[]> organizerRequest(String path, String forwardedProto) {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create(baseUrl() + path))
            .header("Authorization", basic(ORGANIZER_USERNAME, ORGANIZER_PASSWORD));
    if (forwardedProto != null) {
      builder.header("X-Forwarded-For", "203.0.113.7").header("X-Forwarded-Proto", forwardedProto);
    }
    return send(builder.GET().build());
  }

  private static void assertRefusedWithoutData(HttpResponse<byte[]> response) {
    assertThat(response.statusCode()).as(text(response)).isGreaterThanOrEqualTo(400);
    byte[] body = response.body();
    assertThat(body.length >= 2 && body[0] == 'P' && body[1] == 'K').as("workbook").isFalse();
    assertThat(text(response)).doesNotContain(email);
  }

  @DisplayName("AC-008-04")
  @Test
  void ac_008_04_exportOverPlainHttpFromNonLocalClientIsRefused() {
    registerOne();

    assertRefusedWithoutData(organizerRequest("/api/export", "http"));
  }

  @DisplayName("AC-008-04")
  @Test
  void ac_008_04_exportOverHttpsOrFromLocalhostIsAllowed() {
    registerOne();

    assertThat(organizerRequest("/api/export", "https").statusCode()).isEqualTo(200);
    assertThat(organizerRequest("/api/export", null).statusCode()).isEqualTo(200);
  }

  @DisplayName("AC-009-10")
  @Test
  void ac_009_10_searchOverPlainHttpFromNonLocalClientIsRefused() {
    registerOne();

    assertRefusedWithoutData(organizerRequest("/api/registrations?text=" + encode(email), "http"));
  }

  @DisplayName("AC-009-10")
  @Test
  void ac_009_10_searchOverHttpsOrFromLocalhostIsAllowed() {
    registerOne();
    String path = "/api/registrations?text=" + encode(email);

    HttpResponse<byte[]> overHttps = organizerRequest(path, "https");
    HttpResponse<byte[]> fromLocalhost = organizerRequest(path, null);

    assertThat(overHttps.statusCode()).isEqualTo(200);
    assertThat(text(overHttps)).contains(email);
    assertThat(fromLocalhost.statusCode()).isEqualTo(200);
    assertThat(text(fromLocalhost)).contains(email);
  }
}
