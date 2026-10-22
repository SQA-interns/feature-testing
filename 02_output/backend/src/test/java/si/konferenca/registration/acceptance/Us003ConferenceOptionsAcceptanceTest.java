package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import si.konferenca.registration.RegistrationApplication;
import si.konferenca.registration.acceptance.support.HttpApi;
import si.konferenca.registration.acceptance.support.Settings;
import si.konferenca.registration.acceptance.support.StandardAcceptanceTest;
import tools.jackson.databind.JsonNode;

/** US-003 Configurable conference options: {@code GET /api/form-config} and start-up checks. */
class Us003ConferenceOptionsAcceptanceTest extends StandardAcceptanceTest {

  private static final String CONSENTS =
      """
      "consents": [{"id": "privacy", "text": "I agree.", "mandatory": true}]
      """;

  @Test
  @DisplayName("AC-003-01 the form configuration lists only the active options with their fields")
  void ac003_01_formConfigurationListsOnlyActiveOptions() {
    HttpApi.Response response = api.get("/api/form-config");

    assertThat(response.status()).as("response %s", response).isEqualTo(200);
    JsonNode options = response.json().path("options");
    assertThat(options.valueStream().map(o -> o.path("id").asString()))
        .containsExactlyInAnyOrder(
            "ws-ai", "ws-leadership", "ev-dinner", "meal-lunch", "other-tour");
    JsonNode leadership = option(options, "ws-leadership");
    assertThat(leadership.path("name").asString()).isEqualTo("Leadership workshop");
    assertThat(leadership.path("category").asString()).isEqualTo("WORKSHOP");
    assertThat(strings(leadership.path("availableTo"))).containsExactly("EXTERNAL");
    JsonNode lunch = option(options, "meal-lunch");
    assertThat(lunch.path("category").asString()).isEqualTo("MEAL");
    assertThat(strings(lunch.path("availableTo"))).containsExactlyInAnyOrder("EXTERNAL", "STUDENT");
    for (JsonNode option : options) {
      assertThat(option.has("id") && option.has("name")).isTrue();
      assertThat(option.has("category") && option.has("availableTo")).isTrue();
    }
    JsonNode privacy = response.json().path("consents").get(0);
    assertThat(privacy.path("id").asString()).isEqualTo("privacy");
    assertThat(privacy.path("mandatory").asBoolean()).isTrue();
  }

  @Test
  @DisplayName("AC-003-02 a different options configuration is reflected without a code change")
  void ac003_02_differentConfigurationIsReflected() throws IOException {
    Path config =
        writeConfig(
            """
            {
              "options": [
                {"id": "ws-ai", "name": "AI workshop", "category": "WORKSHOP", "active": false},
                {"id": "ws-quantum", "name": "Quantum workshop", "category": "WORKSHOP", "active": true},
                {"id": "ev-dinner", "name": "Conference dinner", "category": "EVENT", "active": true}
              ],
            """
                + CONSENTS
                + "}");

    try (ConfigurableApplicationContext backend = start(config)) {
      HttpApi other = new HttpApi(port(backend));
      HttpApi.Response response = other.get("/api/form-config");

      assertThat(response.status()).as("response %s", response).isEqualTo(200);
      assertThat(response.json().path("options").valueStream().map(o -> o.path("id").asString()))
          .containsExactlyInAnyOrder("ws-quantum", "ev-dinner");
    }
    assertThat(api.get("/api/form-config").json().path("options").valueStream())
        .as("the running backend keeps its own configuration")
        .anyMatch(o -> o.path("id").asString().equals("ws-ai"));
  }

  @ParameterizedTest(name = "AC-003-03 the backend refuses to start with {0}")
  @ValueSource(strings = {"duplicate identifier", "unknown category", "missing name"})
  void ac003_03_invalidConfigurationPreventsStart(String defect) throws IOException {
    String option =
        switch (defect) {
          case "duplicate identifier" ->
              """
              {"id": "ws-ai", "name": "AI workshop", "category": "WORKSHOP", "active": true},
              {"id": "ws-ai", "name": "AI workshop again", "category": "WORKSHOP", "active": true}
              """;
          case "unknown category" ->
              """
              {"id": "ws-ai", "name": "AI workshop", "category": "EXCURSION", "active": true}
              """;
          default ->
              """
              {"id": "ws-ai", "category": "WORKSHOP", "active": true}
              """;
        };
    Path invalid = writeConfig("{\"options\": [" + option + "]," + CONSENTS + "}");
    Path valid =
        writeConfig(
            "{\"options\": [{\"id\": \"ws-ai\", \"name\": \"AI workshop\", \"category\":"
                + " \"WORKSHOP\", \"active\": true}],"
                + CONSENTS
                + "}");

    try (ConfigurableApplicationContext control = start(valid)) {
      assertThat(new HttpApi(port(control)).get("/api/form-config").status())
          .as("the same settings with a valid file start and serve the configuration")
          .isEqualTo(200);
    }
    assertThatThrownBy(() -> start(invalid).close()).isInstanceOf(Exception.class);
  }

  private static ConfigurableApplicationContext start(Path config) {
    Map<String, String> values = Settings.defaults(JSON_COPY_DIR);
    values.put(
        "CONFERENCE_CONFIG", "file:" + config.toAbsolutePath().toString().replace('\\', '/'));
    values.put("server.port", "0");
    values.put("spring.main.banner-mode", "off");
    return new SpringApplicationBuilder(RegistrationApplication.class)
        .run(Settings.asArguments(values));
  }

  private static int port(ConfigurableApplicationContext context) {
    return context.getEnvironment().getRequiredProperty("local.server.port", Integer.class);
  }

  private static Path writeConfig(String json) throws IOException {
    Path file = Files.createTempFile("conference-", ".json");
    Files.writeString(file, json, StandardCharsets.UTF_8);
    return file;
  }

  private static JsonNode option(JsonNode options, String id) {
    return options
        .valueStream()
        .filter(o -> o.path("id").asString().equals(id))
        .findFirst()
        .orElseThrow(() -> new AssertionError("option " + id + " missing"));
  }

  private static List<String> strings(JsonNode array) {
    return array.valueStream().map(JsonNode::asString).toList();
  }
}
