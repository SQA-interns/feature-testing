package si.konferenca.registration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static si.konferenca.registration.domain.DomainFixtures.DINNER;
import static si.konferenca.registration.domain.DomainFixtures.LEGACY;
import static si.konferenca.registration.domain.DomainFixtures.PHOTOS;
import static si.konferenca.registration.domain.DomainFixtures.PRIVACY;
import static si.konferenca.registration.domain.DomainFixtures.WORKSHOP;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConferenceCatalogTest {

  @Test
  void keepsConfigurationOrderAndFiltersActiveOptions() {
    ConferenceCatalog catalog = DomainFixtures.catalog();

    assertThat(catalog.options()).containsExactly(WORKSHOP, DINNER, LEGACY);
    assertThat(catalog.activeOptions()).containsExactly(WORKSHOP, DINNER);
    assertThat(catalog.consents()).containsExactly(PRIVACY, PHOTOS);
  }

  @Test
  void findsOptionsAndConsentsById() {
    ConferenceCatalog catalog = DomainFixtures.catalog();

    assertThat(catalog.option("ev-dinner")).contains(DINNER);
    assertThat(catalog.option("ws-old")).contains(LEGACY);
    assertThat(catalog.option("nope")).isEmpty();
    assertThat(catalog.consent("photos")).contains(PHOTOS);
    assertThat(catalog.consent("nope")).isEmpty();
  }

  @Test
  void copiesTheGivenLists() {
    List<ConferenceOption> options = new ArrayList<>(List.of(WORKSHOP));
    List<ConsentDefinition> consents = new ArrayList<>(List.of(PRIVACY));
    ConferenceCatalog catalog = new ConferenceCatalog(options, consents);
    options.add(DINNER);
    consents.add(PHOTOS);

    assertThat(catalog.options()).containsExactly(WORKSHOP);
    assertThat(catalog.consents()).containsExactly(PRIVACY);
  }

  @Test
  void rejectsDuplicateOptionIds() {
    ConferenceOption twin =
        new ConferenceOption("ws-a", "Twin", OptionCategory.OTHER, true, Set.of());

    assertThatThrownBy(() -> new ConferenceCatalog(List.of(WORKSHOP, twin), List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("duplicate option id: ws-a");
  }

  @Test
  void rejectsDuplicateConsentIds() {
    ConsentDefinition twin = new ConsentDefinition("privacy", "Twin", false);

    assertThatThrownBy(() -> new ConferenceCatalog(List.of(), List.of(PRIVACY, twin)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("duplicate consent id: privacy");
  }

  @Test
  void optionAvailabilityFollowsItsTypes() {
    assertThat(DINNER.isAvailableTo(RegistrationType.EXTERNAL)).isTrue();
    assertThat(DINNER.isAvailableTo(RegistrationType.STUDENT)).isFalse();
  }
}
