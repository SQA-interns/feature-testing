package si.konferenca.registration.acceptance.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Conference configuration files (docs/02_contracts/conference-config.schema.json). */
public final class TestConferenceConfig {

  public static final String PRIVACY_TEXT =
      "I agree that my personal data is processed for organizing the conference and that it is"
          + " shared with the conference organizers.";

  /**
   * Active for both types: ws-testing, ev-dinner, meal-lunch, other-tour. Active, external only:
   * ws-industry. Inactive: ws-legacy. Mandatory consent: privacy; optional consent: photos.
   */
  public static final String DEFAULT =
      """
      {
        "options": [
          { "id": "ws-testing", "name": "Workshop: Testing in practice", "category": "WORKSHOP", "active": true },
          { "id": "ws-industry", "name": "Workshop: Industry partnerships", "category": "WORKSHOP", "active": true, "availableTo": ["EXTERNAL"] },
          { "id": "ws-legacy", "name": "Workshop: Legacy systems", "category": "WORKSHOP", "active": false },
          { "id": "ev-dinner", "name": "Conference dinner", "category": "EVENT", "active": true },
          { "id": "meal-lunch", "name": "Lunch (vegetarian)", "category": "MEAL", "active": true, "availableTo": ["EXTERNAL", "STUDENT"] },
          { "id": "other-tour", "name": "City tour", "category": "OTHER", "active": true }
        ],
        "consents": [
          { "id": "privacy", "text": "%s", "mandatory": true },
          { "id": "photos", "text": "Photos of me may be published.", "mandatory": false }
        ]
      }
      """
          .formatted(PRIVACY_TEXT);

  private TestConferenceConfig() {}

  public static Path writeDefault() throws IOException {
    return write(DEFAULT);
  }

  public static Path write(String content) throws IOException {
    Path file = Files.createTempFile("conference", ".json");
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return file;
  }
}
