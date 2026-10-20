package si.konferenca.registration.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import si.konferenca.registration.adapter.captcha.RecaptchaHttpVerifier;
import si.konferenca.registration.adapter.captcha.TestModeCaptchaVerifier;
import si.konferenca.registration.adapter.jsoncopy.FileJsonCopyStore;
import si.konferenca.registration.adapter.mail.MailRegistrationNotifier;
import si.konferenca.registration.application.CaptchaVerifier;
import si.konferenca.registration.application.JsonCopyStore;
import si.konferenca.registration.application.RegistrationNotifier;
import si.konferenca.registration.application.RegistrationRepository;
import si.konferenca.registration.application.RegistrationService;
import si.konferenca.registration.domain.ConferenceConfig;
import si.konferenca.registration.domain.RegistrationValidator;

/** Wires the registration use case to its adapters. */
@Configuration
public class RegistrationConfiguration {

  @Bean
  RegistrationValidator registrationValidator(ConferenceConfig conference) {
    return new RegistrationValidator(conference);
  }

  /** Test mode only when configured (SR-02); otherwise the Google verify endpoint (SR-01). */
  @Bean
  CaptchaVerifier captchaVerifier(AppProperties properties) {
    AppProperties.Captcha captcha = properties.captcha();
    if (captcha.testMode()) {
      return new TestModeCaptchaVerifier();
    }
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    return new RecaptchaHttpVerifier(URI.create(captcha.verifyUrl()), captcha.secretKey(), client);
  }

  @Bean
  JsonCopyStore jsonCopyStore(AppProperties properties) {
    return new FileJsonCopyStore(Path.of(properties.jsonCopyDir()));
  }

  @Bean
  RegistrationNotifier registrationNotifier(
      JavaMailSender sender, ConferenceConfig conference, AppProperties properties) {
    return new MailRegistrationNotifier(
        sender, conference, properties.mail().from(), properties.organizer().emails());
  }

  /** SMTP authentication only when an account is configured (production). */
  @Bean
  SmartInitializingSingleton smtpAuthentication(JavaMailSender sender) {
    return () -> {
      if (sender instanceof JavaMailSenderImpl impl) {
        String username = impl.getUsername();
        if (username != null && !username.isBlank()) {
          impl.getJavaMailProperties().put("mail.smtp.auth", "true");
        }
      }
    };
  }

  @Bean
  RegistrationService registrationService(
      RegistrationValidator validator,
      CaptchaVerifier captcha,
      RegistrationRepository repository,
      JsonCopyStore jsonCopies,
      RegistrationNotifier notifier,
      PlatformTransactionManager transactionManager,
      Clock clock) {
    return new RegistrationService(
        validator,
        captcha,
        repository,
        jsonCopies,
        notifier,
        new TransactionTemplate(transactionManager),
        clock);
  }
}
