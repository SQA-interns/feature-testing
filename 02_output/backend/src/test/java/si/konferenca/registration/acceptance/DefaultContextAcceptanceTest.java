package si.konferenca.registration.acceptance;

import java.util.Map;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Acceptance tests that share one application context with the default test settings. */
abstract class DefaultContextAcceptanceTest extends AcceptanceTestSupport {

  @DynamicPropertySource
  static void settings(DynamicPropertyRegistry registry) {
    registerSettings(registry, Map.of());
  }
}
