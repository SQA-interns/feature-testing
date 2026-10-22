package si.konferenca.registration.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Answers 401 with a JSON body and no registration data. The Basic challenge header is left out for
 * requests from the organizer page (X-Requested-With), so the browser shows no login dialog.
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    if (request.getHeader("X-Requested-With") == null) {
      response.setHeader("WWW-Authenticate", "Basic realm=\"organizer\"");
    }
    writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
  }

  static void writeJson(HttpServletResponse response, int status, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getWriter().write("{\"message\":\"" + message + "\"}");
  }
}
