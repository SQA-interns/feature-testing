package si.konferenca.registration.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import si.konferenca.registration.config.AppProperties;

/**
 * Bodies over the limit get 413 (SR-03): at once from Content-Length, or while the body is read
 * when the length is not declared.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RequestSizeFilter extends OncePerRequestFilter {

  private static final String TOO_LARGE = "The request is too large.";

  private final int maxBytes;

  public RequestSizeFilter(AppProperties properties) {
    this.maxBytes = properties.maxRequestBytes();
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
      JsonErrors.write(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, TOO_LARGE);
      return;
    }
    chain.doFilter(new LimitedRequest(request, maxBytes), response);
  }

  /** Counts the bytes read and fails past the limit. */
  private static final class LimitedRequest extends HttpServletRequestWrapper {

    private final int maxBytes;

    LimitedRequest(HttpServletRequest request, int maxBytes) {
      super(request);
      this.maxBytes = maxBytes;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      ServletInputStream in = super.getInputStream();
      return new ServletInputStream() {
        private long read;

        @Override
        public int read() throws IOException {
          int b = in.read();
          if (b >= 0) {
            count(1);
          }
          return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
          int n = in.read(buffer, offset, length);
          if (n > 0) {
            count(n);
          }
          return n;
        }

        private void count(int n) throws PayloadTooLargeException {
          read += n;
          if (read > maxBytes) {
            throw new PayloadTooLargeException();
          }
        }

        @Override
        public boolean isFinished() {
          return in.isFinished();
        }

        @Override
        public boolean isReady() {
          return in.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
          in.setReadListener(listener);
        }
      };
    }
  }
}
