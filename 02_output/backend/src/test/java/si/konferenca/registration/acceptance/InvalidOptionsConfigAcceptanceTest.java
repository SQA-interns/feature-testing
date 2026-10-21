package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import si.konferenca.registration.RegistrationApplication;
import si.konferenca.registration.acceptance.support.TestStack;

/**
 * US-003: the backend refuses to start with an invalid options configuration. The settings are
 * passed as command-line arguments, which take precedence over every configuration file.
 */
class InvalidOptionsConfigAcceptanceTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "invalid-duplicate-id.json",
        "invalid-unknown-category.json",
        "invalid-missing-name.json"
      })
  void ac_003_03_backendRefusesToStartWithInvalidOptionsConfiguration(String file) {
    String[] args = arguments("classpath:acceptance/" + file);

    assertThatThrownBy(() -> start(args).close()).isInstanceOf(Exception.class);
  }

  @Test
  void ac_003_03_sameSettingsWithValidConfigurationStart() {
    try (ConfigurableApplicationContext context = start(arguments(TestStack.DEFAULT_CONFIG))) {
      assertThat(context.isRunning()).isTrue();
    }
  }

  private static ConfigurableApplicationContext start(String[] args) {
    return new SpringApplicationBuilder(RegistrationApplication.class).profiles("test").run(args);
  }

  private static String[] arguments(String configLocation) {
    Map<String, Object> properties =
        TestStack.properties(
            Map.of("app.conference.config-location", configLocation, "server.port", "0"));
    return properties.entrySet().stream()
        .map(e -> "--" + e.getKey() + "=" + e.getValue())
        .toArray(String[]::new);
  }
}
