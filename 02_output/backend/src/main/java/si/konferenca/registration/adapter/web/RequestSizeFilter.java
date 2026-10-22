package si.konferenca.registration.adapter.web;

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
 * Limits request bodies on {@code /api} to {@code maxBytes} (SR-03): a declared larger body is
 * answered 413 at once; an undeclared one fails while it is read and is then answered 413 too.
 */
public class RequestSizeFilter extends OncePerRequestFilter {

  /** Request attribute set when the body exceeded the limit while being read. */
  public static final String TOO_LARGE = RequestSizeFilter.class.getName() + ".tooLarge";

  private final long maxBytes;

  public RequestSizeFilter(long maxBytes) {
    this.maxBytes = maxBytes;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getContentLengthLong() > maxBytes) {
      ErrorResponses.write(response, 413, "body", "The request is too large.");
      return;
    }
    chain.doFilter(new Limited(request, maxBytes), response);
  }

  private static final class Limited extends HttpServletRequestWrapper {

    private final long maxBytes;
    private ServletInputStream stream;

    Limited(HttpServletRequest request, long maxBytes) {
      super(request);
      this.maxBytes = maxBytes;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      if (stream == null) {
        stream = new LimitedStream(super.getInputStream(), maxBytes, this);
      }
      return stream;
    }
  }

  private static final class LimitedStream extends ServletInputStream {

    private final ServletInputStream delegate;
    private final long maxBytes;
    private final HttpServletRequest request;
    private long count;

    LimitedStream(ServletInputStream delegate, long maxBytes, HttpServletRequest request) {
      this.delegate = delegate;
      this.maxBytes = maxBytes;
      this.request = request;
    }

    @Override
    public int read() throws IOException {
      int b = delegate.read();
      if (b >= 0) {
        counted(1);
      }
      return b;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      int n = delegate.read(buffer, offset, length);
      if (n > 0) {
        counted(n);
      }
      return n;
    }

    private void counted(int n) throws IOException {
      count += n;
      if (count > maxBytes) {
        request.setAttribute(TOO_LARGE, Boolean.TRUE);
        throw new IOException("request body over the limit");
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
