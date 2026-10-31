# Spec Delta

## MODIFIED Requirements

### Requirement: The rule declares a minimum language version
The rule SHALL declare `minimumLanguageVersion="10"` in `category/java/joke.xml`, because `var` does
not exist before Java 10. PMD SHALL therefore skip the rule for source analysed at an earlier
language version rather than emitting violations a consumer cannot act on. Because PMD 7 under Gradle
analyses at its own default language version, this declaration alone does not stop the rule on a
Java 8 codebase; the target-version property below does.

#### Scenario: The rule is skipped below Java 10
- **WHEN** PMD analyses source at Java 8
- **THEN** the rule contributes no violations

#### Scenario: The rule applies from Java 10
- **WHEN** PMD analyses source at Java 10 or later
- **THEN** the rule reports explicitly typed local variables

## ADDED Requirements

### Requirement: The rule accepts the consumer's target Java version
The rule SHALL expose a property named `targetJavaVersion` that a consumer sets in the ruleset XML it
passes to the PMD tool. The property holds a Java language level such as `8`, `10` or `17`. When the
property is set to a level below 10, the rule SHALL report no violations, regardless of the language
version PMD analysed the file with. When the property is unset, the rule SHALL behave as the minimum
language version requirement alone describes.

#### Scenario: A target below Java 10 suppresses the rule
- **WHEN** the rule is configured with `targetJavaVersion` set to `8` and a method body contains `String name = "joke";`
- **THEN** the rule reports no violation

#### Scenario: A target of Java 10 or later keeps the rule active
- **WHEN** the rule is configured with `targetJavaVersion` set to `10` and a method body contains `String name = "joke";`
- **THEN** the rule reports a violation on that declaration

#### Scenario: An unset target leaves the rule unchanged
- **WHEN** the rule is configured without `targetJavaVersion` and a method body contains `String name = "joke";`
- **THEN** the rule reports a violation on that declaration

#### Scenario: A value that is not a Java language level is rejected
- **WHEN** the rule is configured with `targetJavaVersion` set to a value that is not a Java language level, such as `eight`
- **THEN** PMD reports a configuration error for the rule rather than silently applying it

### Requirement: The target-version property is documented
`category/java/joke.xml` SHALL describe the `targetJavaVersion` property in the rule's description, and
the pmd-rules README SHALL show a ruleset XML that sets it for a Java 8 codebase.

#### Scenario: The category entry describes the property
- **WHEN** the `UseVarForLocalVariables` entry in `category/java/joke.xml` is inspected
- **THEN** its description names `targetJavaVersion` and states that it suppresses the rule below Java 10

#### Scenario: The README shows the Java 8 configuration
- **WHEN** the pmd-rules README is inspected
- **THEN** it contains a ruleset example that sets `targetJavaVersion` to `8` for `UseVarForLocalVariables`
