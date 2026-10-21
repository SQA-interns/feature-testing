package si.konferenca.registration.adapter.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Repository;
import si.konferenca.registration.application.RegistrationRepository;
import si.konferenca.registration.application.RegistrationSearch;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.RegistrationPage;
import si.konferenca.registration.domain.SearchCriteria;

/** PostgreSQL storage of registrations through JPA (BR-07, AC-005-01) and the search (US-009). */
@Repository
class JpaRegistrationRepository implements RegistrationRepository, RegistrationSearch {

  /** The text fields the search text is matched against (BR-10). */
  static final List<String> SEARCHED_FIELDS =
      List.of("firstName", "lastName", "email", "organization", "studyInstitution");

  private final EntityManager entityManager;
  private final RegistrationJpaRepository registrations;

  JpaRegistrationRepository(EntityManager entityManager, RegistrationJpaRepository registrations) {
    this.entityManager = entityManager;
    this.registrations = registrations;
  }

  @Override
  public void save(Registration registration) {
    entityManager.persist(RegistrationEntity.from(registration));
    entityManager.flush();
  }

  @Override
  public List<Registration> findAll() {
    return registrations.findAllByOrderByRegisteredAtAscIdAsc().stream()
        .map(RegistrationEntity::toDomain)
        .toList();
  }

  @Override
  public RegistrationPage search(SearchCriteria criteria) {
    List<String> conditions = new ArrayList<>();
    Map<String, Object> parameters = new LinkedHashMap<>();
    if (!criteria.text().isEmpty()) {
      conditions.add(
          SEARCHED_FIELDS.stream()
              .map(field -> "lower(r." + field + ") like :pattern escape '\\'")
              .reduce((a, b) -> a + " or " + b)
              .map(any -> "(" + any + ")")
              .orElseThrow());
      parameters.put("pattern", "%" + likeEscape(criteria.text().toLowerCase(Locale.ROOT)) + "%");
    }
    if (criteria.type() != null) {
      conditions.add("r.type = :type");
      parameters.put("type", criteria.type());
    }
    if (criteria.optionId() != null) {
      conditions.add(":option member of r.options");
      parameters.put("option", criteria.optionId());
    }
    String where = conditions.isEmpty() ? "" : " where " + String.join(" and ", conditions);

    TypedQuery<Long> count =
        entityManager.createQuery("select count(r) from RegistrationEntity r" + where, Long.class);
    TypedQuery<RegistrationEntity> page =
        entityManager
            .createQuery(
                "select r from RegistrationEntity r"
                    + where
                    + " order by r.registeredAt desc, r.id desc",
                RegistrationEntity.class)
            .setFirstResult(criteria.offset())
            .setMaxResults(SearchCriteria.PAGE_SIZE);
    parameters.forEach(
        (name, value) -> {
          count.setParameter(name, value);
          page.setParameter(name, value);
        });
    return new RegistrationPage(
        count.getSingleResult(),
        criteria.page(),
        page.getResultList().stream().map(RegistrationEntity::toDomain).toList());
  }

  /** Escapes the LIKE wildcards, so that {@code %}, {@code _} and {@code \} match literally. */
  static String likeEscape(String text) {
    return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
