package si.konferenca.registration.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestFiltersTest {

  private static MockHttpServletRequest request(String method, String path, String peer) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    request.setRemoteAddr(peer);
    return request;
  }

  // ClientAddress

  @Test
  void directClientIsThePeer() {
    assertThat(ClientAddress.of(request("GET", "/", "203.0.113.9"))).isEqualTo("203.0.113.9");
  }

  @Test
  void forwardedForCountsOnlyFromTrustedProxies() {
    MockHttpServletRequest fromProxy = request("GET", "/", "172.18.0.5");
    fromProxy.addHeader("X-Forwarded-For", "198.51.100.1, 203.0.113.7 ");
    MockHttpServletRequest fromOutside = request("GET", "/", "203.0.113.9");
    fromOutside.addHeader("X-Forwarded-For", "127.0.0.1");
    MockHttpServletRequest emptyHeader = request("GET", "/", "10.0.0.2");
    emptyHeader.addHeader("X-Forwarded-For", " ");
    MockHttpServletRequest trailingComma = request("GET", "/", "192.168.1.2");
    trailingComma.addHeader("X-Forwarded-For", "203.0.113.7, ");

    assertThat(ClientAddress.of(fromProxy)).isEqualTo("203.0.113.7");
    assertThat(ClientAddress.of(fromOutside)).isEqualTo("203.0.113.9");
    assertThat(ClientAddress.of(emptyHeader)).isEqualTo("10.0.0.2");
    assertThat(ClientAddress.of(trailingComma)).isEqualTo("192.168.1.2");
  }

  @Test
  void trustedProxiesAreLoopbackAndPrivateAddresses() {
    assertThat(ClientAddress.isTrustedProxy("127.0.0.1")).isTrue();
    assertThat(ClientAddress.isTrustedProxy("::1")).isTrue();
    assertThat(ClientAddress.isTrustedProxy("10.1.2.3")).isTrue();
    assertThat(ClientAddress.isTrustedProxy("172.16.0.1")).isTrue();
    assertThat(ClientAddress.isTrustedProxy("192.168.0.1")).isTrue();
    assertThat(ClientAddress.isTrustedProxy("fd12::1")).isTrue();
    assertThat(ClientAddress.isTrustedProxy("fc00::1")).isTrue();
    assertThat(ClientAddress.isTrustedProxy("fe80::1")).isFalse();
    assertThat(ClientAddress.isTrustedProxy("2001:db8::1")).isFalse();
    assertThat(ClientAddress.isTrustedProxy("8.8.8.8")).isFalse();
    assertThat(ClientAddress.isTrustedProxy("example.org")).isFalse();
    assertThat(ClientAddress.isTrustedProxy(null)).isFalse();
    assertThat(ClientAddress.isTrustedProxy("1.2.3.4.5")).isFalse();
  }

  @Test
  void secureMeansTlsOrHttpsFromATrustedProxy() {
    MockHttpServletRequest tls = request("GET", "/", "203.0.113.9");
    tls.setSecure(true);
    MockHttpServletRequest proxied = request("GET", "/", "10.0.0.2");
    proxied.addHeader("X-Forwarded-Proto", "HTTPS, http");
    MockHttpServletRequest spoofed = request("GET", "/", "203.0.113.9");
    spoofed.addHeader("X-Forwarded-Proto", "https");
    MockHttpServletRequest plainProxied = request("GET", "/", "10.0.0.2");
    plainProxied.addHeader("X-Forwarded-Proto", "http");

    assertThat(ClientAddress.isSecure(tls)).isTrue();
    assertThat(ClientAddress.isSecure(proxied)).isTrue();
    assertThat(ClientAddress.isSecure(spoofed)).isFalse();
    assertThat(ClientAddress.isSecure(plainProxied)).isFalse();
    assertThat(ClientAddress.isSecure(request("GET", "/", "10.0.0.2"))).isFalse();
  }

  @Test
  void localMeansALoopbackClient() {
    MockHttpServletRequest forwardedRemote = request("GET", "/", "127.0.0.1");
    forwardedRemote.addHeader("X-Forwarded-For", "203.0.113.7");
    MockHttpServletRequest forwardedGarbage = request("GET", "/", "127.0.0.1");
    forwardedGarbage.addHeader("X-Forwarded-For", "localhost");

    assertThat(ClientAddress.isLocal(request("GET", "/", "127.0.0.1"))).isTrue();
    assertThat(ClientAddress.isLocal(request("GET", "/", "172.18.0.1"))).isFalse();
    assertThat(ClientAddress.isLocal(forwardedRemote)).isFalse();
    assertThat(ClientAddress.isLocal(forwardedGarbage)).isFalse();
  }

  // HttpsOnlyFilter

  @Test
  void httpsOnlyRefusesRemotePlainOrganizerRequests() throws Exception {
    HttpsOnlyFilter filter = new HttpsOnlyFilter(true);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request("GET", "/api/export", "203.0.113.9"), response, chain);

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString()).contains("\"field\":\"protocol\"");
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void httpsOnlyLetsSecureAndLocalRequestsThrough() throws Exception {
    HttpsOnlyFilter filter = new HttpsOnlyFilter(true);
    MockHttpServletRequest secure = request("GET", "/api/registrations", "203.0.113.9");
    secure.setSecure(true);

    for (HttpServletRequest allowed :
        new HttpServletRequest[] {secure, request("GET", "/api/registrations", "127.0.0.1")}) {
      MockFilterChain chain = new MockFilterChain();
      filter.doFilter(allowed, new MockHttpServletResponse(), chain);
      assertThat(chain.getRequest()).isSameAs(allowed);
    }
  }

  @Test
  void httpsOnlyIgnoresOtherRequestsAndCanBeOff() {
    HttpsOnlyFilter on = new HttpsOnlyFilter(true);
    HttpsOnlyFilter off = new HttpsOnlyFilter(false);

    assertThat(on.shouldNotFilter(request("GET", "/api/export", "203.0.113.9"))).isFalse();
    assertThat(on.shouldNotFilter(request("GET", "/api/registrations", "203.0.113.9"))).isFalse();
    assertThat(on.shouldNotFilter(request("POST", "/api/registrations", "203.0.113.9"))).isTrue();
    assertThat(on.shouldNotFilter(request("GET", "/api/form-config", "203.0.113.9"))).isTrue();
    assertThat(off.shouldNotFilter(request("GET", "/api/export", "203.0.113.9"))).isTrue();
  }

  // RateLimitFilter

  @Test
  void rateLimitAllowsTheLimitPerClientEndpointAndMinute() throws Exception {
    MutableClock clock = new MutableClock(Instant.parse("2026-10-20T10:00:10Z"));
    RateLimitFilter filter = new RateLimitFilter(2, clock);

    assertThat(status(filter, request("GET", "/api/form-config", "1.1.1.1"))).isEqualTo(200);
    assertThat(status(filter, request("GET", "/api/form-config", "1.1.1.1"))).isEqualTo(200);
    MockHttpServletResponse limited = new MockHttpServletResponse();
    filter.doFilter(request("GET", "/api/form-config", "1.1.1.1"), limited, new MockFilterChain());
    assertThat(limited.getStatus()).isEqualTo(429);
    assertThat(limited.getHeader("Retry-After")).isEqualTo("50");
    assertThat(status(filter, request("GET", "/api/form-config", "2.2.2.2"))).isEqualTo(200);
    assertThat(status(filter, request("POST", "/api/form-config", "1.1.1.1"))).isEqualTo(200);
    assertThat(status(filter, request("GET", "/api/export", "1.1.1.1"))).isEqualTo(200);

    clock.now = Instant.parse("2026-10-20T10:01:00Z");
    assertThat(status(filter, request("GET", "/api/form-config", "1.1.1.1"))).isEqualTo(200);
  }

  @Test
  void rateLimitAppliesOnlyToTheApi() {
    RateLimitFilter filter = new RateLimitFilter(1, Clock.systemUTC());

    assertThat(filter.shouldNotFilter(request("GET", "/actuator/health", "1.1.1.1"))).isTrue();
    assertThat(filter.shouldNotFilter(request("GET", "/api/form-config", "1.1.1.1"))).isFalse();
  }

  @Test
  void rateLimitForgetsOldWindowsWhenItTracksTooManyClients() throws Exception {
    MutableClock clock = new MutableClock(Instant.parse("2026-10-20T10:00:00Z"));
    RateLimitFilter filter = new RateLimitFilter(1, clock);
    for (int i = 0; i <= RateLimitFilter.MAX_TRACKED; i++) {
      status(filter, request("GET", "/api/form-config", "client-" + i));
    }
    clock.now = Instant.parse("2026-10-20T10:01:00Z");
    status(filter, request("GET", "/api/form-config", "new-client"));
    clock.now = Instant.parse("2026-10-20T10:01:30Z");

    assertThat(status(filter, request("GET", "/api/form-config", "new-client"))).isEqualTo(429);
    assertThat(status(filter, request("GET", "/api/form-config", "client-1"))).isEqualTo(200);
  }

  // RequestSizeFilter

  @Test
  void declaredLargeBodyIsRefusedAtOnce() throws Exception {
    RequestSizeFilter filter = new RequestSizeFilter(10);
    MockHttpServletRequest big = request("POST", "/api/registrations", "1.1.1.1");
    big.setContent(new byte[11]);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(big, response, chain);

    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(response.getContentAsString()).contains("\"field\":\"body\"");
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void bodyAtTheLimitIsReadCompletely() throws Exception {
    RequestSizeFilter filter = new RequestSizeFilter(10);
    MockHttpServletRequest exact = request("POST", "/api/registrations", "1.1.1.1");
    exact.setContent("0123456789".getBytes(StandardCharsets.UTF_8));
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(exact, new MockHttpServletResponse(), chain);

    HttpServletRequest passed = (HttpServletRequest) chain.getRequest();
    ServletInputStream in = passed.getInputStream();
    assertThat(passed.getInputStream()).isSameAs(in);
    assertThat(in.readAllBytes()).hasSize(10);
    assertThat(in.isFinished()).isTrue();
    assertThat(in.isReady()).isTrue();
    assertThat(passed.getAttribute(RequestSizeFilter.TOO_LARGE)).isNull();
  }

  @Test
  void undeclaredLargeBodyFailsWhileReading() throws Exception {
    RequestSizeFilter filter = new RequestSizeFilter(4);
    MockHttpServletRequest chunked =
        new MockHttpServletRequest("POST", "/api/registrations") {
          @Override
          public long getContentLengthLong() {
            return -1;
          }
        };
    chunked.setContent("0123456789".getBytes(StandardCharsets.UTF_8));
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(chunked, new MockHttpServletResponse(), chain);
    HttpServletRequest passed = (HttpServletRequest) chain.getRequest();
    ServletInputStream in = passed.getInputStream();

    assertThat(in.read()).isEqualTo('0');
    assertThat(in.read(new byte[3], 0, 3)).isEqualTo(3);
    assertThatThrownBy(in::read).isInstanceOf(IOException.class);
    assertThat(passed.getAttribute(RequestSizeFilter.TOO_LARGE)).isEqualTo(Boolean.TRUE);
  }

  @Test
  void streamAtItsEndAndListenersAreHandedThrough() throws Exception {
    RequestSizeFilter filter = new RequestSizeFilter(4);
    MockHttpServletRequest empty = request("POST", "/api/registrations", "1.1.1.1");
    empty.setContent(new byte[0]);
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(empty, new MockHttpServletResponse(), chain);
    ServletInputStream in = ((HttpServletRequest) chain.getRequest()).getInputStream();

    assertThat(in.read()).isEqualTo(-1);
    assertThat(in.read(new byte[2], 0, 2)).isEqualTo(-1);
    assertThatThrownBy(() -> in.setReadListener(new NoListener()))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThat(filter.shouldNotFilter(request("GET", "/actuator/health", "1.1.1.1"))).isTrue();
    assertThat(filter.shouldNotFilter(request("POST", "/api/registrations", "1.1.1.1"))).isFalse();
  }

  private static int status(RateLimitFilter filter, MockHttpServletRequest request)
      throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, new MockFilterChain());
    return response.getStatus();
  }

  private static final class MutableClock extends Clock {
    Instant now;

    MutableClock(Instant now) {
      this.now = now;
    }

    @Override
    public ZoneOffset getZone() {
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

  private static final class NoListener implements ReadListener {
    @Override
    public void onDataAvailable() {}

    @Override
    public void onAllDataRead() {}

    @Override
    public void onError(Throwable t) {}
  }
}
