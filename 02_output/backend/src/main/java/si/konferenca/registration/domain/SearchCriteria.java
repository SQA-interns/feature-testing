package si.konferenca.registration.domain;

/**
 * A validated organizer search (US-009, BR-10): the trimmed text (empty matches every
 * registration), the type and option filters ({@code null} when not given) and the page from 1.
 */
public record SearchCriteria(String text, RegistrationType type, String optionId, int page) {

  /** Registrations per page (BR-10). */
  public static final int PAGE_SIZE = 20;

  /** Number of matching registrations before this page. */
  public int offset() {
    return (page - 1) * PAGE_SIZE;
  }
}
