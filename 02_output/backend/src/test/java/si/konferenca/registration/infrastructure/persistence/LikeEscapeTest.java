package si.konferenca.registration.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The search text is matched literally: LIKE wildcards are escaped (SB-05). */
class LikeEscapeTest {

  @Test
  void escapesBackslashPercentAndUnderscore() {
    assertThat(JpaRegistrationStore.escapeLike("a\\b%c_d")).isEqualTo("a\\\\b\\%c\\_d");
  }

  @Test
  void leavesOtherTextUnchanged() {
    assertThat(JpaRegistrationStore.escapeLike("čšž abc")).isEqualTo("čšž abc");
  }
}
