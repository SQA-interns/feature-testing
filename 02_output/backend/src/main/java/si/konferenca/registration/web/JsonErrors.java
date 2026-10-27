package si.konferenca.registration.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;

/** Writes an openapi.yaml {@code Error} body from a filter. */
final class JsonErrors {

  private JsonErrors() {}

  static void write(HttpServletResponse response, int status, String message) throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getWriter().write("{\"message\":\"" + message + "\"}");
  }
}
