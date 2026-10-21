package si.konferenca.registration.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limits request bodies to {@code app.request.max-bytes}: a larger {@code Content-Length} is
 * answered with HTTP 413 at once, a body without a length fails while it is read (SR-03).
 */
public class RequestSizeFilter extends OncePerRequestFilter {

  static final String BODY = "{\"message\":\"The request is too large.\"}";

  private final long maxBytes;

  public RequestSizeFilter(long maxBytes) {
    this.maxBytes = maxBytes;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getContentLengthLong() > maxBytes) {
      response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
      response.setContentType("application/json;charset=UTF-8");
      response.getOutputStream().write(BODY.getBytes(StandardCharsets.UTF_8));
      return;
    }
    chain.doFilter(new LimitedRequest(request, maxBytes), response);
  }

  /** The request with a body stream that fails after {@code maxBytes} bytes. */
  static final class LimitedRequest extends HttpServletRequestWrapper {

    private final long maxBytes;
    private final AtomicLong count = new AtomicLong();

    LimitedRequest(HttpServletRequest request, long maxBytes) {
      super(request);
      this.maxBytes = maxBytes;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      return new LimitedInputStream(super.getInputStream(), maxBytes, count);
    }

    @Override
    public BufferedReader getReader() throws IOException {
      String encoding = getCharacterEncoding();
      Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
      return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }
  }

  /** Counts the bytes read and fails once there are more than allowed. */
  static final class LimitedInputStream extends ServletInputStream {

    private final ServletInputStream delegate;
    private final long maxBytes;
    private final AtomicLong count;

    LimitedInputStream(ServletInputStream delegate, long maxBytes, AtomicLong count) {
      this.delegate = delegate;
      this.maxBytes = maxBytes;
      this.count = count;
    }

    @Override
    public int read() throws IOException {
      int b = delegate.read();
      if (b >= 0) {
        count(1);
      }
      return b;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      int n = delegate.read(buffer, offset, length);
      if (n > 0) {
        count(n);
      }
      return n;
    }

    private void count(int n) throws PayloadTooLargeException {
      if (count.addAndGet(n) > maxBytes) {
        throw new PayloadTooLargeException();
      }
    }

    @Override
    public boolean isFinished() {
      return delegate.isFinished();
    }

    @Override
    public boolean isReady() {
      return delegate.isReady();
    }

    @Override
    public void setReadListener(ReadListener listener) {
      delegate.setReadListener(listener);
    }
  }
}
