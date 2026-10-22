package si.konferenca.registration.acceptance.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import tools.jackson.databind.JsonNode;

/** Reads the messages the backend sent, through the Mailpit HTTP API. */
public final class Mailpit {

  private final HttpClient client = HttpClient.newHttpClient();
  private final String baseUrl = Containers.mailpitBaseUrl();

  public void deleteAll() {
    send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/messages")).DELETE());
  }

  /** Summaries of every stored message (fields {@code ID}, {@code To}, {@code Subject}, …). */
  public List<JsonNode> messages() {
    JsonNode list = getJson("/api/v1/messages?limit=1000");
    return list.path("messages").valueStream().toList();
  }

  /** Waits up to ten seconds until at least {@code count} messages are stored. */
  public List<JsonNode> awaitMessages(int count) {
    long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
    List<JsonNode> messages = messages();
    while (messages.size() < count && System.nanoTime() < deadline) {
      pause();
      messages = messages();
    }
    return messages;
  }

  /** The full message: {@code Text}, {@code HTML}, {@code To}, {@code Cc}, {@code Bcc}, … */
  public JsonNode message(String id) {
    return getJson("/api/v1/message/" + id);
  }

  /** Header name to its values, as received. */
  public JsonNode headers(String id) {
    return getJson("/api/v1/message/" + id + "/headers");
  }

  public byte[] part(String id, String partId) {
    return send(HttpRequest.newBuilder(
                URI.create(baseUrl + "/api/v1/message/" + id + "/part/" + partId))
            .GET())
        .body();
  }

  /** Every recipient address (To, Cc, Bcc) of a full message, lower case. */
  public static List<String> recipients(JsonNode message) {
    return List.of("To", "Cc", "Bcc").stream()
        .flatMap(field -> message.path(field).valueStream())
        .map(address -> address.path("Address").asString().toLowerCase())
        .toList();
  }

  private JsonNode getJson(String path) {
    return HttpApi.JSON.readTree(
        send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET()).body());
  }

  private HttpResponse<byte[]> send(HttpRequest.Builder request) {
    try {
      return client.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  private static void pause() {
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
