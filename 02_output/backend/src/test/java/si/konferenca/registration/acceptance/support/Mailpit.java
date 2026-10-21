package si.konferenca.registration.acceptance.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.awaitility.Awaitility;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Reads the messages caught by Mailpit through its REST API ({@code /api/v1}). */
public final class Mailpit {

  private static final Duration WAIT = Duration.ofSeconds(15);

  private final String baseUrl;
  private final HttpClient http = HttpClient.newHttpClient();
  private final JsonMapper json = JsonMapper.builder().build();

  Mailpit(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /** One caught message: its summary fields plus the full text, HTML and headers. */
  public record Message(
      String id,
      List<String> to,
      List<String> cc,
      List<String> bcc,
      String subject,
      String text,
      String html,
      List<Attachment> attachments,
      Map<String, List<String>> headers) {}

  public record Attachment(String partId, String fileName, String contentType) {}

  /** Every caught message addressed (To, Cc or Bcc) to the address. */
  public List<Message> messagesTo(String address) {
    List<Message> result = new ArrayList<>();
    for (JsonNode summary : summaries()) {
      if (addresses(summary.get("To")).contains(address)
          || addresses(summary.get("Cc")).contains(address)
          || addresses(summary.get("Bcc")).contains(address)) {
        result.add(message(summary.get("ID").asString()));
      }
    }
    return result;
  }

  /** Messages to the address whose text contains the marker (for example a registration id). */
  public List<Message> messagesTo(String address, String marker) {
    return messagesTo(address).stream().filter(m -> m.text().contains(marker)).toList();
  }

  /** Waits until at least {@code count} messages to the address contain the marker. */
  public List<Message> awaitMessagesTo(String address, String marker, int count) {
    Awaitility.await()
        .atMost(WAIT)
        .pollInterval(Duration.ofMillis(250))
        .until(() -> messagesTo(address, marker).size() >= count);
    return messagesTo(address, marker);
  }

  /** Every caught message whose text, subject or recipients contain the value. */
  public List<Message> messagesMentioning(String value) {
    List<Message> result = new ArrayList<>();
    for (JsonNode summary : summaries()) {
      Message m = message(summary.get("ID").asString());
      if (m.text().contains(value)
          || m.subject().contains(value)
          || m.to().contains(value)
          || m.cc().contains(value)
          || m.bcc().contains(value)) {
        result.add(m);
      }
    }
    return result;
  }

  /** The decoded content of an attachment. */
  public byte[] attachment(Message message, Attachment attachment) {
    return getBytes("/api/v1/message/" + message.id() + "/part/" + attachment.partId());
  }

  private List<JsonNode> summaries() {
    JsonNode page =
        json.readTree(new String(getBytes("/api/v1/messages?limit=1000"), StandardCharsets.UTF_8));
    List<JsonNode> result = new ArrayList<>();
    page.path("messages").values().forEach(result::add);
    return result;
  }

  private Message message(String id) {
    JsonNode m =
        json.readTree(new String(getBytes("/api/v1/message/" + id), StandardCharsets.UTF_8));
    JsonNode h =
        json.readTree(
            new String(getBytes("/api/v1/message/" + id + "/headers"), StandardCharsets.UTF_8));
    Map<String, List<String>> headers = new LinkedHashMap<>();
    for (Map.Entry<String, JsonNode> e : h.properties()) {
      headers.put(e.getKey(), e.getValue().values().stream().map(JsonNode::asString).toList());
    }
    List<Attachment> attachments = new ArrayList<>();
    for (JsonNode a : m.path("Attachments").values()) {
      attachments.add(
          new Attachment(
              a.get("PartID").asString(),
              a.get("FileName").asString(),
              a.get("ContentType").asString()));
    }
    return new Message(
        id,
        addresses(m.get("To")),
        addresses(m.get("Cc")),
        addresses(m.get("Bcc")),
        m.path("Subject").asString(""),
        m.path("Text").asString(""),
        m.path("HTML").asString(""),
        attachments,
        headers);
  }

  private static List<String> addresses(JsonNode list) {
    if (list == null || list.isNull()) {
      return List.of();
    }
    return list.values().stream().map(a -> a.get("Address").asString()).toList();
  }

  private byte[] getBytes(String path) {
    try {
      HttpResponse<byte[]> response =
          http.send(
              HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(),
              HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() != 200) {
        throw new IllegalStateException("Mailpit " + path + " answered " + response.statusCode());
      }
      return response.body();
    } catch (IOException e) {
      throw new IllegalStateException("Mailpit not reachable", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted", e);
    }
  }
}
