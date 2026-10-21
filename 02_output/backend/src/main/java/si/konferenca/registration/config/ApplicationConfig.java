package si.konferenca.registration.config;

import java.nio.file.Path;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import si.konferenca.registration.adapter.captcha.GoogleCaptchaVerifier;
import si.konferenca.registration.adapter.captcha.TestModeCaptchaVerifier;
import si.konferenca.registration.adapter.export.PoiWorkbookWriter;
import si.konferenca.registration.adapter.jsoncopy.FileJsonCopyStore;
import si.konferenca.registration.adapter.mail.SmtpRegistrationNotifier;
import si.konferenca.registration.application.CaptchaVerifier;
import si.konferenca.registration.application.ExportRegistrations;
import si.konferenca.registration.application.FormConfiguration;
import si.konferenca.registration.application.JsonCopyStore;
import si.konferenca.registration.application.RegisterParticipant;
import si.konferenca.registration.application.RegistrationNotifier;
import si.konferenca.registration.application.RegistrationRepository;
import si.konferenca.registration.application.RegistrationStorage;
import si.konferenca.registration.application.WorkbookWriter;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.RegistrationValidator;

/** Wires the use cases to their ports (composition root). */
@Configuration
public class ApplicationConfig {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  public FormConfiguration formConfiguration(ConferenceCatalog catalog, AppProperties properties) {
    return new FormConfiguration(
        catalog, properties.captcha().siteKey(), properties.captcha().testMode());
  }

  @Bean
  public RegistrationValidator registrationValidator(ConferenceCatalog catalog) {
    return new RegistrationValidator(catalog);
  }

  @Bean
  public CaptchaVerifier captchaVerifier(AppProperties properties) {
    AppProperties.Captcha captcha = properties.captcha();
    if (captcha.testMode()) {
      return new TestModeCaptchaVerifier();
    }
    return new GoogleCaptchaVerifier(captcha.verifyUrl(), captcha.secretKey());
  }

  @Bean
  public JsonCopyStore jsonCopyStore(AppProperties properties) {
    return new FileJsonCopyStore(Path.of(properties.jsonCopy().directory()));
  }

  @Bean
  public RegistrationStorage registrationStorage(
      RegistrationRepository repository, JsonCopyStore jsonCopyStore) {
    return new RegistrationStorage(repository, jsonCopyStore);
  }

  @Bean
  public RegistrationNotifier registrationNotifier(
      JavaMailSender mailSender, AppProperties properties, ConferenceCatalog catalog) {
    return new SmtpRegistrationNotifier(
        mailSender,
        properties.mail().from(),
        properties.conference().name(),
        properties.organizer().emailList(),
        catalog);
  }

  @Bean
  public WorkbookWriter workbookWriter() {
    return new PoiWorkbookWriter();
  }

  @Bean
  public ExportRegistrations exportRegistrations(
      RegistrationRepository repository, WorkbookWriter workbookWriter) {
    return new ExportRegistrations(repository, workbookWriter);
  }

  @Bean
  public RegisterParticipant registerParticipant(
      RegistrationValidator validator,
      CaptchaVerifier captchaVerifier,
      RegistrationStorage storage,
      RegistrationNotifier notifier,
      Clock clock) {
    return new RegisterParticipant(validator, captchaVerifier, storage, notifier, clock);
  }
}
