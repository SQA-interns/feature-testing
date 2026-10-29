package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import si.konferenca.registration.acceptance.support.OrganizerSearchTestBase;

/** SR-03 (MODIFIED by CR-001): the organizer search is rate limited per client. */
class OrganizerSearchRateLimitAcceptanceTest extends OrganizerSearchTestBase {

  private static final int LIMIT = 3;

  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry registry) {
    register(registry, Map.of("RATE_LIMIT_ORGANIZER_SEARCH_PER_MINUTE", String.valueOf(LIMIT)));
  }

  @Test
  @DisplayName(
      "SR-03 the organizer search answers 429 with Retry-After over its limit, failed logins"
          + " included")
  void sr_03_organizerSearchIsRateLimited() {
    // 2 * LIMIT + 1 calls: even if a one-minute window ends during the loop, one window
    // receives more than LIMIT calls.
    List<HttpResponse<String>> responses = new ArrayList<>();
    for (int i = 0; i < 2 * LIMIT + 1; i++) {
      responses.add(
          send(
              searchRequest(Map.of())
                  .header("Authorization", basicAuth(ORGANIZER_USERNAME, "wrong-password-123456"))
                  .GET()));
    }

    List<Integer> statuses = responses.stream().map(HttpResponse::statusCode).toList();
    assertThat(statuses).as("statuses").containsOnly(401, 429).contains(429);
    assertThat(statuses.subList(0, LIMIT)).as("the first calls are answered").containsOnly(401);
    HttpResponse<String> limited =
        responses.stream().filter(r -> r.statusCode() == 429).findFirst().orElseThrow();
    assertThat(limited.headers().firstValue("Retry-After")).isPresent();
    assertThat(json(limited).path("message").asString()).isNotBlank();
  }
}
