package si.konferenca.registration.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import si.konferenca.registration.domain.ConferenceCatalog;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the conference configuration file and checks it against conference-config.schema.json; any
 * problem stops the application (AC-003-03).
 */
public final class ConferenceConfigLoader {

  private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9-]{0,63}");
  private static final Set<String> TOP = Set.of("options", "consents");
  private static final Set<String> OPTION =
      Set.of("id", "name", "category", "active", "availableTo");
  private static final Set<String> CONSENT = Set.of("id", "text", "mandatory");
  private static final int NAME_MAX = 200;
  private static final int CONSENT_TEXT_MAX = 1000;

  private final JsonMapper json = JsonMapper.builder().build();

  /**
   * Loads the catalog.
   *
   * @throws InvalidConferenceConfigException naming the file and the first problem
   */
  public ConferenceCatalog load(Path file) {
    JsonNode root;
    try {
      root = json.readTree(Files.readString(file));
    } catch (IOException | JacksonException e) {
      throw new InvalidConferenceConfigException(file, "cannot be read as JSON");
    }
    try {
      requireObject(root, "$", TOP, TOP);
      List<ConferenceOption> options = new ArrayList<>();
      JsonNode optionNodes = requireArray(root.get("options"), "options");
      for (int i = 0; i < optionNodes.size(); i++) {
        options.add(option(optionNodes.get(i), "options[" + i + "]"));
      }
      List<ConsentDefinition> consents = new ArrayList<>();
      JsonNode consentNodes = requireArray(root.get("consents"), "consents");
      for (int i = 0; i < consentNodes.size(); i++) {
        consents.add(consent(consentNodes.get(i), "consents[" + i + "]"));
      }
      return new ConferenceCatalog(options, consents);
    } catch (IllegalArgumentException e) {
      throw new InvalidConferenceConfigException(file, e.getMessage());
    }
  }

  private static ConferenceOption option(JsonNode node, String at) {
    requireObject(node, at, Set.of("id", "name", "category", "active"), OPTION);
    String id = id(node.get("id"), at + ".id");
    String name = text(node.get("name"), at + ".name", NAME_MAX);
    OptionCategory category = category(node.get("category"), at + ".category");
    boolean active = bool(node.get("active"), at + ".active");
    Set<RegistrationType> availableTo = EnumSet.allOf(RegistrationType.class);
    if (node.has("availableTo")) {
      availableTo = availableTo(node.get("availableTo"), at + ".availableTo");
    }
    return new ConferenceOption(id, name, category, active, availableTo);
  }

  private static ConsentDefinition consent(JsonNode node, String at) {
    requireObject(node, at, CONSENT, CONSENT);
    return new ConsentDefinition(
        id(node.get("id"), at + ".id"),
        text(node.get("text"), at + ".text", CONSENT_TEXT_MAX),
        bool(node.get("mandatory"), at + ".mandatory"));
  }

  private static void requireObject(
      JsonNode node, String at, Set<String> required, Set<String> allowed) {
    if (node == null || !node.isObject()) {
      throw new IllegalArgumentException(at + ": must be an object");
    }
    for (String name : required) {
      if (!node.has(name)) {
        throw new IllegalArgumentException(at + "." + name + ": is missing");
      }
    }
    for (String name : node.propertyNames()) {
      if (!allowed.contains(name)) {
        throw new IllegalArgumentException(at + "." + name + ": is not allowed");
      }
    }
  }

  private static JsonNode requireArray(JsonNode node, String at) {
    if (node == null || !node.isArray()) {
      throw new IllegalArgumentException(at + ": must be an array");
    }
    return node;
  }

  private static String id(JsonNode node, String at) {
    if (node == null || !node.isString() || !ID.matcher(node.asString()).matches()) {
      throw new IllegalArgumentException(at + ": must match " + ID.pattern());
    }
    return node.asString();
  }

  private static String text(JsonNode node, String at, int max) {
    if (node == null || !node.isString() || node.asString().isBlank()) {
      throw new IllegalArgumentException(at + ": must be a non-blank string");
    }
    String value = node.asString();
    if (value.codePointCount(0, value.length()) > max) {
      throw new IllegalArgumentException(at + ": is longer than " + max);
    }
    return value;
  }

  private static boolean bool(JsonNode node, String at) {
    if (node == null || !node.isBoolean()) {
      throw new IllegalArgumentException(at + ": must be true or false");
    }
    return node.asBoolean();
  }

  private static OptionCategory category(JsonNode node, String at) {
    if (node != null && node.isString()) {
      for (OptionCategory c : OptionCategory.values()) {
        if (c.name().equals(node.asString())) {
          return c;
        }
      }
    }
    throw new IllegalArgumentException(at + ": must be one of WORKSHOP, EVENT, MEAL, OTHER");
  }

  private static Set<RegistrationType> availableTo(JsonNode node, String at) {
    JsonNode array = requireArray(node, at);
    Set<RegistrationType> types = EnumSet.noneOf(RegistrationType.class);
    for (JsonNode value : array) {
      RegistrationType type = null;
      for (RegistrationType t : RegistrationType.values()) {
        if (value.isString() && t.name().equals(value.asString())) {
          type = t;
        }
      }
      if (type == null || !types.add(type)) {
        throw new IllegalArgumentException(at + ": unknown or repeated registration type");
      }
    }
    if (types.isEmpty()) {
      throw new IllegalArgumentException(at + ": must name at least one registration type");
    }
    return types;
  }
}
