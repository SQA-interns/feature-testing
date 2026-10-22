package si.konferenca.registration.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limits request bodies (SR-03): a declared length over the limit is answered with 413; a body
 * without declared length stops being readable after the limit, so it is rejected as invalid.
 */
public class RequestSizeFilter extends OncePerRequestFilter {

  private final long maxBytes;

  public RequestSizeFilter(long maxBytes) {
    this.maxBytes = maxBytes;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getContentLengthLong() > maxBytes) {
      JsonAuthenticationEntryPoint.writeJson(
          response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Request too large.");
      return;
    }
    chain.doFilter(new LimitedRequest(request, maxBytes), response);
  }

  /** Thrown when a body without declared length grows over the limit. */
  static final class TooLargeException extends IOException {
    private static final long serialVersionUID = 1L;

    TooLargeException() {
      super("Request body too large");
    }
  }

  private static final class LimitedRequest extends HttpServletRequestWrapper {

    private final long maxBytes;
    private ServletInputStream stream;

    LimitedRequest(HttpServletRequest request, long maxBytes) {
      super(request);
      this.maxBytes = maxBytes;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      if (stream == null) {
        stream = new LimitedStream(super.getInputStream(), maxBytes);
      }
      return stream;
    }
  }

  private static final class LimitedStream extends ServletInputStream {

    private final ServletInputStream delegate;
    private final long maxBytes;
    private long read;

    LimitedStream(ServletInputStream delegate, long maxBytes) {
      this.delegate = delegate;
      this.maxBytes = maxBytes;
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

    private void count(int n) throws TooLargeException {
      read += n;
      if (read > maxBytes) {
        throw new TooLargeException();
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
