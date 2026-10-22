package si.konferenca.registration.adapter.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import tools.jackson.databind.json.JsonMapper;

/** Writes an {@link ErrorBody} directly to a servlet response (used by filters). */
public final class ErrorResponses {

  public static final String JSON_UTF8 = "application/json;charset=UTF-8";

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private ErrorResponses() {}

  public static void write(HttpServletResponse response, int status, String field, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType(JSON_UTF8);
    response.getOutputStream().write(JSON.writeValueAsBytes(ErrorBody.of(field, message)));
  }
}
