package si.konferenca.registration.acceptance.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Request bodies for POST /api/registrations (openapi.yaml RegistrationRequest). */
public final class Submissions {

  /** Token the backend accepts in reCAPTCHA test mode (specification S4.5). */
  public static final String CAPTCHA_PASS = "test-pass";

  private Submissions() {}

  /** A valid external submission: all fields, the mandatory consent, a passing token. */
  public static Map<String, Object> validExternal() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "EXTERNAL");
    body.put("firstName", "Ana");
    body.put("lastName", "Novak");
    body.put("email", "ana.novak@example.org");
    body.put("organization", "Example Institute");
    body.put("optionIds", new ArrayList<>(List.of("ws-testing", "ev-dinner")));
    body.put("consents", new ArrayList<>(List.of("privacy")));
    body.put("recaptchaToken", CAPTCHA_PASS);
    return body;
  }

  /** A valid student submission: all fields, the mandatory consent, a passing token. */
  public static Map<String, Object> validStudent() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "STUDENT");
    body.put("firstName", "Luka");
    body.put("lastName", "Kranjc");
    body.put("email", "luka.kranjc@student.example.org");
    body.put("studyInstitution", "University of Ljubljana");
    body.put("studyProgramme", "Computer Science");
    body.put("studentId", "63200001");
    body.put("optionIds", new ArrayList<>(List.of("ws-testing", "meal-lunch")));
    body.put("consents", new ArrayList<>(List.of("privacy")));
    body.put("recaptchaToken", CAPTCHA_PASS);
    return body;
  }
}
