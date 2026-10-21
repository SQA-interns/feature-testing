package si.konferenca.registration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * The declared backend architecture of {@code docs/02_specification.md} section 2 (AR-02) and no
 * package cycles (AR-03).
 */
class ArchitectureTest {

  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("si.konferenca.registration");

  @Test
  void ar02_domainDependsOnlyOnTheJavaPlatform() {
    classes()
        .that()
        .resideInAPackage("..registration.domain..")
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage("..registration.domain..", "java..")
        .check(CLASSES);
  }

  @Test
  void ar02_applicationDependsOnlyOnDomainAndTransactions() {
    classes()
        .that()
        .resideInAPackage("..registration.application..")
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(
            "..registration.application..",
            "..registration.domain..",
            "java..",
            "org.slf4j..",
            "org.springframework.stereotype..",
            "org.springframework.transaction..")
        .check(CLASSES);
  }

  @Test
  void ar02_adaptersDoNotUseTheWebLayer() {
    noClasses()
        .that()
        .resideInAPackage("..registration.adapter..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..registration.web..", "..registration.config..")
        .check(CLASSES);
  }

  @Test
  void ar02_adaptersDoNotUseEachOther() {
    slices()
        .matching("..registration.adapter.(*)..")
        .should()
        .notDependOnEachOther()
        .check(CLASSES);
  }

  @Test
  void ar02_webUsesNoAdapterAndNoPersistence() {
    noClasses()
        .that()
        .resideInAPackage("..registration.web..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..registration.adapter..", "..registration.config..", "jakarta.persistence..")
        .check(CLASSES);
  }

  @Test
  void ar02_onlyTheCompositionRootWiresAdapters() {
    noClasses()
        .that()
        .resideInAnyPackage("..registration.domain..", "..registration.application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..registration.adapter..", "..registration.config..")
        .check(CLASSES);
  }

  @Test
  void ar03_noPackageCycles() {
    slices().matching("si.konferenca.registration.(*)..").should().beFreeOfCycles().check(CLASSES);
  }
}
