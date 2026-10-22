package si.konferenca.registration.domain;

/**
 * A validated organizer search (BR-10): trimmed text (empty matches all), optional type and option
 * filters, page number from 1.
 */
public record SearchCriteria(String text, RegistrationType type, String optionId, int page) {

  public static final int PAGE_SIZE = 20;
  public static final int TEXT_MAX = 100;
}
