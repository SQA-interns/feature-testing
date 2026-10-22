package si.konferenca.registration.acceptance.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Acceptance tests against one shared application instance started with the base settings. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTestBase extends AcceptanceSupport {

  @LocalServerPort protected int port;

  @DynamicPropertySource
  static void registerSettings(DynamicPropertyRegistry registry) {
    baseSettings().forEach((key, value) -> registry.add(key, () -> value));
  }

  @Override
  protected String baseUrl() {
    return "http://127.0.0.1:" + port;
  }
}
