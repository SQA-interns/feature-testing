package si.konferenca.registration.acceptance.support;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import si.konferenca.registration.RegistrationApplication;

/** An application instance started with the base settings plus overrides, on a free port. */
public final class RunningApp implements AutoCloseable {

  private final ConfigurableApplicationContext context;

  private RunningApp(ConfigurableApplicationContext context) {
    this.context = context;
  }

  /**
   * Starts the application. Settings are passed as command-line arguments so that they take
   * precedence over the application's own configuration files, like environment variables do.
   */
  public static RunningApp start(Map<String, String> overrides) {
    Map<String, String> settings = new HashMap<>(AcceptanceSupport.baseSettings());
    settings.putAll(overrides);
    settings.put("server.port", "0");
    String[] args =
        settings.entrySet().stream()
            .map(e -> "--" + e.getKey() + "=" + e.getValue())
            .toArray(String[]::new);
    return new RunningApp(new SpringApplicationBuilder(RegistrationApplication.class).run(args));
  }

  public String baseUrl() {
    return "http://127.0.0.1:" + context.getEnvironment().getProperty("local.server.port");
  }

  @Override
  public void close() {
    context.close();
  }
}
