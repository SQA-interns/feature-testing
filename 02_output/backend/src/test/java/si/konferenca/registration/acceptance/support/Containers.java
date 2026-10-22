package si.konferenca.registration.acceptance.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** PostgreSQL and Mailpit, started once per test JVM and shared by every acceptance test. */
public final class Containers {

  public static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:16.15-alpine"));

  public static final int SMTP_PORT = 1025;
  public static final int MAILPIT_HTTP_PORT = 8025;

  @SuppressWarnings("resource")
  public static final GenericContainer<?> MAILPIT =
      new GenericContainer<>(DockerImageName.parse("axllent/mailpit:v1.31.1"))
          .withExposedPorts(SMTP_PORT, MAILPIT_HTTP_PORT)
          .waitingFor(Wait.forHttp("/readyz").forPort(MAILPIT_HTTP_PORT));

  static {
    POSTGRES.start();
    MAILPIT.start();
  }

  private Containers() {}

  public static String mailpitBaseUrl() {
    return "http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(MAILPIT_HTTP_PORT);
  }
}
