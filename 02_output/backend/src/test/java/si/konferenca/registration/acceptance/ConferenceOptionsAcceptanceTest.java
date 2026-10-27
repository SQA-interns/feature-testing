package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import si.konferenca.registration.RegistrationApplication;
import si.konferenca.registration.acceptance.support.AcceptanceTestBase;
import si.konferenca.registration.acceptance.support.TestConferenceConfig;
import tools.jackson.databind.JsonNode;

/** US-003 Configurable conference options, through GET /api/form-config and startup. */
class ConferenceOptionsAcceptanceTest extends AcceptanceTestBase {

  @Test
  @DisplayName("AC-003-01 form configuration lists only active options with their attributes")
  void ac_003_01_formConfigurationListsOnlyActiveOptions() {
    HttpResponse<String> response = getJson("/form-config");

    assertThat(response.statusCode()).as("body: %s", response.body()).isEqualTo(200);
    JsonNode options = json(response).path("options");
    assertThat(ids(options))
        .containsExactlyInAnyOrder(
            "ws-testing", "ws-industry", "ev-dinner", "meal-lunch", "other-tour");

    JsonNode industry = byId(options, "ws-industry");
    assertThat(industry.path("name").asString()).isEqualTo("Workshop: Industry partnerships");
    assertThat(industry.path("category").asString()).isEqualTo("WORKSHOP");
    assertThat(strings(industry.path("availableTo"))).containsExactly("EXTERNAL");

    JsonNode testing = byId(options, "ws-testing");
    assertThat(testing.path("name").asString()).isEqualTo("Workshop: Testing in practice");
    assertThat(strings(testing.path("availableTo")))
        .as("availableTo defaults to both types (OQ-01)")
        .containsExactlyInAnyOrder("EXTERNAL", "STUDENT");

    assertThat(byId(options, "ev-dinner").path("category").asString()).isEqualTo("EVENT");
    assertThat(byId(options, "meal-lunch").path("category").asString()).isEqualTo("MEAL");
    assertThat(byId(options, "other-tour").path("category").asString()).isEqualTo("OTHER");
  }

  @Test
  @DisplayName("AC-003-02 a different options configuration is reflected without code change")
  void ac_003_02_changedConfigurationIsReflectedAfterRestart() throws Exception {
    Path changed =
        TestConferenceConfig.write(
            """
            {
              "options": [
                { "id": "ws-testing", "name": "Workshop: Testing in practice", "category": "WORKSHOP", "active": false },
                { "id": "ws-new", "name": "Workshop: Brand new", "category": "WORKSHOP", "active": true },
                { "id": "ev-dinner", "name": "Conference dinner", "category": "EVENT", "active": true }
              ],
              "consents": [ { "id": "privacy", "text": "%s", "mandatory": true } ]
            }
            """
                .formatted(TestConferenceConfig.PRIVACY_TEXT));

    try (ConfigurableApplicationContext app = start(changed)) {
      int otherPort =
          Integer.parseInt(app.getEnvironment().getRequiredProperty("local.server.port"));
      HttpResponse<String> response =
          send(
              HttpRequest.newBuilder(
                      URI.create("http://localhost:" + otherPort + "/api/form-config"))
                  .GET());

      assertThat(response.statusCode()).as("body: %s", response.body()).isEqualTo(200);
      assertThat(ids(json(response).path("options")))
          .containsExactlyInAnyOrder("ws-new", "ev-dinner");
    }
  }

  static Stream<Arguments> invalidConfigurations() {
    String privacy =
        "\"consents\": [ { \"id\": \"privacy\", \"text\": \"Consent\", \"mandatory\": true } ]";
    return Stream.of(
        Arguments.of(
            "duplicate identifier",
            "{ \"options\": [ { \"id\": \"ws-a\", \"name\": \"A\", \"category\": \"WORKSHOP\","
                + " \"active\": true }, { \"id\": \"ws-a\", \"name\": \"B\", \"category\":"
                + " \"EVENT\", \"active\": true } ], "
                + privacy
                + " }"),
        Arguments.of(
            "unknown category",
            "{ \"options\": [ { \"id\": \"ws-a\", \"name\": \"A\", \"category\": \"PARTY\","
                + " \"active\": true } ], "
                + privacy
                + " }"),
        Arguments.of(
            "missing name",
            "{ \"options\": [ { \"id\": \"ws-a\", \"category\": \"WORKSHOP\", \"active\": true }"
                + " ], "
                + privacy
                + " }"));
  }

  @ParameterizedTest(name = "AC-003-03 refuses to start with {0}")
  @MethodSource("invalidConfigurations")
  void ac_003_03_invalidConfigurationPreventsStartup(String problem, String content)
      throws Exception {
    Path invalid = TestConferenceConfig.write(content);

    assertThatThrownBy(
            () -> {
              try (ConfigurableApplicationContext app = start(invalid)) {
                assertThat(app.isActive()).isTrue();
              }
            })
        .as("startup with %s", problem)
        .isNotInstanceOf(AssertionError.class);
  }

  /** Starts a second backend instance with the given conference configuration file. */
  private static ConfigurableApplicationContext start(Path configFile) {
    Map<String, String> properties = baseProperties();
    properties.put("CONFERENCE_CONFIG_FILE", configFile.toString());
    properties.put("server.port", "0");
    List<String> args = new ArrayList<>();
    properties.forEach((key, value) -> args.add("--" + key + "=" + value));
    return new SpringApplicationBuilder(RegistrationApplication.class)
        .run(args.toArray(String[]::new));
  }

  private static List<String> ids(JsonNode options) {
    List<String> ids = new ArrayList<>();
    for (JsonNode o : options) {
      ids.add(o.path("id").asString());
    }
    return ids;
  }

  private static List<String> strings(JsonNode array) {
    List<String> values = new ArrayList<>();
    for (JsonNode v : array) {
      values.add(v.asString());
    }
    return values;
  }

  private static JsonNode byId(JsonNode options, String id) {
    for (JsonNode o : options) {
      if (id.equals(o.path("id").asString())) {
        return o;
      }
    }
    throw new AssertionError("option " + id + " not in the form configuration");
  }
}
