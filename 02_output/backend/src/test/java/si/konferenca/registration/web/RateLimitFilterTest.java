package si.konferenca.registration.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

  /** A clock the test moves forward. */
  private static final class MovableClock extends Clock {
    private Instant now;

    MovableClock(Instant now) {
      this.now = now;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  private static final Instant MINUTE_START = Instant.parse("2026-10-21T10:00:00Z");

  private final MovableClock clock = new MovableClock(MINUTE_START.plusMillis(30_500));
  private final RateLimitFilter filter = new RateLimitFilter(2, clock);

  private MockHttpServletResponse call(String address, String method, String uri) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
    request.setRemoteAddr(address);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(request, response, chain);
    if (chain.getRequest() != null) {
      response.setStatus(299);
    }
    return response;
  }

  @Test
  void passesUpToTheLimitThenAnswers429WithRetryAfter() throws Exception {
    assertThat(call("10.0.0.1", "POST", "/api/registrations").getStatus()).isEqualTo(299);
    assertThat(call("10.0.0.1", "POST", "/api/registrations").getStatus()).isEqualTo(299);

    MockHttpServletResponse limited = call("10.0.0.1", "POST", "/api/registrations");

    assertThat(limited.getStatus()).isEqualTo(429);
    assertThat(limited.getHeader("Retry-After")).isEqualTo("30");
    assertThat(limited.getContentType()).isEqualTo("application/json;charset=UTF-8");
    assertThat(limited.getContentAsString()).isEqualTo(RateLimitFilter.BODY);
  }

  @Test
  void countsEachClientAndEndpointSeparately() throws Exception {
    call("10.0.0.1", "GET", "/api/export");
    call("10.0.0.1", "GET", "/api/export");

    assertThat(call("10.0.0.2", "GET", "/api/export").getStatus()).isEqualTo(299);
    assertThat(call("10.0.0.1", "GET", "/api/form-config").getStatus()).isEqualTo(299);
    assertThat(call("10.0.0.1", "POST", "/api/export").getStatus()).isEqualTo(299);
    assertThat(call("10.0.0.1", "GET", "/api/export").getStatus()).isEqualTo(429);
  }

  @Test
  void startsANewWindowEveryMinute() throws Exception {
    call("10.0.0.1", "GET", "/api/form-config");
    call("10.0.0.1", "GET", "/api/form-config");
    clock.now = MINUTE_START.plusMillis(59_999);
    MockHttpServletResponse lastMillisecond = call("10.0.0.1", "GET", "/api/form-config");
    assertThat(lastMillisecond.getStatus()).isEqualTo(429);
    assertThat(lastMillisecond.getHeader("Retry-After")).isEqualTo("1");

    clock.now = MINUTE_START.plusSeconds(60);

    assertThat(call("10.0.0.1", "GET", "/api/form-config").getStatus()).isEqualTo(299);
  }

  @Test
  void ignoresRequestsOutsideTheApi() throws Exception {
    for (int i = 0; i < 5; i++) {
      assertThat(call("10.0.0.1", "GET", "/actuator/health").getStatus()).isEqualTo(299);
    }
  }

  @Test
  void forgetsOldWindowsWhenTooManyClientsAreTracked() throws Exception {
    for (int i = 0; i < 10_001; i++) {
      call("10.1." + (i / 256) + "." + (i % 256), "GET", "/api/form-config");
    }
    assertThat(filter.trackedWindows()).isEqualTo(10_001);
    clock.now = MINUTE_START.plusSeconds(61);

    call("10.9.9.9", "GET", "/api/form-config");

    assertThat(filter.trackedWindows()).isEqualTo(1);
  }
}
