package si.konferenca.registration.acceptance.support;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import tools.jackson.databind.JsonNode;

/** Minimal client of the Mailpit HTTP API (v1) used to observe the emails the backend sends. */
public final class Mailpit {

  private final String baseUrl;

  public Mailpit(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  private JsonNode getJson(String path) {
    HttpResponse<byte[]> response =
        AcceptanceSupport.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build());
    if (response.statusCode() != 200) {
      throw new IllegalStateException("Mailpit " + path + " -> " + response.statusCode());
    }
    return AcceptanceSupport.json(response);
  }

  /** Summaries of all stored messages (newest first). */
  public List<JsonNode> all() {
    List<JsonNode> messages = new ArrayList<>();
    getJson("/api/v1/messages?limit=1000").path("messages").forEach(messages::add);
    return messages;
  }

  /** Every address of the To, Cc and Bcc lists of a message summary or message. */
  public static List<String> recipients(JsonNode message) {
    List<String> addresses = new ArrayList<>();
    for (String list : List.of("To", "Cc", "Bcc")) {
      message.path(list).forEach(a -> addresses.add(a.path("Address").asString().toLowerCase()));
    }
    return addresses;
  }

  /** Summaries of messages that list the address as a recipient. */
  public List<JsonNode> messagesTo(String address) {
    return all().stream().filter(m -> recipients(m).contains(address.toLowerCase())).toList();
  }

  /** Waits up to 15 seconds until at least {@code count} summaries match. */
  public List<JsonNode> await(Predicate<JsonNode> predicate, int count) {
    Instant deadline = Instant.now().plus(Duration.ofSeconds(15));
    List<JsonNode> matches = List.of();
    while (Instant.now().isBefore(deadline)) {
      matches = all().stream().filter(predicate).toList();
      if (matches.size() >= count) {
        return matches;
      }
      AcceptanceSupport.sleep(Duration.ofMillis(250));
    }
    return matches;
  }

  public List<JsonNode> awaitTo(String address, int count) {
    return await(m -> recipients(m).contains(address.toLowerCase()), count);
  }

  /**
   * Waits up to 15 seconds for a message to {@code address} whose full content matches, and returns
   * the full message, or null.
   */
  public JsonNode awaitMessageTo(String address, Predicate<JsonNode> fullMessage) {
    Instant deadline = Instant.now().plus(Duration.ofSeconds(15));
    while (Instant.now().isBefore(deadline)) {
      for (JsonNode summary : messagesTo(address)) {
        JsonNode full = message(summary.path("ID").asString());
        if (fullMessage.test(full)) {
          return full;
        }
      }
      AcceptanceSupport.sleep(Duration.ofMillis(300));
    }
    return null;
  }

  /** The full message: Text, HTML, To, Cc, Bcc, Subject, Attachments. */
  public JsonNode message(String id) {
    return getJson("/api/v1/message/" + id);
  }

  /** All headers of a message as name to list of values. */
  public JsonNode headers(String id) {
    return getJson("/api/v1/message/" + id + "/headers");
  }

  /** Raw bytes of one attachment part. */
  public byte[] part(String messageId, String partId) {
    HttpResponse<byte[]> response =
        AcceptanceSupport.send(
            HttpRequest.newBuilder(
                    URI.create(baseUrl + "/api/v1/message/" + messageId + "/part/" + partId))
                .GET()
                .build());
    if (response.statusCode() != 200) {
      throw new IllegalStateException("Mailpit part -> " + response.statusCode());
    }
    return response.body();
  }
}
