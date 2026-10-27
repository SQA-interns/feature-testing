package si.konferenca.registration.acceptance.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.awaitility.Awaitility;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Client for the Mailpit HTTP API (the local SMTP substitute from environments.md). */
public final class Mailpit {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final String baseUrl;
  private final HttpClient http = HttpClient.newHttpClient();

  public Mailpit(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /** A received message as Mailpit reports it. */
  public record Message(
      String id,
      String from,
      List<String> to,
      List<String> cc,
      List<String> bcc,
      List<String> replyTo,
      String subject,
      String text,
      String html,
      List<Attachment> attachments) {}

  /** An attachment of a received message. */
  public record Attachment(String partId, String fileName, String contentType) {}

  public void deleteAll() {
    send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/messages")).DELETE());
  }

  /** All messages currently held, newest first. */
  public List<Message> messages() {
    JsonNode list = JSON.readTree(get("/api/v1/messages?limit=500"));
    List<Message> result = new ArrayList<>();
    for (JsonNode summary : list.path("messages")) {
      result.add(message(summary.path("ID").asString()));
    }
    return result;
  }

  /** Waits until exactly {@code count} messages exist and returns them. */
  public List<Message> awaitMessages(int count) {
    Awaitility.await()
        .atMost(Duration.ofSeconds(15))
        .pollInterval(Duration.ofMillis(250))
        .until(() -> total() >= count);
    return messages();
  }

  /** Messages addressed to the given address. */
  public List<Message> messagesTo(String address) {
    return messages().stream().filter(m -> m.to().contains(address)).toList();
  }

  public int total() {
    return JSON.readTree(get("/api/v1/messages?limit=1")).path("messages_count").asInt();
  }

  public Message message(String id) {
    JsonNode m = JSON.readTree(get("/api/v1/message/" + id));
    List<Attachment> attachments = new ArrayList<>();
    for (JsonNode a : m.path("Attachments")) {
      attachments.add(
          new Attachment(
              a.path("PartID").asString(),
              a.path("FileName").asString(),
              a.path("ContentType").asString()));
    }
    return new Message(
        id,
        m.path("From").path("Address").asString(),
        addresses(m.path("To")),
        addresses(m.path("Cc")),
        addresses(m.path("Bcc")),
        addresses(m.path("ReplyTo")),
        m.path("Subject").asString(),
        m.path("Text").asString(),
        m.path("HTML").asString(),
        attachments);
  }

  /** Raw header values of a message, by header name. */
  public Map<String, List<String>> headers(String id) {
    JsonNode h = JSON.readTree(get("/api/v1/message/" + id + "/headers"));
    Map<String, List<String>> result = new java.util.LinkedHashMap<>();
    for (Map.Entry<String, JsonNode> e : h.properties()) {
      List<String> values = new ArrayList<>();
      for (JsonNode v : e.getValue()) {
        values.add(v.asString());
      }
      result.put(e.getKey(), values);
    }
    return result;
  }

  public byte[] attachment(String messageId, String partId) {
    try {
      return http.send(
              HttpRequest.newBuilder(
                      URI.create(baseUrl + "/api/v1/message/" + messageId + "/part/" + partId))
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofByteArray())
          .body();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  private static List<String> addresses(JsonNode list) {
    List<String> result = new ArrayList<>();
    for (JsonNode a : list) {
      result.add(a.path("Address").asString());
    }
    return result;
  }

  private String get(String path) {
    return send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET());
  }

  private String send(HttpRequest.Builder request) {
    try {
      return http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
          .body();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }
}
