package si.konferenca.registration.acceptance.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Calls the backend over real HTTP, as a browser or organizer client would. */
public final class HttpApi {

  public static final JsonMapper JSON = JsonMapper.builder().build();

  private final HttpClient client =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private final String baseUrl;

  public HttpApi(int port) {
    this.baseUrl = "http://127.0.0.1:" + port;
  }

  /** Sends a GET; {@code headers} are name, value pairs. */
  public Response get(String pathAndQuery, String... headers) {
    HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + pathAndQuery)).GET();
    return send(request, headers);
  }

  public Response postJson(String path, Object body, String... headers) {
    HttpRequest.Builder request =
        HttpRequest.newBuilder(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofByteArray(JSON.writeValueAsBytes(body)));
    return send(request, headers);
  }

  public Response getAsOrganizer(String pathAndQuery, String... headers) {
    String[] all = new String[headers.length + 2];
    all[0] = "Authorization";
    all[1] = basic(Settings.ORGANIZER_USERNAME, Settings.ORGANIZER_PASSWORD);
    System.arraycopy(headers, 0, all, 2, headers.length);
    return get(pathAndQuery, all);
  }

  public static String basic(String username, String password) {
    String pair = username + ":" + password;
    return "Basic " + Base64.getEncoder().encodeToString(pair.getBytes(StandardCharsets.UTF_8));
  }

  /** Builds {@code ?a=1&b=2} with UTF-8 encoding; an empty map gives an empty string. */
  public static String query(Map<String, String> parameters) {
    if (parameters.isEmpty()) {
      return "";
    }
    return parameters.entrySet().stream()
        .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
        .collect(Collectors.joining("&", "?", ""));
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private Response send(HttpRequest.Builder request, String... headers) {
    for (int i = 0; i < headers.length; i += 2) {
      request.header(headers[i], headers[i + 1]);
    }
    try {
      HttpResponse<byte[]> response =
          client.send(
              request.timeout(Duration.ofSeconds(60)).build(),
              HttpResponse.BodyHandlers.ofByteArray());
      return new Response(response.statusCode(), response.headers().map(), response.body());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  /** A received response. */
  public record Response(int status, Map<String, List<String>> headers, byte[] body) {

    public String text() {
      return new String(body, StandardCharsets.UTF_8);
    }

    public JsonNode json() {
      return JSON.readTree(body);
    }

    public String header(String name) {
      return headers.entrySet().stream()
          .filter(e -> e.getKey().equalsIgnoreCase(name))
          .flatMap(e -> e.getValue().stream())
          .findFirst()
          .orElse(null);
    }

    /** The {@code field} of every entry in the error body, or an empty list. */
    public List<String> errorFields() {
      if (body.length == 0) {
        return List.of();
      }
      JsonNode errors;
      try {
        errors = json().path("errors");
      } catch (RuntimeException notJson) {
        return List.of();
      }
      return errors.valueStream().map(e -> e.path("field").asString()).toList();
    }

    @Override
    public String toString() {
      return status + " " + text();
    }
  }
}
