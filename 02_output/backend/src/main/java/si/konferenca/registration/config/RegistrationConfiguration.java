package si.konferenca.registration.config;

import jakarta.persistence.EntityManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.transaction.PlatformTransactionManager;
import si.konferenca.registration.adapter.captcha.RecaptchaVerifier;
import si.konferenca.registration.adapter.captcha.TestModeCaptchaVerifier;
import si.konferenca.registration.adapter.jsoncopy.FileJsonCopyStore;
import si.konferenca.registration.adapter.mail.MailNotifier;
import si.konferenca.registration.adapter.persistence.JpaRegistrationStore;
import si.konferenca.registration.adapter.persistence.RegistrationJpaRepository;
import si.konferenca.registration.adapter.persistence.SpringTransactionRunner;
import si.konferenca.registration.application.CaptchaVerifier;
import si.konferenca.registration.application.JsonCopyStore;
import si.konferenca.registration.application.Notifier;
import si.konferenca.registration.application.RegisterParticipant;
import si.konferenca.registration.application.RegistrationStore;
import si.konferenca.registration.application.TransactionRunner;
import si.konferenca.registration.domain.ConferenceCatalogue;
import si.konferenca.registration.domain.RegistrationValidator;

/** The registration use case and its adapters (US-001, US-002, US-005 … US-007). */
@Configuration
public class RegistrationConfiguration {

  static final int MAIL_TIMEOUT_MILLIS = 5000;

  @Bean
  CaptchaVerifier captchaVerifier(AppSettings settings) {
    AppSettings.Captcha captcha = settings.captcha();
    if (captcha.testMode()) {
      return new TestModeCaptchaVerifier();
    }
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    return new RecaptchaVerifier(client, URI.create(captcha.verifyUrl()), captcha.secretKey());
  }

  @Bean
  FileJsonCopyStore jsonCopyStore(AppSettings settings) {
    return new FileJsonCopyStore(Path.of(settings.jsonCopyDir()));
  }

  @Bean
  JpaRegistrationStore registrationStore(
      EntityManager entityManager, RegistrationJpaRepository repository) {
    return new JpaRegistrationStore(entityManager, repository);
  }

  @Bean
  TransactionRunner transactionRunner(PlatformTransactionManager transactionManager) {
    return new SpringTransactionRunner(transactionManager);
  }

  @Bean
  JavaMailSender mailSender(AppSettings settings) {
    AppSettings.Mail mail = settings.mail();
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(mail.host());
    sender.setPort(mail.port());
    sender.setDefaultEncoding("UTF-8");
    boolean authenticate = mail.username() != null && !mail.username().isBlank();
    if (authenticate) {
      sender.setUsername(mail.username());
      sender.setPassword(mail.password());
    }
    Properties properties = sender.getJavaMailProperties();
    properties.put("mail.smtp.auth", String.valueOf(authenticate));
    properties.put("mail.smtp.starttls.enable", String.valueOf(mail.tls()));
    properties.put("mail.smtp.starttls.required", String.valueOf(mail.tls()));
    properties.put("mail.smtp.connectiontimeout", String.valueOf(MAIL_TIMEOUT_MILLIS));
    properties.put("mail.smtp.timeout", String.valueOf(MAIL_TIMEOUT_MILLIS));
    properties.put("mail.smtp.writetimeout", String.valueOf(MAIL_TIMEOUT_MILLIS));
    return sender;
  }

  @Bean
  Notifier notifier(JavaMailSender sender, AppSettings settings, ConferenceCatalogue catalogue) {
    List<String> organizers =
        Arrays.stream(settings.organizer().emails().split(","))
            .map(String::strip)
            .filter(address -> !address.isEmpty())
            .toList();
    return new MailNotifier(
        sender,
        new MailNotifier.Settings(settings.mail().from(), settings.conferenceName(), organizers),
        catalogue);
  }

  @Bean
  RegisterParticipant registerParticipant(
      ConferenceCatalogue catalogue,
      CaptchaVerifier captchaVerifier,
      RegistrationStore registrationStore,
      JsonCopyStore jsonCopyStore,
      TransactionRunner transactionRunner,
      Notifier notifier,
      Clock clock) {
    return new RegisterParticipant(
        new RegistrationValidator(catalogue),
        captchaVerifier,
        registrationStore,
        jsonCopyStore,
        transactionRunner,
        notifier,
        clock);
  }
}
