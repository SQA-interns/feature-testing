package si.konferenca.registration.domain;

import java.text.Normalizer;
import java.util.Map;

/**
 * The organizer's search text and its matching rule (BR-11, OQ-07, specification S4.9): case is
 * ignored; a letter without accent matches that letter with or without any accent, a letter with
 * accent matches only itself.
 */
public final class SearchText {

  /** Most code points a search text may have after stripping (BR-11). */
  public static final int MAX_LENGTH = 100;

  /** Letters with a mark that Unicode does not decompose, mapped to their base letter. */
  private static final Map<Integer, Integer> UNDECOMPOSED =
      Map.of(
          (int) 'đ', (int) 'd',
          (int) 'ł', (int) 'l',
          (int) 'ø', (int) 'o',
          (int) 'ħ', (int) 'h',
          (int) 'ŧ', (int) 't',
          (int) 'ı', (int) 'i');

  private final int[] needle;

  private SearchText(int[] needle) {
    this.needle = needle;
  }

  /** The text without leading and trailing whitespace; null counts as empty. */
  public static String strip(String raw) {
    return raw == null ? "" : raw.strip();
  }

  /** Whether a stripped text is within {@link #MAX_LENGTH} code points. */
  public static boolean fits(String stripped) {
    return stripped.codePointCount(0, stripped.length()) <= MAX_LENGTH;
  }

  /** The search text of a raw value, stripped. */
  public static SearchText of(String raw) {
    return new SearchText(folded(strip(raw)));
  }

  /** Whether the text is empty, so that it restricts nothing. */
  public boolean isEmpty() {
    return needle.length == 0;
  }

  /** Whether the search text is part of any of the values; an empty text matches everything. */
  public boolean matchesAny(String... values) {
    if (isEmpty()) {
      return true;
    }
    for (String value : values) {
      if (value != null && contains(folded(value))) {
        return true;
      }
    }
    return false;
  }

  private boolean contains(int[] text) {
    for (int start = 0; start + needle.length <= text.length; start++) {
      if (matchesAt(text, start)) {
        return true;
      }
    }
    return false;
  }

  private boolean matchesAt(int[] text, int start) {
    for (int i = 0; i < needle.length; i++) {
      if (!matches(needle[i], text[start + i])) {
        return false;
      }
    }
    return true;
  }

  /** One search character against one text character, both lower case. */
  static boolean matches(int searched, int found) {
    if (searched == found) {
      return true;
    }
    return base(searched) == searched && base(found) == searched;
  }

  /** The letter without its accent: the first code point of the decomposition. */
  static int base(int codePoint) {
    String decomposed = Normalizer.normalize(Character.toString(codePoint), Normalizer.Form.NFD);
    int first = decomposed.codePointAt(0);
    return UNDECOMPOSED.getOrDefault(first, first);
  }

  /** NFC code points in lower case. */
  private static int[] folded(String value) {
    return Normalizer.normalize(value, Normalizer.Form.NFC)
        .codePoints()
        .map(Character::toLowerCase)
        .toArray();
  }
}
