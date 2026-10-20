package si.konferenca.registration.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import si.konferenca.registration.domain.ConferenceConfig;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the conference configuration file (docs/02_contracts/conference-config.schema.json). Any
 * deviation from the schema makes loading fail, so the backend refuses to start (AC-003-03).
 */
public final class ConferenceConfigLoader {

  static final String CLASSPATH_PREFIX = "classpath:";
  private static final Pattern ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,63}");
  private static final JsonMapper MAPPER =
      JsonMapper.builder()
          .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .build();

  private ConferenceConfigLoader() {}

  /** Loads {@code location}: {@code classpath:<name>} or a file system path. */
  public static ConferenceConfig load(String location) {
    if (location == null || location.isBlank()) {
      throw new InvalidConferenceConfigException("no conference configuration path is set");
    }
    return parse(read(location.strip()));
  }

  static byte[] read(String location) {
    try {
      if (location.startsWith(CLASSPATH_PREFIX)) {
        String name = location.substring(CLASSPATH_PREFIX.length()).replaceFirst("^/", "");
        try (InputStream in =
            ConferenceConfigLoader.class.getClassLoader().getResourceAsStream(name)) {
          if (in == null) {
            throw new InvalidConferenceConfigException("configuration not found: " + location);
          }
          return in.readAllBytes();
        }
      }
      return Files.readAllBytes(Path.of(location));
    } catch (IOException e) {
      throw new UncheckedIOException("cannot read the conference configuration", e);
    }
  }

  static ConferenceConfig parse(byte[] content) {
    ConfigFile file;
    try {
      file = MAPPER.readValue(content, ConfigFile.class);
    } catch (JacksonException e) {
      throw new InvalidConferenceConfigException("not a valid configuration file", e);
    }
    if (file == null || file.options() == null || file.consents() == null) {
      throw new InvalidConferenceConfigException(
          "conferenceName, options and consents are required");
    }
    List<ConferenceOption> options = new ArrayList<>();
    for (OptionEntry entry : file.options()) {
      options.add(toOption(entry));
    }
    List<ConsentDefinition> consents = new ArrayList<>();
    for (ConsentEntry entry : file.consents()) {
      consents.add(toConsent(entry));
    }
    try {
      return new ConferenceConfig(file.conferenceName(), options, consents);
    } catch (IllegalArgumentException e) {
      throw new InvalidConferenceConfigException(e.getMessage(), e);
    }
  }

  private static ConferenceOption toOption(OptionEntry entry) {
    if (entry == null) {
      throw new InvalidConferenceConfigException("empty option entry");
    }
    String id = requireId(entry.id(), "option");
    String name = requireText(entry.name(), "name of option " + id);
    OptionCategory category = parseEnum(OptionCategory.class, entry.category(), "category", id);
    Set<RegistrationType> availableTo = EnumSet.allOf(RegistrationType.class);
    if (entry.availableTo() != null) {
      if (entry.availableTo().isEmpty()) {
        throw new InvalidConferenceConfigException("availableTo of option " + id + " is empty");
      }
      availableTo = EnumSet.noneOf(RegistrationType.class);
      for (String type : entry.availableTo()) {
        if (!availableTo.add(parseEnum(RegistrationType.class, type, "availableTo", id))) {
          throw new InvalidConferenceConfigException("availableTo of option " + id + " repeats");
        }
      }
    }
    boolean active = entry.active() == null || entry.active();
    return new ConferenceOption(id, name, category, active, availableTo);
  }

  private static ConsentDefinition toConsent(ConsentEntry entry) {
    if (entry == null) {
      throw new InvalidConferenceConfigException("empty consent entry");
    }
    String id = requireId(entry.id(), "consent");
    String text = requireText(entry.text(), "text of consent " + id);
    if (entry.mandatory() == null) {
      throw new InvalidConferenceConfigException("mandatory of consent " + id + " is missing");
    }
    return new ConsentDefinition(id, text, entry.mandatory());
  }

  private static String requireId(String id, String what) {
    if (id == null || !ID.matcher(id).matches()) {
      throw new InvalidConferenceConfigException(what + " id is missing or invalid");
    }
    return id;
  }

  private static String requireText(String text, String what) {
    if (text == null || text.isBlank()) {
      throw new InvalidConferenceConfigException(what + " is missing");
    }
    return text;
  }

  private static <E extends Enum<E>> E parseEnum(
      Class<E> type, String value, String field, String optionId) {
    if (value != null) {
      for (E constant : type.getEnumConstants()) {
        if (constant.name().equals(value)) {
          return constant;
        }
      }
    }
    throw new InvalidConferenceConfigException(
        "unknown " + field + " '" + value + "' of option " + optionId);
  }

  record ConfigFile(
      String conferenceName, List<OptionEntry> options, List<ConsentEntry> consents) {}

  record OptionEntry(
      String id, String name, String category, Boolean active, List<String> availableTo) {}

  record ConsentEntry(String id, String text, Boolean mandatory) {}
}
