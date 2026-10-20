package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** US-003 configurable options: AC-003-01 (form configuration lists only active options). */
class FormConfigAcceptanceTest extends DefaultContextAcceptanceTest {

  static final String PRIVACY_TEXT =
      "I agree that my personal data is processed for organizing the conference and that it is"
          + " shared with the conference organizers.";

  @Test
  @DisplayName("AC-003-01 the form configuration lists only active options with their attributes")
  void ac_003_01_formConfigListsOnlyActiveOptions() throws Exception {
    HttpResponse<String> response = get("/api/form-config");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Type").orElse(""))
        .startsWith("application/json");
    JsonNode config = jsonBody(response);

    Map<String, JsonNode> options = new LinkedHashMap<>();
    for (JsonNode o : config.path("options")) {
      options.put(o.path("id").asString(), o);
    }
    assertThat(options.keySet())
        .containsExactlyInAnyOrder(
            "ws-ai", "ws-pro", "ev-gala", "ev-student", "ml-lunch", "ot-tour");

    assertOption(options.get("ws-ai"), "AI workshop", "WORKSHOP", "EXTERNAL", "STUDENT");
    assertOption(options.get("ws-pro"), "Professional practice workshop", "WORKSHOP", "EXTERNAL");
    assertOption(options.get("ev-gala"), "Gala dinner", "EVENT", "EXTERNAL", "STUDENT");
    assertOption(options.get("ev-student"), "Student meetup", "EVENT", "STUDENT");
    assertOption(options.get("ml-lunch"), "Lunch", "MEAL", "EXTERNAL", "STUDENT");
    assertOption(options.get("ot-tour"), "City tour", "OTHER", "EXTERNAL", "STUDENT");
  }

  @Test
  @DisplayName("AC-003-01 the form configuration lists the configured consents and a site key")
  void ac_003_01_formConfigListsConsentsAndSiteKey() throws Exception {
    JsonNode config = jsonBody(get("/api/form-config"));

    Map<String, JsonNode> consents = new LinkedHashMap<>();
    for (JsonNode c : config.path("consents")) {
      consents.put(c.path("id").asString(), c);
    }
    assertThat(consents.keySet()).containsExactlyInAnyOrder("privacy", "photos");
    assertThat(consents.get("privacy").path("text").asString()).isEqualTo(PRIVACY_TEXT);
    assertThat(consents.get("privacy").path("mandatory").asBoolean()).isTrue();
    assertThat(consents.get("photos").path("mandatory").asBoolean()).isFalse();
    assertThat(config.has("captchaSiteKey")).isTrue();
    assertThat(config.path("captchaSiteKey").isString()).isTrue();
  }

  static void assertOption(JsonNode option, String name, String category, String... types) {
    assertThat(option.path("name").asString()).isEqualTo(name);
    assertThat(option.path("category").asString()).isEqualTo(category);
    List<String> availableTo = new ArrayList<>();
    for (JsonNode t : option.path("availableTo")) {
      availableTo.add(t.asString());
    }
    assertThat(availableTo).containsExactlyInAnyOrder(types);
  }
}
