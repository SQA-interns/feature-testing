package si.konferenca.registration.adapter.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import si.konferenca.registration.application.RegistrationQueries;
import si.konferenca.registration.application.RegistrationStore;
import si.konferenca.registration.domain.Registration;
import si.konferenca.registration.domain.SearchCriteria;
import si.konferenca.registration.domain.SearchPage;

/** {@link RegistrationStore} and {@link RegistrationQueries} on PostgreSQL through JPA. */
public class JpaRegistrationStore implements RegistrationStore, RegistrationQueries {

  private static final String[] SEARCHED = {
    "firstName", "lastName", "email", "organization", "studyInstitution"
  };

  private final EntityManager entityManager;
  private final RegistrationJpaRepository repository;

  public JpaRegistrationStore(EntityManager entityManager, RegistrationJpaRepository repository) {
    this.entityManager = entityManager;
    this.repository = repository;
  }

  @Override
  public void add(Registration registration) {
    entityManager.persist(RegistrationEntity.from(registration));
    entityManager.flush();
  }

  @Override
  @Transactional(readOnly = true)
  public SearchPage search(SearchCriteria criteria) {
    Sort newestFirst = Sort.by(Sort.Direction.DESC, "registeredAt", "id");
    Page<RegistrationEntity> page =
        repository.findAll(
            matching(criteria),
            PageRequest.of(criteria.page() - 1, SearchCriteria.PAGE_SIZE, newestFirst));
    return new SearchPage(
        page.getTotalElements(),
        criteria.page(),
        page.getContent().stream().map(RegistrationEntity::toDomain).toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<Registration> all() {
    return repository.findAll(Sort.by(Sort.Direction.ASC, "registeredAt", "id")).stream()
        .map(RegistrationEntity::toDomain)
        .toList();
  }

  private static Specification<RegistrationEntity> matching(SearchCriteria criteria) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (criteria.type() != null) {
        predicates.add(cb.equal(root.get("type"), criteria.type().name()));
      }
      if (criteria.optionId() != null) {
        Expression<Collection<String>> options = root.get("optionIds");
        predicates.add(cb.isMember(criteria.optionId(), options));
      }
      if (!criteria.text().isEmpty()) {
        predicates.add(textMatches(root, cb, criteria.text()));
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  private static Predicate textMatches(
      Root<RegistrationEntity> root, CriteriaBuilder cb, String text) {
    String pattern = "%" + escapeLike(text.toLowerCase(Locale.ROOT)) + "%";
    List<Predicate> any = new ArrayList<>();
    for (String field : SEARCHED) {
      any.add(cb.like(cb.lower(root.get(field)), pattern, '\\'));
    }
    return cb.or(any.toArray(Predicate[]::new));
  }

  static String escapeLike(String text) {
    return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
