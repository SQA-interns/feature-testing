package si.konferenca.registration.application;

/** Settings shown in the form configuration (specification S3.1, S4.5). */
public record FormSettings(String conferenceName, boolean captchaTestMode, String captchaSiteKey) {}
