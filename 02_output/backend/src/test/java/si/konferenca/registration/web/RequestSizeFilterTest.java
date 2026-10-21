package si.konferenca.registration.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestSizeFilterTest {

  private final RequestSizeFilter filter = new RequestSizeFilter(10);

  private ServletRequest passThrough(MockHttpServletRequest request) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();
    filter.doFilter(request, response, chain);
    assertThat(response.getStatus()).isEqualTo(200);
    return chain.getRequest();
  }

  @Test
  void answers413WhenTheDeclaredLengthIsTooLarge() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.setContent(new byte[11]);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
    assertThat(response.getContentAsString()).isEqualTo(RequestSizeFilter.BODY);
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void passesABodyOfExactlyTheLimit() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/registrations");
    request.setContent("0123456789".getBytes(StandardCharsets.UTF_8));

    ServletRequest passed = passThrough(request);

    assertThat(passed.getInputStream().readAllBytes()).hasSize(10);
  }

  @Test
  void failsWhileReadingABodyWithoutLengthThatIsTooLarge() throws Exception {
    MockHttpServletRequest request = chunked("01234567890");

    ServletRequest passed = passThrough(request);

    assertThatThrownBy(() -> passed.getInputStream().readAllBytes())
        .isInstanceOf(PayloadTooLargeException.class)
        .hasMessage("Request body too large");
  }

  @Test
  void countsSingleBytesAndEveryStreamOfTheRequestTogether() throws Exception {
    ServletRequest passed = passThrough(chunked("012345678901"));

    ServletInputStream first = passed.getInputStream();
    for (int i = 0; i < 6; i++) {
      assertThat(first.read()).isEqualTo('0' + i);
    }
    ServletInputStream second = passed.getInputStream();
    assertThat(second.read(new byte[4], 0, 4)).isEqualTo(4);
    assertThatThrownBy(second::read).isInstanceOf(PayloadTooLargeException.class);
  }

  @Test
  void readerUsesTheRequestEncodingOrUtf8() throws Exception {
    MockHttpServletRequest latin = chunked("Ž");
    latin.setContent("é".getBytes(StandardCharsets.ISO_8859_1));
    latin.setCharacterEncoding("ISO-8859-1");
    try (BufferedReader reader = passThrough(latin).getReader()) {
      assertThat(reader.readLine()).isEqualTo("é");
    }

    MockHttpServletRequest utf8 = chunked("Žš");
    utf8.setCharacterEncoding((String) null);
    try (BufferedReader reader = passThrough(utf8).getReader()) {
      assertThat(reader.readLine()).isEqualTo("Žš");
    }
  }

  @Test
  void limitedStreamDelegatesItsState() throws Exception {
    AtomicBoolean listenerSet = new AtomicBoolean();
    ServletInputStream delegate =
        new ServletInputStream() {
          private final ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {1});

          @Override
          public boolean isFinished() {
            return in.available() == 0;
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
            return in.read();
          }
        };
    RequestSizeFilter.LimitedInputStream stream =
        new RequestSizeFilter.LimitedInputStream(delegate, 5, new AtomicLong());

    assertThat(stream.isFinished()).isFalse();
    assertThat(stream.isReady()).isFalse();
    stream.setReadListener(null);
    assertThat(listenerSet).isTrue();
    assertThat(stream.read()).isEqualTo(1);
    assertThat(stream.read()).isEqualTo(-1);
    assertThat(stream.read(new byte[2], 0, 2)).isEqualTo(-1);
    assertThat(stream.isFinished()).isTrue();
  }

  private static MockHttpServletRequest chunked(String body) {
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/registrations") {
          @Override
          public long getContentLengthLong() {
            return -1;
          }
        };
    request.setContent(body.getBytes(StandardCharsets.UTF_8));
    request.setCharacterEncoding("UTF-8");
    return request;
  }
}
