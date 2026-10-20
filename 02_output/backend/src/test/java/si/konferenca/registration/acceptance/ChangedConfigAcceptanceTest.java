package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;

/**
 * AC-003-02: the backend started with another options file (ws-new added, ws-ai deactivated)
 * reflects it with no code change.
 */
class ChangedConfigAcceptanceTest extends AcceptanceTestSupport {

  @DynamicPropertySource
  static void settings(DynamicPropertyRegistry registry) {
    registerSettings(
        registry, Map.of("CONFERENCE_CONFIG_PATH", testConfigPath("conference-changed.json")));
  }

  @Test
  @DisplayName("AC-003-02 the form configuration reflects the changed options file")
  void ac_003_02_formConfigReflectsChangedConfiguration() throws Exception {
    JsonNode config = jsonBody(get("/api/form-config"));

    List<String> ids = new ArrayList<>();
    for (JsonNode o : config.path("options")) {
      ids.add(o.path("id").asString());
      if ("ws-new".equals(o.path("id").asString())) {
        assertThat(o.path("name").asString()).isEqualTo("New robotics workshop");
        assertThat(o.path("category").asString()).isEqualTo("WORKSHOP");
      }
    }
    assertThat(ids).contains("ws-new").doesNotContain("ws-ai", "ev-student");
  }

  @Test
  @DisplayName("AC-003-02 registrations follow the changed options file")
  void ac_003_02_registrationFollowsChangedConfiguration() throws Exception {
    Map<String, Object> body = validExternal();
    body.put("options", List.of("ws-new"));
    createdId(postRegistration(body));

    Map<String, Object> deactivated = validExternal();
    deactivated.put("options", List.of("ws-ai"));
    assertThat(errorFields(postRegistration(deactivated))).contains("options");
  }
}
