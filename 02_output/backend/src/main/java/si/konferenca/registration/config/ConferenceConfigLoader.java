package si.konferenca.registration.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import si.konferenca.registration.domain.ConferenceCatalogue;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.Consent;
import si.konferenca.registration.domain.InvalidConfigurationException;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the conference configuration file ({@code docs/02_contracts/conference-config.schema.json})
 * named by {@code CONFERENCE_CONFIG} (AR-04).
 */
final class ConferenceConfigLoader {

  private static final Set<String> TOP_LEVEL = Set.of("options", "consents");
  private static final Set<String> OPTION_FIELDS =
      Set.of("id", "name", "category", "active", "availableTo");
  private static final Set<String> CONSENT_FIELDS = Set.of("id", "text", "mandatory");

  private ConferenceConfigLoader() {}

  static ConferenceCatalogue load(ResourceLoader loader, String location) {
    Resource resource = loader.getResource(location);
    if (!resource.exists()) {
      throw new InvalidConfigurationException("conference configuration not found: " + location);
    }
    JsonNode root;
    try (InputStream in = resource.getInputStream()) {
      root = JsonMapper.builder().build().readTree(in);
    } catch (IOException | JacksonException e) {
      throw new InvalidConfigurationException("conference configuration is not valid JSON");
    }
    return parse(root);
  }

  static ConferenceCatalogue parse(JsonNode root) {
    requireObject(root, TOP_LEVEL, "configuration");
    List<ConferenceOption> options = new ArrayList<>();
    for (JsonNode node : array(root, "options")) {
      requireObject(node, OPTION_FIELDS, "option");
      String id = text(node, "id");
      String category = text(node, "category");
      options.add(
          new ConferenceOption(
              id,
              text(node, "name"),
              OptionCategory.parse(category)
                  .orElseThrow(
                      () ->
                          new InvalidConfigurationException(
                              "option " + id + " has unknown category " + category)),
              bool(node, "active"),
              availableTo(node, id)));
    }
    List<Consent> consents = new ArrayList<>();
    for (JsonNode node : array(root, "consents")) {
      requireObject(node, CONSENT_FIELDS, "consent");
      consents.add(new Consent(text(node, "id"), text(node, "text"), bool(node, "mandatory")));
    }
    return new ConferenceCatalogue(options, consents);
  }

  private static Set<RegistrationType> availableTo(JsonNode option, String id) {
    if (!option.has("availableTo")) {
      return EnumSet.allOf(RegistrationType.class);
    }
    Set<RegistrationType> types = EnumSet.noneOf(RegistrationType.class);
    for (JsonNode type : array(option, "availableTo")) {
      types.add(
          RegistrationType.parse(type.isString() ? type.asString() : null)
              .orElseThrow(
                  () ->
                      new InvalidConfigurationException(
                          "option " + id + " names an unknown registration type")));
    }
    return types;
  }

  private static void requireObject(JsonNode node, Set<String> allowed, String what) {
    if (node == null || !node.isObject()) {
      throw new InvalidConfigurationException(what + " must be a JSON object");
    }
    for (String name : node.propertyNames()) {
      if (!allowed.contains(name)) {
        throw new InvalidConfigurationException(what + " has unknown field " + name);
      }
    }
  }

  private static Iterable<JsonNode> array(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || !value.isArray()) {
      throw new InvalidConfigurationException(field + " must be a JSON array");
    }
    return value;
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || !value.isString()) {
      throw new InvalidConfigurationException("missing text field " + field);
    }
    return value.asString();
  }

  private static boolean bool(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || !value.isBoolean()) {
      throw new InvalidConfigurationException("missing boolean field " + field);
    }
    return value.asBoolean();
  }
}
