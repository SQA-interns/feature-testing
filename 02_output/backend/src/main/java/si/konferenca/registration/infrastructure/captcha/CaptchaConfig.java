package si.konferenca.registration.infrastructure.captcha;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import si.konferenca.registration.application.CaptchaVerifier;
import si.konferenca.registration.infrastructure.config.AppSettings;

/** Chooses the captcha verifier: test mode only when configured (SR-02). */
@Configuration
public class CaptchaConfig {

  @Bean
  public CaptchaVerifier captchaVerifier(AppSettings settings) {
    AppSettings.Captcha captcha = settings.captcha();
    if (captcha.testMode()) {
      return new TestModeCaptchaVerifier();
    }
    return RecaptchaVerifier.create(captcha.verifyUrl(), captcha.secretKey());
  }
}
