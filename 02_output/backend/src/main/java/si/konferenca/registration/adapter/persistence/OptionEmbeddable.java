package si.konferenca.registration.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import si.konferenca.registration.domain.OptionCategory;
import si.konferenca.registration.domain.SelectedOption;

/** A row of registration_option. */
@Embeddable
public class OptionEmbeddable {

  @Column(name = "option_id", nullable = false, length = 64)
  private String optionId;

  @Column(name = "option_name", nullable = false, length = 200)
  private String optionName;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false, length = 16)
  private OptionCategory category;

  protected OptionEmbeddable() {
    // for JPA
  }

  OptionEmbeddable(SelectedOption option) {
    this.optionId = option.id();
    this.optionName = option.name();
    this.category = option.category();
  }

  SelectedOption toDomain() {
    return new SelectedOption(optionId, optionName, category);
  }
}
