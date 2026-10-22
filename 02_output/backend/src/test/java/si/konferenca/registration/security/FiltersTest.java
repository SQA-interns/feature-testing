package si.konferenca.registration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** RateLimitFilter, RequestSizeFilter, HttpsOnlyFilter and the JSON entry point. */
class FiltersTest {

  /** A clock whose time the test moves. */
  static final class MutableClock extends Clock {
    long millis;

    MutableClock(long millis) {
      this.millis = millis;
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
      return Instant.ofEpochMilli(millis);
    }
  }

  private static MockHttpServletRequest request(String method, String uri, String remote) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
    request.setRemoteAddr(remote);
    return request;
  }

  /** Runs the filter; returns the response, and whether the chain was called. */
  private static MockHttpServletResponse run(
      org.springframework.web.filter.OncePerRequestFilter filter,
      HttpServletRequest request,
      AtomicReference<HttpServletRequest> passed)
      throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain =
        new MockFilterChain() {
          @Override
          public void doFilter(
              jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
            passed.set((HttpServletRequest) req);
          }
        };
    filter.doFilter(request, response, chain);
    return response;
  }

  @Test
  void rateLimitAllowsTheLimitPerClientAndEndpointThenAnswers429() throws Exception {
    MutableClock clock = new MutableClock(120_000 + 15_500);
    RateLimitFilter filter = new RateLimitFilter(2, clock);
    AtomicReference<HttpServletRequest> passed = new AtomicReference<>();

    assertThat(run(filter, request("GET", "/api/form-config", "1.1.1.1"), passed).getStatus())
        .isEqualTo(200);
    assertThat(run(filter, request("GET", "/api/form-config", "1.1.1.1"), passed).getStatus())
        .isEqualTo(200);
    passed.set(null);
    MockHttpServletResponse limited =
        run(filter, request("GET", "/api/form-config", "1.1.1.1"), passed);

    assertThat(limited.getStatus()).isEqualTo(429);
    assertThat(limited.getHeader("Retry-After")).isEqualTo("45");
    assertThat(limited.getContentAsString()).isEqualTo("{\"message\":\"Too many requests.\"}");
    assertThat(passed.get()).isNull();
    assertThat(run(filter, request("GET", "/api/form-config", "2.2.2.2"), passed).getStatus())
        .isEqualTo(200);
    assertThat(run(filter, request("POST", "/api/form-config", "1.1.1.1"), passed).getStatus())
        .isEqualTo(200);
    assertThat(run(filter, request("GET", "/api/export", "1.1.1.1"), passed).getStatus())
        .isEqualTo(200);
  }

  @Test
  void rateLimitWindowStartsAgainEachMinute() throws Exception {
    MutableClock clock = new MutableClock(59_999);
    RateLimitFilter filter = new RateLimitFilter(1, clock);
    AtomicReference<HttpServletRequest> passed = new AtomicReference<>();
    run(filter, request("GET", "/api/x", "1.1.1.1"), passed);

    MockHttpServletResponse limited = run(filter, request("GET", "/api/x", "1.1.1.1"), passed);
    clock.millis = 60_000;

    assertThat(limited.getStatus()).isEqualTo(429);
    assertThat(limited.getHeader("Retry-After")).isEqualTo("1");
    assertThat(run(filter, request("GET", "/api/x", "1.1.1.1"), passed).getStatus()).isEqualTo(200);
  }

  @Test
  void rateLimitIgnoresPathsOutsideTheApi() throws Exception {
    RateLimitFilter filter = new RateLimitFilter(1, new MutableClock(0));
    AtomicReference<HttpServletRequest> passed = new AtomicReference<>();

    for (int i = 0; i < 3; i++) {
      assertThat(run(filter, request("GET", "/actuator/health", "1.1.1.1"), passed).getStatus())
          .isEqualTo(200);
    }
    assertThat(run(filter, request("GET", "/apix", "1.1.1.1"), passed).getStatus()).isEqualTo(200);
    assertThat(run(filter, request("GET", "/apix", "1.1.1.1"), passed).getStatus()).isEqualTo(200);
  }

  @Test
  void rateLimitForgetsOldWindowsWhenTrackingManyClients() throws Exception {
    MutableClock clock = new MutableClock(0);
    RateLimitFilter filter = new RateLimitFilter(1, clock);
    AtomicReference<HttpServletRequest> passed = new AtomicReference<>();
    for (int i = 0; i <= 10_001; i++) {
      run(filter, request("GET", "/api/x", "10.0." + (i / 256) + "." + (i % 256)), passed);
    }
    clock.millis = 60_000;

    assertThat(run(filter, request("GET", "/api/x", "10.0.0.0"), passed).getStatus())
        .isEqualTo(200);
    assertThat(run(filter, request("GET", "/api/x", "10.0.0.0"), passed).getStatus())
        .isEqualTo(429);
  }

  @Test
  void declaredBodyOverTheLimitIsAnswered413() throws Exception {
    RequestSizeFilter filter = new RequestSizeFilter(10);
    MockHttpServletRequest request = request("POST", "/api/registrations", "1.1.1.1");
    request.setContent(new byte[11]);
    AtomicReference<HttpServletRequest> passed = new AtomicReference<>();

    MockHttpServletResponse response = run(filter, request, passed);

    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(response.getContentAsString()).isEqualTo("{\"message\":\"Request too large.\"}");
    assertThat(passed.get()).isNull();
  }

  @Test
  void bodyAtTheLimitPassesAndIsReadable() throws Exception {
    RequestSizeFilter filter = new RequestSizeFilter(10);
    MockHttpServletRequest request = request("POST", "/api/registrations", "1.1.1.1");
    request.setContent("0123456789".getBytes());
    AtomicReference<HttpServletRequest> passed = new AtomicReference<>();

    run(filter, request, passed);

    ServletInputStream in = passed.get().getInputStream();
    assertThat(passed.get().getInputStream()).isSameAs(in);
    assertThat(in.read()).isEqualTo('0');
    byte[] rest = new byte[20];
    assertThat(in.read(rest, 0, 20)).isEqualTo(9);
    assertThat(in.read()).isEqualTo(-1);
    assertThat(in.read(rest, 0, 20)).isEqualTo(-1);
    assertThat(in.isFinished()).isTrue();
    assertThat(in.isReady()).isTrue();
  }

  @Test
  void undeclaredBodyStopsBeingReadableAfterTheLimit() throws Exception {
    RequestSizeFilter filter = new RequestSizeFilter(3);
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/registrations") {
          @Override
          public long getContentLengthLong() {
            return -1;
          }
        };
    request.setContent("abcdef".getBytes());
    AtomicReference<HttpServletRequest> passed = new AtomicReference<>();
    run(filter, request, passed);
    ServletInputStream in = passed.get().getInputStream();

    assertThat(in.read(new byte[3], 0, 3)).isEqualTo(3);
    assertThatThrownBy(in::read)
        .isInstanceOf(IOException.class)
        .hasMessage("Request body too large");
    ServletInputStream again = new RequestSizeFilterProbe(passed.get()).fresh();
    assertThatThrownBy(() -> again.read(new byte[10], 0, 10)).isInstanceOf(IOException.class);
  }

  /** Reads the wrapped stream of a fresh request with the same limit. */
  private static final class RequestSizeFilterProbe {
    private final HttpServletRequest wrapped;

    RequestSizeFilterProbe(HttpServletRequest wrapped) {
      this.wrapped = wrapped;
    }

    ServletInputStream fresh() throws Exception {
      MockHttpServletRequest request =
          new MockHttpServletRequest("POST", "/x") {
            @Override
            public long getContentLengthLong() {
              return -1;
            }
          };
      request.setContent("abcdef".getBytes());
      AtomicReference<HttpServletRequest> passed = new AtomicReference<>();
      run(new RequestSizeFilter(3), request, passed);
      assertThat(wrapped).isNotNull();
      return passed.get().getInputStream();
    }
  }

  @Test
  void readListenerIsPassedOn() throws Exception {
    AtomicReference<ReadListener> registered = new AtomicReference<>();
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/x") {
          @Override
          public ServletInputStream getInputStream() {
            return new ServletInputStream() {
              @Override
              public boolean isFinished() {
                return false;
              }

              @Override
              public boolean isReady() {
                return false;
              }

              @Override
              public void setReadListener(ReadListener listener) {
                registered.set(listener);
              }

              @Override
              public int read() {
                return -1;
              }
            };
          }
        };
    AtomicReference<HttpServletRequest> passed = new AtomicReference<>();
    run(new RequestSizeFilter(5), request, passed);
    ServletInputStream in = passed.get().getInputStream();
    ReadListener listener =
        new ReadListener() {
          @Override
          public void onDataAvailable() {}

          @Override
          public void onAllDataRead() {}

          @Override
          public void onError(Throwable t) {}
        };

    in.setReadListener(listener);

    assertThat(registered.get()).isSameAs(listener);
    assertThat(in.isFinished()).isFalse();
    assertThat(in.isReady()).isFalse();
  }

  @Test
  void httpsOnlyRefusesPlainOrganizerRequestsFromOtherHosts() throws Exception {
    HttpsOnlyFilter filter = new HttpsOnlyFilter(true);
    AtomicReference<HttpServletRequest> passed = new AtomicReference<>();

    for (String uri : new String[] {"/api/export", "/api/registrations"}) {
      passed.set(null);
      MockHttpServletResponse response = run(filter, request("GET", uri, "203.0.113.7"), passed);

      assertThat(response.getStatus()).isEqualTo(403);
      assertThat(response.getContentAsString())
          .isEqualTo("{\"message\":\"Organizer access requires HTTPS.\"}");
      assertThat(response.getContentType()).startsWith("application/json");
      assertThat(passed.get()).isNull();
    }
  }

  @Test
  void httpsOnlyAllowsSecureLocalhostAndPublicRequests() throws Exception {
    HttpsOnlyFilter filter = new HttpsOnlyFilter(true);
    AtomicReference<HttpServletRequest> passed = new AtomicReference<>();
    MockHttpServletRequest secure = request("GET", "/api/export", "203.0.113.7");
    secure.setSecure(true);

    for (HttpServletRequest allowed :
        new HttpServletRequest[] {
          secure,
          request("GET", "/api/export", "127.0.0.1"),
          request("GET", "/api/registrations", "127.10.0.2"),
          request("GET", "/api/export", "0:0:0:0:0:0:0:1"),
          request("POST", "/api/registrations", "203.0.113.7"),
          request("GET", "/api/form-config", "203.0.113.7"),
          request("GET", "/api/exports", "203.0.113.7")
        }) {
      passed.set(null);
      assertThat(run(filter, allowed, passed).getStatus()).isEqualTo(200);
      assertThat(passed.get()).isNotNull();
    }
    assertThat(
            run(new HttpsOnlyFilter(false), request("GET", "/api/export", "203.0.113.7"), passed)
                .getStatus())
        .isEqualTo(200);
  }

  @Test
  void loopbackCheckHandlesEveryAddressForm() {
    assertThat(HttpsOnlyFilter.isLoopback("127.0.0.1")).isTrue();
    assertThat(HttpsOnlyFilter.isLoopback("::1")).isTrue();
    assertThat(HttpsOnlyFilter.isLoopback("10.0.0.1")).isFalse();
    assertThat(HttpsOnlyFilter.isLoopback("")).isFalse();
    assertThat(HttpsOnlyFilter.isLoopback(null)).isFalse();
    assertThat(HttpsOnlyFilter.isLoopback("no-such-host.invalid")).isFalse();
  }

  @Test
  void organizerRequestsAreRecognisedBehindAContextPath() {
    MockHttpServletRequest request = request("GET", "/app/api/export", "1.1.1.1");
    request.setContextPath("/app");

    assertThat(HttpsOnlyFilter.isOrganizerRequest(request)).isTrue();
  }

  @Test
  void entryPointChallengesUnlessTheOrganizerPageAsks() throws Exception {
    JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint();
    MockHttpServletResponse browser = new MockHttpServletResponse();
    MockHttpServletResponse page = new MockHttpServletResponse();
    MockHttpServletRequest fromPage = request("GET", "/api/export", "1.1.1.1");
    fromPage.addHeader("X-Requested-With", "XMLHttpRequest");

    entryPoint.commence(request("GET", "/api/export", "1.1.1.1"), browser, null);
    entryPoint.commence(fromPage, page, null);

    assertThat(browser.getStatus()).isEqualTo(401);
    assertThat(browser.getHeader("WWW-Authenticate")).isEqualTo("Basic realm=\"organizer\"");
    assertThat(browser.getContentAsString()).isEqualTo("{\"message\":\"Authentication required\"}");
    assertThat(browser.getCharacterEncoding()).isEqualTo("UTF-8");
    assertThat(page.getStatus()).isEqualTo(401);
    assertThat(page.getHeader("WWW-Authenticate")).isNull();
  }
}
