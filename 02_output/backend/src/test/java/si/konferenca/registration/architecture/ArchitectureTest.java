package si.konferenca.registration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The architecture declared in {@code docs/02_specification.md} section 3 (AR-02) and no package
 * cycles (AR-03).
 */
@AnalyzeClasses(
    packages = "si.konferenca.registration",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  private static final String ROOT = "si.konferenca.registration";

  @ArchTest
  static final ArchRule domainDependsOnNothingElseInTheProject =
      noClasses()
          .that()
          .resideInAPackage(ROOT + ".domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(ROOT + ".application..", ROOT + ".adapter..", ROOT + ".config..");

  @ArchTest
  static final ArchRule domainIsFrameworkFree =
      noClasses()
          .that()
          .resideInAPackage(ROOT + ".domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "tools.jackson..");

  @ArchTest
  static final ArchRule applicationDependsOnlyOnDomain =
      noClasses()
          .that()
          .resideInAPackage(ROOT + ".application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(ROOT + ".adapter..", ROOT + ".config..");

  @ArchTest
  static final ArchRule adaptersDoNotDependOnConfig =
      noClasses()
          .that()
          .resideInAPackage(ROOT + ".adapter..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage(ROOT + ".config..");

  @ArchTest
  static final ArchRule adaptersDoNotDependOnEachOther =
      slices().matching(ROOT + ".adapter.(*)..").should().notDependOnEachOther();

  @ArchTest
  static final ArchRule noPackageCycles =
      slices().matching(ROOT + ".(*)..").should().beFreeOfCycles();
}
