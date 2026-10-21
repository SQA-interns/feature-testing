package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import si.konferenca.registration.acceptance.support.AcceptanceTest;
import si.konferenca.registration.acceptance.support.TestStack;
import tools.jackson.databind.JsonNode;

/** US-003: the same backend started with another options file (one added, one deactivated). */
@AcceptanceTest
class ChangedOptionsConfigAcceptanceTest {

  @Autowired private MockMvc mvc;

  @DynamicPropertySource
  static void stack(DynamicPropertyRegistry registry) {
    TestStack.register(
        registry,
        Map.of(
            "app.conference.config-location",
            "classpath:acceptance/conference-config-changed.json"));
  }

  @Test
  void ac_003_02_formConfigReflectsAnotherConfigurationWithoutCodeChange() throws Exception {
    Map<String, JsonNode> options =
        FormConfigAcceptanceTest.optionsById(FormConfigAcceptanceTest.formConfig(mvc));

    assertThat(options).containsKey("other-hike").doesNotContainKey("ws-data");
    assertThat(options.get("other-hike").path("name").asString("")).isEqualTo("Morning hike");
    assertThat(FormConfigAcceptanceTest.strings(options.get("other-hike").path("availableTo")))
        .containsExactly("STUDENT");
    assertThat(options.keySet())
        .containsExactlyInAnyOrder(
            "ws-ai", "ev-gala", "ev-tour", "meal-lunch", "other-cert", "other-hike");
  }
}
