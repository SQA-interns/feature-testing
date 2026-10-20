package si.konferenca.registration.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import si.konferenca.registration.application.StorageException;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.RegistrationSubmission;
import si.konferenca.registration.domain.ValidationException;
import tools.jackson.databind.json.JsonMapper;

class WebAdapterTest {

  static final JsonMapper JSON = JsonMapper.builder().build();

  static RegistrationSubmission parse(String json) {
    return RegistrationController.parse(JSON.readTree(json));
  }

  static List<FieldError> parseErrors(String json) {
    return catchThrowableOfType(ValidationException.class, () -> parse(json)).errors();
  }

  @Test
  void parsesTextAndLists() {
    RegistrationSubmission s =
        parse(
            "{\"type\":\"STUDENT\",\"firstName\":\"A\",\"lastName\":\"B\",\"email\":\"e\","
                + "\"organization\":null,\"studyInstitution\":\"U\",\"studyProgramme\":\"P\","
                + "\"studentId\":\"1\",\"options\":[\"x\",\"y\"],\"consents\":[\"c\"],"
                + "\"captchaToken\":\"t\"}");

    assertThat(s)
        .isEqualTo(
            new RegistrationSubmission(
                "STUDENT",
                "A",
                "B",
                "e",
                null,
                "U",
                "P",
                "1",
                List.of("x", "y"),
                List.of("c"),
                "t"));
  }

  @Test
  void missingValuesAreNull() {
    RegistrationSubmission s = parse("{\"options\":null}");
    assertThat(s.type()).isNull();
    assertThat(s.options()).isNull();
    assertThat(s.consents()).isNull();
  }

  @Test
  void wrongJsonTypesAreFieldErrors() {
    assertThat(
            parseErrors(
                "{\"firstName\":1,\"options\":\"x\",\"consents\":[1],\"captchaToken\":true}"))
        .containsExactly(
            new FieldError("firstName", RegistrationController.NOT_TEXT),
            new FieldError("options", RegistrationController.NOT_LIST),
            new FieldError("consents", RegistrationController.NOT_LIST),
            new FieldError("captchaToken", RegistrationController.NOT_TEXT));
  }

  @Test
  void nonObjectBodiesAreRejected() {
    assertThat(parseErrors("[]"))
        .containsExactly(new FieldError("body", RegistrationController.NOT_OBJECT));
    assertThatThrownBy(() -> RegistrationController.parse(null))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  void errorHandlerAnswersWithoutInternals() throws Exception {
    ApiExceptionHandler handler = new ApiExceptionHandler();
    FieldError error = new FieldError("email", "bad");

    var rejected = handler.rejected(new ValidationException(List.of(error)));
    assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(rejected.getBody()).containsEntry("errors", List.of(error));

    var unreadable =
        handler.unreadable(
            new HttpMessageNotReadableException("x", new MockHttpInputMessage(new byte[0])));
    assertThat(unreadable.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(unreadable.getBody())
        .containsEntry("errors", List.of(new FieldError("body", ApiExceptionHandler.UNREADABLE)));

    assertThat(
            handler
                .unsupportedMediaType(new HttpMediaTypeNotSupportedException("x"))
                .getStatusCode())
        .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    assertThat(
            handler
                .methodNotAllowed(new HttpRequestMethodNotSupportedException("PUT"))
                .getStatusCode())
        .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(
            handler
                .notFound(new NoResourceFoundException(HttpMethod.GET, "/x", "x"))
                .getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);

    var notStored = handler.notStored(new StorageException(new IOException("disk /secret/path")));
    assertThat(notStored.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(notStored.getBody()).containsEntry("message", ApiExceptionHandler.GENERAL);

    var unexpected = handler.unexpected(new IllegalStateException("internal detail"));
    assertThat(unexpected.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(unexpected.getBody())
        .containsOnly(java.util.Map.entry("message", ApiExceptionHandler.GENERAL));
  }

  static final class CountingChain implements FilterChain {
    final List<ServletRequest> passed = new ArrayList<>();

    @Override
    public void doFilter(ServletRequest request, jakarta.servlet.ServletResponse response) {
      passed.add(request);
    }
  }

  static MockHttpServletRequest api(String address, String uri) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
    request.setRemoteAddr(address);
    return request;
  }

  @Test
  void rateLimitPerClientAndEndpointPerMinute() throws Exception {
    Clock clock = Clock.fixed(Instant.parse("2026-10-20T10:00:15Z"), ZoneOffset.UTC);
    RateLimitFilter filter = new RateLimitFilter(2, clock);
    CountingChain chain = new CountingChain();

    for (int i = 0; i < 2; i++) {
      filter.doFilter(api("1.1.1.1", "/api/form-config"), new MockHttpServletResponse(), chain);
    }
    MockHttpServletResponse limited = new MockHttpServletResponse();
    filter.doFilter(api("1.1.1.1", "/api/form-config"), limited, chain);
    filter.doFilter(api("2.2.2.2", "/api/form-config"), new MockHttpServletResponse(), chain);
    filter.doFilter(api("1.1.1.1", "/api/export"), new MockHttpServletResponse(), chain);

    assertThat(chain.passed).hasSize(4);
    assertThat(limited.getStatus()).isEqualTo(429);
    assertThat(limited.getHeader("Retry-After")).isEqualTo("45");
    assertThat(limited.getContentAsString()).isEqualTo(RateLimitFilter.BODY);
  }

  @Test
  void rateLimitWindowRestartsEachMinuteAndIgnoresOtherPaths() throws Exception {
    MutableClock clock = new MutableClock(Instant.parse("2026-10-20T10:00:59Z"));
    RateLimitFilter filter = new RateLimitFilter(1, clock);
    CountingChain chain = new CountingChain();

    filter.doFilter(api("1.1.1.1", "/api/x"), new MockHttpServletResponse(), chain);
    clock.now = Instant.parse("2026-10-20T10:01:00Z");
    filter.doFilter(api("1.1.1.1", "/api/x"), new MockHttpServletResponse(), chain);
    for (int i = 0; i < 3; i++) {
      filter.doFilter(api("1.1.1.1", "/actuator/health"), new MockHttpServletResponse(), chain);
    }

    assertThat(chain.passed).hasSize(5);
  }

  @Test
  void rateLimitForgetsOldWindowsWhenTrackingTooManyClients() throws Exception {
    MutableClock clock = new MutableClock(Instant.parse("2026-10-20T10:00:00Z"));
    RateLimitFilter filter = new RateLimitFilter(1, clock);
    CountingChain chain = new CountingChain();
    for (int i = 0; i <= RateLimitFilter.MAX_TRACKED_CLIENTS; i++) {
      filter.doFilter(
          api("10.0." + i / 256 + "." + i % 256, "/api/x"), new MockHttpServletResponse(), chain);
    }
    clock.now = Instant.parse("2026-10-20T10:01:00Z");
    MockHttpServletResponse first = new MockHttpServletResponse();
    filter.doFilter(api("10.0.0.0", "/api/x"), first, chain);
    MockHttpServletResponse again = new MockHttpServletResponse();
    filter.doFilter(api("10.0.0.0", "/api/x"), again, chain);

    assertThat(first.getStatus()).isEqualTo(200);
    assertThat(again.getStatus()).isEqualTo(429);
  }

  static final class MutableClock extends Clock {
    Instant now;

    MutableClock(Instant now) {
      this.now = now;
    }

    @Override
    public ZoneOffset getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  static MockHttpServletRequest post(byte[] body, boolean declareLength) {
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/registrations") {
          @Override
          public long getContentLengthLong() {
            return declareLength ? body.length : -1;
          }
        };
    request.setContent(body);
    request.setCharacterEncoding("UTF-8");
    return request;
  }

  @Test
  void declaredLargeBodiesAreRefused() throws Exception {
    BodySizeLimitFilter filter = new BodySizeLimitFilter(4);
    CountingChain chain = new CountingChain();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(post(new byte[5], true), response, chain);

    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(response.getContentAsString()).isEqualTo(BodySizeLimitFilter.BODY);
    assertThat(chain.passed).isEmpty();
  }

  @Test
  void undeclaredBodiesFailWhileReadingPastTheLimit() throws Exception {
    BodySizeLimitFilter filter = new BodySizeLimitFilter(4);
    CountingChain chain = new CountingChain();

    filter.doFilter(
        post("abcd".getBytes(StandardCharsets.UTF_8), false), new MockHttpServletResponse(), chain);
    filter.doFilter(
        post("abcde".getBytes(StandardCharsets.UTF_8), false),
        new MockHttpServletResponse(),
        chain);
    filter.doFilter(
        post("ab".getBytes(StandardCharsets.UTF_8), false), new MockHttpServletResponse(), chain);
    filter.doFilter(
        post("abcdef".getBytes(StandardCharsets.UTF_8), false),
        new MockHttpServletResponse(),
        chain);

    var fits = (jakarta.servlet.http.HttpServletRequest) chain.passed.get(0);
    assertThat(fits.getInputStream().readAllBytes()).containsExactly('a', 'b', 'c', 'd');
    var tooLong = (jakarta.servlet.http.HttpServletRequest) chain.passed.get(1);
    assertThatThrownBy(() -> tooLong.getInputStream().readAllBytes())
        .isInstanceOf(IOException.class);
    var reader = (jakarta.servlet.http.HttpServletRequest) chain.passed.get(2);
    assertThat(reader.getReader().readLine()).isEqualTo("ab");
    var singleBytes = (jakarta.servlet.http.HttpServletRequest) chain.passed.get(3);
    var stream = singleBytes.getInputStream();
    for (int i = 0; i < 4; i++) {
      assertThat(stream.read()).isEqualTo('a' + i);
    }
    assertThatThrownBy(stream::read).isInstanceOf(IOException.class);
    assertThat(stream.isReady()).isTrue();
  }

  @Test
  void limitedStreamReportsEndOfInput() throws Exception {
    BodySizeLimitFilter filter = new BodySizeLimitFilter(4);
    CountingChain chain = new CountingChain();
    filter.doFilter(post(new byte[0], false), new MockHttpServletResponse(), chain);

    var stream = ((jakarta.servlet.http.HttpServletRequest) chain.passed.get(0)).getInputStream();
    assertThat(stream.read()).isEqualTo(-1);
    assertThat(stream.read(new byte[2], 0, 2)).isEqualTo(-1);
    assertThat(stream.isFinished()).isTrue();
  }

  @Test
  void bodyLimitIgnoresOtherPaths() throws Exception {
    BodySizeLimitFilter filter = new BodySizeLimitFilter(1);
    CountingChain chain = new CountingChain();
    MockHttpServletRequest health = new MockHttpServletRequest("POST", "/actuator/health");
    health.setContent(new byte[10]);

    filter.doFilter(health, new MockHttpServletResponse(), chain);

    assertThat(chain.passed).containsExactly(health);
  }
}
