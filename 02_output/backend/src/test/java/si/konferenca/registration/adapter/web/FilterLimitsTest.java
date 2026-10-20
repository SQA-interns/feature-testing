package si.konferenca.registration.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Boundaries and responses of the SR-03 filters. */
class FilterLimitsTest {

  static MockHttpServletResponse call(RateLimitFilter filter, String address) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(
        WebAdapterTest.api(address, "/api/x"), response, new WebAdapterTest.CountingChain());
    return response;
  }

  @Test
  void requestsInTheSameMinuteShareOneWindow() throws Exception {
    var clock = new WebAdapterTest.MutableClock(Instant.parse("2026-10-20T10:00:01Z"));
    RateLimitFilter filter = new RateLimitFilter(1, clock);

    assertThat(call(filter, "1.1.1.1").getStatus()).isEqualTo(200);
    clock.now = Instant.parse("2026-10-20T10:00:02Z");
    MockHttpServletResponse limited = call(filter, "1.1.1.1");

    assertThat(limited.getStatus()).isEqualTo(429);
    assertThat(limited.getContentType()).isEqualTo("application/json;charset=UTF-8");
  }

  @Test
  void staleWindowsAreKeptUntilTooManyClientsAreTracked() throws Exception {
    var clock = new WebAdapterTest.MutableClock(Instant.parse("2026-10-20T10:00:00Z"));
    RateLimitFilter filter = new RateLimitFilter(5, clock);
    for (int i = 0; i < RateLimitFilter.MAX_TRACKED_CLIENTS; i++) {
      call(filter, "10.1." + i / 256 + "." + i % 256);
    }
    clock.now = Instant.parse("2026-10-20T10:01:00Z");

    call(filter, "10.9.9.9");
    assertThat(filter.trackedClients()).isEqualTo(RateLimitFilter.MAX_TRACKED_CLIENTS + 1);

    call(filter, "10.9.9.8");
    assertThat(filter.trackedClients()).isEqualTo(2);
  }

  @Test
  void cleanupKeepsTheCurrentMinute() throws Exception {
    var clock = new WebAdapterTest.MutableClock(Instant.parse("2026-10-20T10:00:00Z"));
    RateLimitFilter filter = new RateLimitFilter(1, clock);
    call(filter, "9.9.9.9");
    for (int i = 0; i <= RateLimitFilter.MAX_TRACKED_CLIENTS; i++) {
      call(filter, "10.2." + i / 256 + "." + i % 256);
    }

    assertThat(call(filter, "9.9.9.9").getStatus()).isEqualTo(429);
  }

  static MockHttpServletRequest declared(int length) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.setContent(new byte[length]);
    return request;
  }

  @Test
  void aBodyOfExactlyTheLimitIsAccepted() throws Exception {
    BodySizeLimitFilter filter = new BodySizeLimitFilter(4);
    WebAdapterTest.CountingChain chain = new WebAdapterTest.CountingChain();
    MockHttpServletResponse tooLarge = new MockHttpServletResponse();

    filter.doFilter(declared(4), new MockHttpServletResponse(), chain);
    filter.doFilter(declared(5), tooLarge, chain);

    assertThat(chain.passed).hasSize(1);
    assertThat(tooLarge.getContentType()).isEqualTo("application/json;charset=UTF-8");
  }

  @Test
  void zeroBytesCountTowardsTheLimit() throws Exception {
    BodySizeLimitFilter filter = new BodySizeLimitFilter(2);
    WebAdapterTest.CountingChain chain = new WebAdapterTest.CountingChain();
    filter.doFilter(WebAdapterTest.post(new byte[3], false), new MockHttpServletResponse(), chain);

    ServletInputStream stream =
        ((jakarta.servlet.http.HttpServletRequest) chain.passed.get(0)).getInputStream();
    assertThat(stream.read()).isZero();
    assertThat(stream.read()).isZero();
    assertThatThrownBy(stream::read).isInstanceOf(IOException.class);
  }

  @Test
  void readerUsesTheDeclaredCharset() throws Exception {
    BodySizeLimitFilter filter = new BodySizeLimitFilter(10);
    WebAdapterTest.CountingChain chain = new WebAdapterTest.CountingChain();
    MockHttpServletRequest latin =
        WebAdapterTest.post("é".getBytes(StandardCharsets.ISO_8859_1), false);
    latin.setCharacterEncoding("ISO-8859-1");
    filter.doFilter(latin, new MockHttpServletResponse(), chain);

    assertThat(
            ((jakarta.servlet.http.HttpServletRequest) chain.passed.get(0)).getReader().readLine())
        .isEqualTo("é");
  }

  @Test
  void streamStateComesFromTheDelegate() {
    AtomicBoolean listenerSet = new AtomicBoolean();
    ServletInputStream delegate =
        new ServletInputStream() {
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
            listenerSet.set(true);
          }

          @Override
          public int read() {
            return -1;
          }
        };
    var stream = new BodySizeLimitFilter.LimitedStream(delegate, 4, new AtomicLong());

    assertThat(stream.isFinished()).isFalse();
    assertThat(stream.isReady()).isFalse();
    stream.setReadListener(null);
    assertThat(listenerSet).isTrue();
  }
}
