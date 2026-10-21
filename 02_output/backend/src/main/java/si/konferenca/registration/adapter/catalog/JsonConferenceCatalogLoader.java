package si.konferenca.registration.adapter.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.ConferenceConfigException;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Reads the conference configuration file ({@code conference-config.schema.json}). */
public final class JsonConferenceCatalogLoader {

  private static final Set<String> ROOT_FIELDS = Set.of("options", "consents");
  private static final Set<String> OPTION_FIELDS =
      Set.of("id", "name", "category", "active", "availableTo");
  private static final Set<String> CONSENT_FIELDS = Set.of("id", "text", "mandatory");

  private final JsonMapper json = JsonMapper.builder().build();

  /**
   * Parses and validates the configuration.
   *
   * @param source the file content
   * @param name the location, used in error messages
   * @throws ConferenceConfigException when the file is unreadable or invalid
   */
  public ConferenceCatalog load(InputStream source, String name) {
    JsonNode root;
    try (InputStream in = source) {
      root = json.readTree(in);
    } catch (IOException | JacksonException e) {
      throw new ConferenceConfigException("Conference configuration " + name + " is unreadable", e);
    }
    requireObject(root, ROOT_FIELDS, "the configuration");
    List<ConferenceOption> options = new ArrayList<>();
    for (JsonNode option : array(root, "options")) {
      options.add(option(option));
    }
    List<ConsentDefinition> consents = new ArrayList<>();
    for (JsonNode consent : array(root, "consents")) {
      requireObject(consent, CONSENT_FIELDS, "a consent");
      consents.add(
          new ConsentDefinition(
              text(consent, "id"), text(consent, "text"), bool(consent, "mandatory", null)));
    }
    return ConferenceCatalog.of(options, consents);
  }

  private static ConferenceOption option(JsonNode node) {
    requireObject(node, OPTION_FIELDS, "an option");
    String id = text(node, "id");
    String category = text(node, "category");
    OptionCategory parsed;
    try {
      parsed = category == null ? null : OptionCategory.valueOf(category);
    } catch (IllegalArgumentException e) {
      throw new ConferenceConfigException(
          "Option '" + id + "' has the unknown category '" + category + "'", e);
    }
    Set<RegistrationType> availableTo = EnumSet.allOf(RegistrationType.class);
    if (node.has("availableTo")) {
      availableTo = EnumSet.noneOf(RegistrationType.class);
      for (JsonNode type : array(node, "availableTo")) {
        availableTo.add(registrationType(id, type));
      }
    }
    return new ConferenceOption(
        id, text(node, "name"), parsed, bool(node, "active", Boolean.TRUE), availableTo);
  }

  private static RegistrationType registrationType(String optionId, JsonNode type) {
    try {
      return RegistrationType.valueOf(type.isString() ? type.stringValue() : "");
    } catch (IllegalArgumentException e) {
      throw new ConferenceConfigException(
          "Option '" + optionId + "' names an unknown registration type " + type, e);
    }
  }

  private static void requireObject(JsonNode node, Set<String> allowed, String what) {
    if (node == null || !node.isObject()) {
      throw new ConferenceConfigException("Expected an object for " + what);
    }
    for (String field : node.propertyNames()) {
      if (!allowed.contains(field)) {
        throw new ConferenceConfigException("Unknown field '" + field + "' in " + what);
      }
    }
  }

  private static List<JsonNode> array(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || !value.isArray()) {
      throw new ConferenceConfigException("Field '" + field + "' must be an array");
    }
    return List.copyOf(value.values());
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || value.isNull()) {
      return null;
    }
    if (!value.isString()) {
      throw new ConferenceConfigException("Field '" + field + "' must be a string");
    }
    return value.stringValue();
  }

  private static boolean bool(JsonNode node, String field, Boolean fallback) {
    JsonNode value = node.get(field);
    if (value == null && fallback != null) {
      return fallback;
    }
    if (value == null || !value.isBoolean()) {
      throw new ConferenceConfigException("Field '" + field + "' must be true or false");
    }
    return value.booleanValue();
  }
}
