package si.konferenca.registration.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class ErrorHandlingTest {

  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  private static ErrorBody.Item only(ResponseEntity<ErrorBody> response) {
    assertThat(response.getBody().errors()).hasSize(1);
    return response.getBody().errors().getFirst();
  }

  @Test
  void unreadableBodyIs400UnlessItWasTooLarge() {
    HttpMessageNotReadableException e =
        new HttpMessageNotReadableException("bad", new MockHttpInputMessage(new byte[0]));
    MockHttpServletRequest request = new MockHttpServletRequest();

    ResponseEntity<ErrorBody> unreadable = handler.unreadable(e, request);
    request.setAttribute(RequestSizeFilter.TOO_LARGE, Boolean.TRUE);
    ResponseEntity<ErrorBody> tooLarge = handler.unreadable(e, request);

    assertThat(unreadable.getStatusCode().value()).isEqualTo(400);
    assertThat(only(unreadable).field()).isEqualTo("body");
    assertThat(tooLarge.getStatusCode().value()).isEqualTo(413);
    assertThat(unreadable.getHeaders().getContentType().toString()).contains("charset=UTF-8");
  }

  @Test
  void otherFailuresMapToTheirStatusWithoutDetails() {
    assertThat(
            handler.mediaType(new HttpMediaTypeNotSupportedException("x")).getStatusCode().value())
        .isEqualTo(415);
    assertThat(
            handler
                .method(new HttpRequestMethodNotSupportedException("PUT"))
                .getStatusCode()
                .value())
        .isEqualTo(405);
    assertThat(
            handler
                .notFound(new NoResourceFoundException(HttpMethod.GET, "/x", "x"))
                .getStatusCode()
                .value())
        .isEqualTo(404);
    ResponseEntity<ErrorBody> unexpected =
        handler.unexpected(new IllegalStateException("secret detail"));
    assertThat(unexpected.getStatusCode().value()).isEqualTo(500);
    assertThat(only(unexpected)).isEqualTo(new ErrorBody.Item("server", "Internal error."));
  }

  @Test
  void entryPointChallengesOnlyNonScriptClients() throws Exception {
    OrganizerAuthenticationEntryPoint entryPoint = new OrganizerAuthenticationEntryPoint();
    MockHttpServletResponse plain = new MockHttpServletResponse();
    MockHttpServletResponse script = new MockHttpServletResponse();
    MockHttpServletRequest scriptRequest = new MockHttpServletRequest();
    scriptRequest.addHeader("X-Requested-With", "XMLHttpRequest");

    entryPoint.commence(new MockHttpServletRequest(), plain, new BadCredentialsException("x"));
    entryPoint.commence(scriptRequest, script, new BadCredentialsException("x"));

    assertThat(plain.getStatus()).isEqualTo(401);
    assertThat(plain.getHeader("WWW-Authenticate")).isEqualTo("Basic realm=\"organizer\"");
    assertThat(plain.getContentType()).isEqualTo(ErrorResponses.JSON_UTF8);
    assertThat(plain.getContentAsString())
        .isEqualTo(
            "{\"errors\":[{\"field\":\"credentials\",\"message\":\"Valid organizer credentials are required.\"}]}");
    assertThat(script.getStatus()).isEqualTo(401);
    assertThat(script.getHeader("WWW-Authenticate")).isNull();
  }
}
