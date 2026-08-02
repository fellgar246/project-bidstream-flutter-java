package com.bidstream.api.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

  private static final JavaClasses DOMAIN_CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.bidstream.domain");

  @Test
  void domain_mustNotDependOnSpring() {
    noClasses()
        .that()
        .resideInAPackage("com.bidstream.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.springframework..")
        .check(DOMAIN_CLASSES);
  }

  @Test
  void domain_mustNotDependOnJpa() {
    noClasses()
        .that()
        .resideInAPackage("com.bidstream.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("jakarta.persistence..")
        .check(DOMAIN_CLASSES);
  }

  @Test
  void domain_mustNotDependOnJackson() {
    noClasses()
        .that()
        .resideInAPackage("com.bidstream.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("com.fasterxml..")
        .check(DOMAIN_CLASSES);
  }
}
