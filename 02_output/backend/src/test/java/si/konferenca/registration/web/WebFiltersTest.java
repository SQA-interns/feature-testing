package si.konferenca.registration.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import si.konferenca.registration.config.AppProperties;

class WebFiltersTest {

  private static AppProperties properties(int maxBytes) {
    return new AppProperties(
        "test",
        new AppProperties.Conference("C", Path.of("c.json")),
        Path.of("copies"),
        new AppProperties.Recaptcha(true, "", "", ""),
        new AppProperties.Organizer("o", "p", "a@x.si", true),
        new AppProperties.Mail("h", 25, false, null, null, "f@x.si"),
        null,
        new AppProperties.RateLimit(2, 1, 3),
        maxBytes);
  }

  private static MockHttpServletRequest request(String method, String uri, String address) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
    request.setRemoteAddr(address);
    return request;
  }

  private static final class MutableClock extends Clock {
    private Instant now;

    MutableClock(Instant now) {
      this.now = now;
    }

    @Override
    public java.time.ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  private static int call(RateLimitFilter filter, MockHttpServletRequest request) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    return response.getStatus();
  }

  @Test
  void rateLimitCountsPerGroupAndClientAndAnswers429() throws Exception {
    MutableClock clock = new MutableClock(Instant.ofEpochMilli(120_000 + 59_000));
    RateLimitFilter filter = new RateLimitFilter(properties(100), clock);

    assertThat(call(filter, request("GET", "/api/form-config", "10.0.0.1"))).isEqualTo(200);
    assertThat(call(filter, request("GET", "/api/form-config", "10.0.0.1"))).isEqualTo(200);
    MockHttpServletResponse limited = new MockHttpServletResponse();
    filter.doFilter(request("GET", "/api/form-config", "10.0.0.1"), limited, new MockFilterChain());
    assertThat(limited.getStatus()).isEqualTo(429);
    assertThat(limited.getHeader("Retry-After")).isEqualTo("1");
    assertThat(limited.getContentAsString())
        .isEqualTo("{\"message\":\"Too many requests. Please try again later.\"}");

    assertThat(call(filter, request("GET", "/api/form-config", "10.0.0.2"))).isEqualTo(200);
    assertThat(call(filter, request("POST", "/api/registrations", "10.0.0.1"))).isEqualTo(200);
    assertThat(call(filter, request("POST", "/api/registrations", "10.0.0.1"))).isEqualTo(429);
    for (int i = 0; i < 3; i++) {
      assertThat(call(filter, request("GET", "/api/registrations/export", "10.0.0.1")))
          .isEqualTo(200);
    }
    assertThat(call(filter, request("GET", "/api/registrations/export", "10.0.0.1")))
        .isEqualTo(429);
  }

  @Test
  void rateLimitRetryAfterRoundsUpToTheWindowEnd() throws Exception {
    MutableClock clock = new MutableClock(Instant.ofEpochMilli(60_000 + 500));
    RateLimitFilter filter = new RateLimitFilter(properties(100), clock);
    call(filter, request("POST", "/api/registrations", "10.0.0.1"));

    MockHttpServletResponse limited = new MockHttpServletResponse();
    filter.doFilter(
        request("POST", "/api/registrations", "10.0.0.1"), limited, new MockFilterChain());

    assertThat(limited.getHeader("Retry-After")).isEqualTo("60");
  }

  @Test
  void rateLimitStartsAgainInTheNextMinute() throws Exception {
    MutableClock clock = new MutableClock(Instant.ofEpochMilli(59_999));
    RateLimitFilter filter = new RateLimitFilter(properties(100), clock);
    call(filter, request("POST", "/api/registrations", "10.0.0.1"));
    assertThat(call(filter, request("POST", "/api/registrations", "10.0.0.1"))).isEqualTo(429);

    clock.now = Instant.ofEpochMilli(60_000);

    assertThat(call(filter, request("POST", "/api/registrations", "10.0.0.1"))).isEqualTo(200);
    assertThat(call(filter, request("POST", "/api/registrations", "10.0.0.1"))).isEqualTo(429);
  }

  @Test
  void rateLimitIgnoresNonApiPaths() throws Exception {
    RateLimitFilter filter = new RateLimitFilter(properties(100), new MutableClock(Instant.EPOCH));
    for (int i = 0; i < 5; i++) {
      assertThat(call(filter, request("GET", "/actuator/health", "10.0.0.1"))).isEqualTo(200);
    }
  }

  private static MockHttpServletResponse sized(MockHttpServletRequest request, int max)
      throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    new RequestSizeFilter(properties(max)).doFilter(request, response, new MockFilterChain());
    return response;
  }

  @Test
  void declaredLengthOverTheLimitIsRefusedAtOnce() throws Exception {
    MockHttpServletRequest request = request("POST", "/api/registrations", "10.0.0.1");
    request.setContent(new byte[11]);

    MockHttpServletResponse response = sized(request, 10);

    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(response.getContentAsString())
        .isEqualTo("{\"message\":\"The request is too large.\"}");
    assertThat(response.getContentType()).startsWith("application/json");
  }

  @Test
  void declaredLengthAtTheLimitPasses() throws Exception {
    MockHttpServletRequest request = request("POST", "/api/registrations", "10.0.0.1");
    request.setContent(new byte[10]);

    assertThat(sized(request, 10).getStatus()).isEqualTo(200);
  }

  private static ServletInputStream limitedStream(byte[] body, int max) throws Exception {
    MockHttpServletRequest undeclared =
        new MockHttpServletRequest("POST", "/api/registrations") {
          @Override
          public long getContentLengthLong() {
            return -1;
          }

          @Override
          public int getContentLength() {
            return -1;
          }
        };
    undeclared.setContent(body);
    AtomicReference<HttpServletRequest> seen = new AtomicReference<>();
    new RequestSizeFilter(properties(max))
        .doFilter(
            undeclared,
            new MockHttpServletResponse(),
            (req, res) -> seen.set((HttpServletRequest) req));
    return seen.get().getInputStream();
  }

  @Test
  void undeclaredBodyIsCountedWhileRead() throws Exception {
    ServletInputStream atLimit = limitedStream(new byte[] {1, 2, 3}, 3);
    assertThat(atLimit.read()).isEqualTo(1);
    assertThat(atLimit.read(new byte[8], 0, 8)).isEqualTo(2);
    assertThat(atLimit.read()).isEqualTo(-1);
    assertThat(atLimit.read(new byte[8], 0, 8)).isEqualTo(-1);
    assertThat(atLimit.isFinished()).isTrue();
    assertThat(atLimit.isReady()).isTrue();

    ServletInputStream bulk = limitedStream(new byte[] {1, 2, 3, 4}, 3);
    assertThatThrownBy(() -> bulk.read(new byte[8], 0, 8))
        .isInstanceOf(PayloadTooLargeException.class)
        .hasMessage("request body too large");

    ServletInputStream single = limitedStream(new byte[] {1, 2, 3, 4}, 3);
    single.read();
    single.read();
    single.read();
    assertThatThrownBy(single::read).isInstanceOf(PayloadTooLargeException.class);
  }

  @Test
  void sizeFilterIgnoresNonApiPaths() throws Exception {
    MockHttpServletRequest request = request("POST", "/other", "10.0.0.1");
    request.setContent(new byte[11]);

    assertThat(sized(request, 10).getStatus()).isEqualTo(200);
  }

  private static MockHttpServletResponse https(
      boolean httpsOnly, String uri, String address, boolean secure) throws Exception {
    MockHttpServletRequest request = request("GET", uri, address);
    request.setSecure(secure);
    MockHttpServletResponse response = new MockHttpServletResponse();
    new HttpsOnlyFilter(httpsOnly).doFilter(request, response, new MockFilterChain());
    return response;
  }

  @Test
  void exportOverPlainHttpFromAnotherHostIsRefused() throws Exception {
    MockHttpServletResponse response =
        https(true, HttpsOnlyFilter.EXPORT_PATH, "203.0.113.10", false);

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString())
        .isEqualTo("{\"message\":\"Organizer access requires HTTPS.\"}");
  }

  @Test
  void exportOverHttpsLoopbackOrWithTheCheckOffPasses() throws Exception {
    assertThat(https(true, HttpsOnlyFilter.EXPORT_PATH, "203.0.113.10", true).getStatus())
        .isEqualTo(200);
    assertThat(https(true, HttpsOnlyFilter.EXPORT_PATH, "127.0.0.1", false).getStatus())
        .isEqualTo(200);
    assertThat(https(false, HttpsOnlyFilter.EXPORT_PATH, "203.0.113.10", false).getStatus())
        .isEqualTo(200);
    assertThat(https(true, "/api/form-config", "203.0.113.10", false).getStatus()).isEqualTo(200);
  }

  @ParameterizedTest
  @ValueSource(strings = {"127.0.0.1", "127.1.2.3", "::1", "0:0:0:0:0:0:0:1"})
  void loopbackAddresses(String address) {
    assertThat(HttpsOnlyFilter.isLoopback(address)).isTrue();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "localhost", "10.0.0.1", "::2", "999.1.1.1", "fe80::1%eth0"})
  void otherAddresses(String address) {
    assertThat(HttpsOnlyFilter.isLoopback(address)).isFalse();
  }

  @Test
  void unauthenticatedAnswerHasAChallengeAndNoData() throws IOException {
    MockHttpServletResponse response = new MockHttpServletResponse();

    new JsonAuthenticationEntryPoint().commence(new MockHttpServletRequest(), response, null);

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getHeader("WWW-Authenticate"))
        .isEqualTo("Basic realm=\"registration-export\"");
    assertThat(response.getContentAsString())
        .isEqualTo("{\"message\":\"Authentication required.\"}");
    assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
  }
}
