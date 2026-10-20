package si.konferenca.registration.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import si.konferenca.registration.RegistrationApplication;

/**
 * AC-003-03: the backend refuses to start with an invalid options configuration. Each case first
 * starts the application with the acceptance settings and a valid options file (control), then with
 * the same settings and the invalid file, so a failure has no other cause.
 */
class InvalidConfigAcceptanceTest {

  static final String CONSENTS =
      "\"consents\": [{\"id\": \"privacy\", \"text\": \"I agree.\", \"mandatory\": true}]";

  @TempDir Path dir;

  static Stream<Arguments> invalidConfigurations() {
    return Stream.of(
        Arguments.of(
            "duplicate identifier",
            "{\"conferenceName\": \"K\", \"options\": ["
                + "{\"id\": \"ws-ai\", \"name\": \"AI workshop\", \"category\": \"WORKSHOP\"},"
                + "{\"id\": \"ws-ai\", \"name\": \"Other AI\", \"category\": \"EVENT\"}], "
                + CONSENTS
                + "}"),
        Arguments.of(
            "unknown category",
            "{\"conferenceName\": \"K\", \"options\": ["
                + "{\"id\": \"ws-ai\", \"name\": \"AI workshop\", \"category\": \"PARTY\"}], "
                + CONSENTS
                + "}"),
        Arguments.of(
            "missing name",
            "{\"conferenceName\": \"K\", \"options\": ["
                + "{\"id\": \"ws-ai\", \"category\": \"WORKSHOP\"}], "
                + CONSENTS
                + "}"));
  }

  @ParameterizedTest(
      name = "AC-003-03 an options file with a {0} makes the backend refuse to start")
  @MethodSource("invalidConfigurations")
  void ac_003_03_invalidConfigurationRefusesToStart(String reason, String content)
      throws Exception {
    Path valid = write("valid.json", Files.readString(Path.of(validConfigPath())));
    try (ConfigurableApplicationContext context = start(valid)) {
      assertThat(context.isRunning()).as("control start with a valid options file").isTrue();
    }
    Path invalid = write("invalid.json", content);

    Throwable failure = catchThrowable(() -> start(invalid).close());

    assertThat(failure).as("start with an options file with a %s", reason).isNotNull();
  }

  private static String validConfigPath() {
    return AcceptanceTestSupport.testConfigPath("conference-test.json");
  }

  private Path write(String name, String content) throws Exception {
    Path file = dir.resolve(name);
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return file;
  }

  private static ConfigurableApplicationContext start(Path config) {
    Map<String, Object> settings =
        AcceptanceTestSupport.settingsMap(
            Map.of("CONFERENCE_CONFIG_PATH", config.toAbsolutePath().toString()));
    settings.put("server.port", "0");
    return new SpringApplicationBuilder(RegistrationApplication.class).properties(settings).run();
  }
}
