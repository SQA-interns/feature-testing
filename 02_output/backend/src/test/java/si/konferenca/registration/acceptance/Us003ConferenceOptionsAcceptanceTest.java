package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.RunningApp;
import tools.jackson.databind.JsonNode;

/** US-003 Configurable conference options (AC-003-01..03). */
class Us003ConferenceOptionsAcceptanceTest extends AcceptanceTestBase {

  private static Map<String, JsonNode> optionsById(JsonNode formConfig) {
    Map<String, JsonNode> byId = new LinkedHashMap<>();
    formConfig.path("options").forEach(o -> byId.put(o.path("id").asString(), o));
    return byId;
  }

  private static List<String> strings(JsonNode array) {
    List<String> values = new ArrayList<>();
    array.forEach(v -> values.add(v.asString()));
    return values;
  }

  @DisplayName("AC-003-01")
  @Test
  void ac_003_01_formConfigurationListsOnlyActiveOptionsWithTheirAttributes() {
    HttpResponse<byte[]> response = get("/api/form-config");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Type").orElse(""))
        .startsWith("application/json");
    Map<String, JsonNode> options = optionsById(json(response));
    assertThat(options.keySet())
        .containsExactlyInAnyOrder("ws-ai", "ws-cloud", "ev-gala", "meal-lunch", "other-tour");

    JsonNode ai = options.get("ws-ai");
    assertThat(ai.path("name").asString()).isEqualTo("AI workshop");
    assertThat(ai.path("category").asString()).isEqualTo("WORKSHOP");
    assertThat(strings(ai.path("availableTo"))).containsExactlyInAnyOrder("EXTERNAL", "STUDENT");

    JsonNode cloud = options.get("ws-cloud");
    assertThat(cloud.path("name").asString()).isEqualTo("Cloud workshop");
    assertThat(strings(cloud.path("availableTo"))).containsExactly("EXTERNAL");

    assertThat(options.get("ev-gala").path("category").asString()).isEqualTo("EVENT");
    assertThat(options.get("meal-lunch").path("category").asString()).isEqualTo("MEAL");
    assertThat(options.get("other-tour").path("category").asString()).isEqualTo("OTHER");
    assertThat(options.get("other-tour").path("name").asString()).isEqualTo("City tour");
  }

  @DisplayName("AC-003-01")
  @Test
  void ac_003_01_formConfigurationListsTheConfiguredConsents() {
    JsonNode config = json(get("/api/form-config"));

    Map<String, JsonNode> consents = new LinkedHashMap<>();
    config.path("consents").forEach(c -> consents.put(c.path("id").asString(), c));
    assertThat(consents).containsKeys("privacy", "photo");
    assertThat(consents.get("privacy").path("text").asString()).isEqualTo(PRIVACY_TEXT);
    assertThat(consents.get("privacy").path("mandatory").asBoolean()).isTrue();
    assertThat(consents.get("photo").path("mandatory").asBoolean()).isFalse();
    assertThat(config.has("captchaSiteKey")).isTrue();
  }

  // ------------------------------------------------------------ restarts with other configurations

  private static RunningApp start(String configJson) throws Exception {
    Path file = Files.createTempFile(WORK_DIR, "options", ".json");
    Files.writeString(file, configJson, StandardCharsets.UTF_8);
    return RunningApp.start(Map.of("CONFERENCE_CONFIG_FILE", file.toString()));
  }

  private static JsonNode formConfigOf(RunningApp app) {
    HttpResponse<byte[]> response =
        send(HttpRequest.newBuilder(URI.create(app.baseUrl() + "/api/form-config")).GET().build());
    assertThat(response.statusCode()).isEqualTo(200);
    return json(response);
  }

  private static final String CONSENTS =
      "\"consents\": [{\"id\": \"privacy\", \"text\": \"I agree.\", \"mandatory\": true}]";

  @DisplayName("AC-003-02")
  @Test
  void ac_003_02_restartWithChangedConfigurationIsReflectedWithoutCodeChange() throws Exception {
    String first =
        """
        {"options": [
          {"id": "ws-a", "name": "Workshop A", "category": "WORKSHOP"},
          {"id": "ev-b", "name": "Event B", "category": "EVENT"}
        ], %s}
        """
            .formatted(CONSENTS);
    String second =
        """
        {"options": [
          {"id": "ws-a", "name": "Workshop A", "category": "WORKSHOP", "active": false},
          {"id": "ev-b", "name": "Event B", "category": "EVENT"},
          {"id": "meal-c", "name": "Meal C", "category": "MEAL", "availableTo": ["STUDENT"]}
        ], %s}
        """
            .formatted(CONSENTS);

    try (RunningApp app = start(first)) {
      assertThat(optionsById(formConfigOf(app)).keySet()).containsExactlyInAnyOrder("ws-a", "ev-b");
    }
    try (RunningApp app = start(second)) {
      Map<String, JsonNode> options = optionsById(formConfigOf(app));
      assertThat(options.keySet()).containsExactlyInAnyOrder("ev-b", "meal-c");
      assertThat(options.get("meal-c").path("name").asString()).isEqualTo("Meal C");
      assertThat(options.get("meal-c").path("category").asString()).isEqualTo("MEAL");
      assertThat(strings(options.get("meal-c").path("availableTo"))).containsExactly("STUDENT");
    }
  }

  @DisplayName("AC-003-03")
  @ParameterizedTest(name = "AC-003-03 {0}")
  @ValueSource(strings = {"duplicate identifier", "unknown category", "missing name"})
  void ac_003_03_invalidConfigurationRefusesToStart(String problem) {
    String options =
        switch (problem) {
          case "duplicate identifier" ->
              """
              {"id": "ws-a", "name": "Workshop A", "category": "WORKSHOP"},
              {"id": "ws-a", "name": "Workshop A again", "category": "EVENT"}
              """;
          case "unknown category" -> "{\"id\": \"ws-a\", \"name\": \"A\", \"category\": \"PARTY\"}";
          default -> "{\"id\": \"ws-a\", \"category\": \"WORKSHOP\"}";
        };
    String config = "{\"options\": [" + options + "], " + CONSENTS + "}";

    Throwable refusal = catchThrowable(() -> start(config).close());

    assertThat(refusal)
        .as("the backend must refuse to start with a configuration with a " + problem)
        .isNotNull();
  }
}
