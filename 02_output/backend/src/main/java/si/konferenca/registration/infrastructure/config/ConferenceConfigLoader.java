package si.konferenca.registration.infrastructure.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import si.konferenca.registration.domain.ConferenceConfiguration;
import si.konferenca.registration.domain.ConferenceOption;
import si.konferenca.registration.domain.ConsentDefinition;
import si.konferenca.registration.domain.InvalidConfigurationException;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.RegistrationType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Loads the conference configuration file once at start (AR-04). Any problem in the file stops the
 * application context, so the backend refuses to start (AC-003-03).
 */
@Configuration
public class ConferenceConfigLoader {

  static final String SHIPPED = "conference-config.json";

  private static final JsonMapper STRICT =
      JsonMapper.builder()
          .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
          .build();

  record OptionEntry(
      String id,
      String name,
      OptionCategory category,
      Boolean active,
      Set<RegistrationType> availableTo) {}

  record ConsentEntry(String id, String text, Boolean mandatory) {}

  record ConfigFile(List<OptionEntry> options, List<ConsentEntry> consents) {}

  @Bean
  public ConferenceConfiguration conferenceConfiguration(AppSettings settings) throws IOException {
    String file = settings.configFile();
    if (file == null || file.isBlank()) {
      if (settings.production()) {
        throw new InvalidConfigurationException("CONFERENCE_CONFIG_FILE is required in production");
      }
      try (InputStream in = new ClassPathResource(SHIPPED).getInputStream()) {
        return parse(in.readAllBytes());
      }
    }
    return parse(Files.readAllBytes(Path.of(file)));
  }

  static ConferenceConfiguration parse(byte[] json) {
    ConfigFile config;
    try {
      config = STRICT.readValue(json, ConfigFile.class);
    } catch (JacksonException e) {
      throw new InvalidConfigurationException("unreadable file: " + e.getOriginalMessage());
    }
    if (config == null || config.options() == null || config.consents() == null) {
      throw new InvalidConfigurationException("options and consents are required");
    }
    List<ConferenceOption> options =
        config.options().stream().map(ConferenceConfigLoader::toOption).toList();
    List<ConsentDefinition> consents =
        config.consents().stream().map(ConferenceConfigLoader::toConsent).toList();
    return new ConferenceConfiguration(options, consents);
  }

  private static ConferenceOption toOption(OptionEntry entry) {
    if (entry == null) {
      throw new InvalidConfigurationException("empty option entry");
    }
    Set<RegistrationType> availableTo =
        entry.availableTo() == null
            ? EnumSet.allOf(RegistrationType.class)
            : Set.copyOf(entry.availableTo());
    return new ConferenceOption(
        entry.id(),
        entry.name(),
        entry.category(),
        entry.active() == null || entry.active(),
        availableTo);
  }

  private static ConsentDefinition toConsent(ConsentEntry entry) {
    if (entry == null) {
      throw new InvalidConfigurationException("empty consent entry");
    }
    return new ConsentDefinition(
        entry.id(), entry.text(), entry.mandatory() == null || entry.mandatory());
  }
}
