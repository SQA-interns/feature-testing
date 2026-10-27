package si.konferenca.registration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** The package rules of specification S2 (AR-02, AR-03). */
@AnalyzeClasses(
    packages = "si.konferenca.registration",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  private static final String ROOT = "si.konferenca.registration";

  @ArchTest
  static final ArchRule domainDependsOnTheJdkOnly =
      noClasses()
          .that()
          .resideInAPackage(ROOT + ".domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              ROOT,
              ROOT + ".application..",
              ROOT + ".config..",
              ROOT + ".adapter..",
              ROOT + ".web..",
              "org.springframework..",
              "jakarta.persistence..",
              "org.apache.poi..");

  @ArchTest
  static final ArchRule applicationDependsOnlyOnTheDomain =
      noClasses()
          .that()
          .resideInAPackage(ROOT + ".application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(ROOT, ROOT + ".config..", ROOT + ".adapter..", ROOT + ".web..");

  @ArchTest
  static final ArchRule adaptersDoNotDependOnTheWeb =
      noClasses()
          .that()
          .resideInAPackage(ROOT + ".adapter..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage(ROOT + ".web..");

  @ArchTest
  static final ArchRule adaptersDoNotDependOnEachOther =
      slices().matching(ROOT + ".adapter.(*)..").should().notDependOnEachOther();

  @ArchTest
  static final ArchRule webDoesNotDependOnAdapters =
      noClasses()
          .that()
          .resideInAPackage(ROOT + ".web..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage(ROOT + ".adapter..");

  @ArchTest
  static final ArchRule noCyclesBetweenTopLevelSlices =
      slices().matching(ROOT + ".(*)..").should().beFreeOfCycles();
}
