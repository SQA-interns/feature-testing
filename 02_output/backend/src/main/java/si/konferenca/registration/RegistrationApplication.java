package si.konferenca.registration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the conference registration API. */
@SpringBootApplication
@SuppressWarnings("PMD.UseUtilityClass") // Spring instantiates the configuration class
public class RegistrationApplication {

  public static void main(String[] args) {
    SpringApplication.run(RegistrationApplication.class, args);
  }
}
