package si.konferenca.registration.acceptance.support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Valid submissions as defined in {@code acceptance-criteria.md} ("valid external submission",
 * "valid student submission"), for the options in {@code acceptance/conference.json}. Each call
 * returns a new mutable map, so a test can change one field.
 */
public final class Submissions {

  private Submissions() {}

  public static Map<String, Object> validExternal() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "EXTERNAL");
    body.put("firstName", "Ana");
    body.put("lastName", "Novak");
    body.put("email", "ana.novak@example.org");
    body.put("organization", "Example d.o.o.");
    body.put("studyInstitution", null);
    body.put("studyProgramme", null);
    body.put("studentId", null);
    body.put("options", new ArrayList<>(List.of("ws-ai", "ev-dinner")));
    body.put("consents", new ArrayList<>(List.of("privacy")));
    body.put("captchaToken", Settings.CAPTCHA_PASS);
    return body;
  }

  public static Map<String, Object> validStudent() {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "STUDENT");
    body.put("firstName", "Luka");
    body.put("lastName", "Horvat");
    body.put("email", "luka.horvat@student.example.org");
    body.put("organization", null);
    body.put("studyInstitution", "Univerza v Ljubljani");
    body.put("studyProgramme", "Computer Science");
    body.put("studentId", "63210001");
    body.put("options", new ArrayList<>(List.of("ws-ai", "meal-lunch")));
    body.put("consents", new ArrayList<>(List.of("privacy")));
    body.put("captchaToken", Settings.CAPTCHA_PASS);
    return body;
  }

  /** A valid submission of the given type with the given field values replaced. */
  public static Map<String, Object> with(Map<String, Object> base, Object... fieldValuePairs) {
    for (int i = 0; i < fieldValuePairs.length; i += 2) {
      base.put((String) fieldValuePairs[i], fieldValuePairs[i + 1]);
    }
    return base;
  }
}
