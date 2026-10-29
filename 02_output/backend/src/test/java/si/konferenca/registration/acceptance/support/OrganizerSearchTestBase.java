package si.konferenca.registration.acceptance.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;

/** Calls GET /api/organizer/registrations (openapi.yaml searchRegistrations, CR-001). */
public abstract class OrganizerSearchTestBase extends AcceptanceTestBase {

  public static final String SEARCH_PATH = "/organizer/registrations";

  /** Query parameters in a fixed order; a null value is left out. */
  protected static Map<String, String> params(String... keyValues) {
    Map<String, String> p = new LinkedHashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      if (keyValues[i + 1] != null) {
        p.put(keyValues[i], keyValues[i + 1]);
      }
    }
    return p;
  }

  protected HttpRequest.Builder searchRequest(Map<String, String> params) {
    String query =
        params.entrySet().stream()
            .map(
                e ->
                    URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
    String uri = api(SEARCH_PATH) + (query.isEmpty() ? "" : "?" + query);
    return HttpRequest.newBuilder(java.net.URI.create(uri)).header("Accept", "application/json");
  }

  /** The search with the organizer's credentials. */
  protected HttpResponse<String> searchAsOrganizer(Map<String, String> params) {
    return send(
        searchRequest(params)
            .header("Authorization", basicAuth(ORGANIZER_USERNAME, ORGANIZER_PASSWORD))
            .GET());
  }

  /** A successful search as the organizer; fails the test on any other status. */
  protected JsonNode search(Map<String, String> params) {
    HttpResponse<String> response = searchAsOrganizer(params);
    assertThat(response.statusCode()).as("search %s: %s", params, response.body()).isEqualTo(200);
    return json(response);
  }

  /** The body holds none of the given values and no data of the default external submission. */
  protected static void assertNoRegistrationData(String body, String... values) {
    assertThat(body).doesNotContain(values);
    assertThat(body).doesNotContain("Ana", "Novak", "ana.novak@example.org", "registeredAt");
  }

  /** Ids of the listed registrations, in the order of the response. */
  protected static List<String> ids(JsonNode result) {
    List<String> ids = new ArrayList<>();
    for (JsonNode r : result.path("registrations")) {
      ids.add(r.path("id").asString());
    }
    return ids;
  }

  /** Last names of the listed registrations, in the order of the response. */
  protected static List<String> lastNames(JsonNode result) {
    List<String> names = new ArrayList<>();
    for (JsonNode r : result.path("registrations")) {
      names.add(r.path("lastName").asString());
    }
    return names;
  }

  /** Registers the body and returns the new registration's id. */
  protected String registered(Map<String, Object> body) {
    HttpResponse<String> response = postRegistration(body);
    assertThat(response.statusCode()).as("setup registration: %s", response.body()).isEqualTo(201);
    return json(response).path("id").asString();
  }

  /** A valid external submission with the given last name and a neutral, unique email. */
  protected static Map<String, Object> externalNamed(String lastName, int n) {
    Map<String, Object> body = Submissions.validExternal();
    body.put("lastName", lastName);
    body.put("email", "person" + n + "@example.org");
    return body;
  }

  /** Option id → count of one category in a result's optionCounts. */
  protected static Map<String, Integer> counts(JsonNode result, String category) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (JsonNode c : result.path("optionCounts")) {
      if (category.equals(c.path("category").asString())) {
        for (JsonNode o : c.path("options")) {
          counts.put(o.path("id").asString(), o.path("count").asInt());
        }
      }
    }
    return counts;
  }
}
