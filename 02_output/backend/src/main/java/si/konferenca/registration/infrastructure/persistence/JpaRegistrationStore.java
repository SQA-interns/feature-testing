package si.konferenca.registration.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;
import si.konferenca.registration.application.RegistrationStore;
import si.konferenca.registration.application.SearchPage;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.SearchCriteria;

/** Stores registrations in PostgreSQL with parameterised JPA statements only (SB-05). */
@Repository
public class JpaRegistrationStore implements RegistrationStore {

  @PersistenceContext private EntityManager entityManager;

  @Override
  public void save(Registration registration) {
    entityManager.persist(RegistrationEntity.from(registration));
    entityManager.flush();
  }

  @Override
  public List<Registration> findAllOldestFirst() {
    return entityManager
        .createQuery("select r from RegistrationEntity r order by r.seq", RegistrationEntity.class)
        .getResultList()
        .stream()
        .map(RegistrationEntity::toRegistration)
        .toList();
  }

  @Override
  public SearchPage search(SearchCriteria criteria) {
    StringBuilder where = new StringBuilder(" where 1 = 1");
    Map<String, Object> parameters = new HashMap<>();
    if (!criteria.text().isEmpty()) {
      where.append(" and r.searchText like :pattern escape '\\'");
      parameters.put("pattern", "%" + escapeLike(criteria.text()) + "%");
    }
    criteria
        .type()
        .ifPresent(
            type -> {
              where.append(" and r.type = :type");
              parameters.put("type", type.name());
            });
    criteria
        .option()
        .ifPresent(
            option -> {
              where.append(" and :option member of r.options");
              parameters.put("option", option);
            });
    TypedQuery<Long> count =
        entityManager.createQuery("select count(r) from RegistrationEntity r" + where, Long.class);
    TypedQuery<RegistrationEntity> page =
        entityManager.createQuery(
            "select r from RegistrationEntity r"
                + where
                + " order by r.registeredAt desc, r.seq desc",
            RegistrationEntity.class);
    parameters.forEach(
        (name, value) -> {
          count.setParameter(name, value);
          page.setParameter(name, value);
        });
    List<Registration> items =
        page
            .setFirstResult(criteria.offset())
            .setMaxResults(SearchCriteria.PAGE_SIZE)
            .getResultList()
            .stream()
            .map(RegistrationEntity::toRegistration)
            .toList();
    return new SearchPage(count.getSingleResult(), items);
  }

  /** Escapes the LIKE wildcards, so the text matches literally (SB-05). */
  static String escapeLike(String text) {
    return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
