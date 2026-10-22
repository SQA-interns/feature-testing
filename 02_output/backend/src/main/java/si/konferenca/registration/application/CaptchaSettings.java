package si.konferenca.registration.application;

/** What the registration form needs to know about the captcha (SR-01, AR-07). */
public record CaptchaSettings(boolean testMode, String siteKey) {}
