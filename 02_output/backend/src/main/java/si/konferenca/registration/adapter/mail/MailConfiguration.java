package si.konferenca.registration.adapter.mail;

import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import si.konferenca.registration.config.AppProperties;

/** SMTP client from the settings, with 5 s timeouts (specification S4.7). */
@Configuration
public class MailConfiguration {

  private static final String TIMEOUT_MS = "5000";

  @Bean
  public JavaMailSender javaMailSender(AppProperties properties) {
    AppProperties.Mail mail = properties.mail();
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(mail.host());
    sender.setPort(mail.port());
    sender.setDefaultEncoding("UTF-8");
    boolean auth = mail.username() != null && !mail.username().isBlank();
    if (auth) {
      sender.setUsername(mail.username());
      sender.setPassword(mail.password());
    }
    Properties smtp = sender.getJavaMailProperties();
    smtp.put("mail.smtp.auth", String.valueOf(auth));
    smtp.put("mail.smtp.starttls.enable", String.valueOf(mail.starttls()));
    smtp.put("mail.smtp.starttls.required", String.valueOf(mail.starttls()));
    smtp.put("mail.smtp.connectiontimeout", TIMEOUT_MS);
    smtp.put("mail.smtp.timeout", TIMEOUT_MS);
    smtp.put("mail.smtp.writetimeout", TIMEOUT_MS);
    return sender;
  }
}
