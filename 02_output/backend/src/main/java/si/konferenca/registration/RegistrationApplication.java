package si.konferenca.registration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the conference registration API. */
@SpringBootApplication
public class RegistrationApplication {

  /**
   * Starts the application.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(RegistrationApplication.class, args);
  }
}
