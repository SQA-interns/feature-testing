package si.konferenca.registration;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/** The architecture declared in docs/02_specification.md section 2 (AR-02, AR-03, DoD-04). */
class ArchitectureTest {

  static final String ROOT = "si.konferenca.registration";
  static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages(ROOT);

  @Test
  void a1DomainDependsOnNothingElse() {
    noClasses()
        .that()
        .resideInAPackage(ROOT + ".domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            ROOT + ".application..",
            ROOT + ".adapter..",
            ROOT + ".config..",
            "org.springframework..",
            "jakarta..",
            "com.fasterxml..",
            "tools.jackson..",
            "org.apache.poi..")
        .check(CLASSES);
  }

  @Test
  void a2ApplicationDependsOnlyOnDomain() {
    classes()
        .that()
        .resideInAPackage(ROOT + ".application..")
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(
            ROOT + ".application..",
            ROOT + ".domain..",
            "java..",
            "org.springframework.transaction..",
            "org.slf4j..")
        .check(CLASSES);
  }

  @Test
  void a3AdaptersAreIndependentOfEachOtherAndOfConfig() {
    for (String adapter :
        new String[] {"web", "persistence", "jsoncopy", "mail", "captcha", "export"}) {
      noClasses()
          .that()
          .resideInAPackage(ROOT + ".adapter." + adapter + "..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(ROOT + ".config..")
          .orShould()
          .dependOnClassesThat(
              com.tngtech.archunit.base.DescribedPredicate.describe(
                  "another adapter",
                  c ->
                      c.getPackageName().startsWith(ROOT + ".adapter.")
                          && !c.getPackageName().startsWith(ROOT + ".adapter." + adapter)))
          .check(CLASSES);
    }
  }

  @Test
  void a4NoPackageCycles() {
    slices().matching(ROOT + ".(*)..").should().beFreeOfCycles().check(CLASSES);
    slices().matching(ROOT + ".adapter.(*)..").should().beFreeOfCycles().check(CLASSES);
  }

  @Test
  void a5TechnologiesStayInTheirAdapter() {
    noClasses()
        .that()
        .resideOutsideOfPackage(ROOT + ".adapter.persistence..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("jakarta.persistence..")
        .check(CLASSES);
    noClasses()
        .that()
        .resideOutsideOfPackage(ROOT + ".adapter.export..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.apache.poi..")
        .check(CLASSES);
    noClasses()
        .that()
        .resideOutsideOfPackages(ROOT + ".adapter.mail..", ROOT + ".config..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.springframework.mail..", "jakarta.mail..")
        .check(CLASSES);
  }
}
