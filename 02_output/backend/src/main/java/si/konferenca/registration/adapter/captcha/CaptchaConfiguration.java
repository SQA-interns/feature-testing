package si.konferenca.registration.adapter.captcha;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import si.konferenca.registration.application.port.CaptchaVerifier;
import si.konferenca.registration.config.AppProperties;

/** Chooses the verifier: test mode only when the environment enables it (SR-02). */
@Configuration
public class CaptchaConfiguration {

  @Bean
  public CaptchaVerifier captchaVerifier(AppProperties properties) {
    AppProperties.Recaptcha recaptcha = properties.recaptcha();
    if (recaptcha.testMode()) {
      return new TestModeCaptchaVerifier();
    }
    return new SiteverifyCaptchaVerifier(recaptcha.verifyUrl(), recaptcha.secretKey());
  }
}
