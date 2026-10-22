package si.konferenca.registration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The declared architecture of docs/02_specification.md section 2 (AR-02, AR-03, DoD-04). */
class ArchitectureTest {

  private static final String ROOT = "si.konferenca.registration";
  private static JavaClasses classes;

  @BeforeAll
  static void importProductionClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);
  }

  @DisplayName("AR-02 rule 1: the domain depends only on the JDK")
  @Test
  void domainDependsOnlyOnTheJdk() {
    classes()
        .that()
        .resideInAPackage(ROOT + ".domain..")
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(ROOT + ".domain..", "java..")
        .check(classes);
  }

  @DisplayName("AR-02 rule 2: the application does not use adapters, web or security")
  @Test
  void applicationDoesNotDependOnOuterLayers() {
    noClasses()
        .that()
        .resideInAPackage(ROOT + ".application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(ROOT + ".infrastructure..", ROOT + ".web..", ROOT + ".security..")
        .check(classes);
  }

  @DisplayName("AR-02 rule 3: the web layer does not use adapters")
  @Test
  void webDoesNotDependOnInfrastructure() {
    noClasses()
        .that()
        .resideInAPackage(ROOT + ".web..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(ROOT + ".infrastructure..")
        .check(classes);
  }

  @DisplayName("AR-02 rule 4: adapters do not use web or security")
  @Test
  void infrastructureDoesNotDependOnWebOrSecurity() {
    noClasses()
        .that()
        .resideInAPackage(ROOT + ".infrastructure..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(ROOT + ".web..", ROOT + ".security..")
        .check(classes);
  }

  @DisplayName("AR-03 rule 5: no cycles between layers or between adapters")
  @Test
  void noPackageCycles() {
    slices().matching(ROOT + ".(*)..").should().beFreeOfCycles().check(classes);
    slices().matching(ROOT + ".infrastructure.(*)..").should().beFreeOfCycles().check(classes);
  }

  @DisplayName("AR-02 rule 6: JPA only in the persistence adapter")
  @Test
  void jpaOnlyInPersistence() {
    noClasses()
        .that()
        .resideOutsideOfPackage(ROOT + ".infrastructure.persistence..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("jakarta.persistence..")
        .check(classes);
  }
}
