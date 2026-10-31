# var-local-variables-rule Specification

## Purpose

The `UseVarForLocalVariables` rule: which local variable declarations it reports, the boundaries
where `var` is illegal and it must stay silent, its minimum language version, its documentation in
the category file, and its test and mutation-coverage obligations.

## Requirements

### Requirement: UseVarForLocalVariables reports explicitly typed local variables
The artifact SHALL provide a rule named `UseVarForLocalVariables` that reports a local variable
declaration written with an explicit type where `var` would compile and preserve the declared type.

The rule SHALL be implemented in Java against the PMD 7 Java AST. It SHALL NOT be a port of the
PMD 6 XPath expression `//LocalVariableDeclaration[Type/@Image != 'var']`, which relies on an AST
attribute PMD 7 no longer exposes and therefore silently matches nothing.

#### Scenario: An explicitly typed local variable is reported
- **WHEN** a method body contains `String name = "joke";`
- **THEN** the rule reports a violation on that declaration

#### Scenario: A var-declared local variable is not reported
- **WHEN** a method body contains `var name = "joke";`
- **THEN** the rule reports no violation

#### Scenario: An enhanced for-loop variable is reported
- **WHEN** a method body contains `for (String s : names) { }`
- **THEN** the rule reports a violation on the loop variable

#### Scenario: A basic for-loop initializer is reported
- **WHEN** a method body contains `for (int i = 0; i < n; i++) { }`
- **THEN** the rule reports a violation on the loop variable

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

### Requirement: The rule does not report declarations where var is illegal
The rule SHALL NOT report a declaration that cannot legally be rewritten with `var`. A violation a
consumer cannot fix is worse than a missed one, so the rule SHALL be conservative at these
boundaries.

The rule SHALL NOT report:

- a declaration with no initializer, such as `int i;`
- a declaration initialized to the `null` literal, such as `String s = null;`
- a declaration using array-initializer shorthand, such as `int[] a = {1, 2};`
- a declaration initialized with a lambda expression, such as `Runnable r = () -> {};`
- a declaration initialized with a method reference, such as `Supplier<String> s = String::new;`
- a declaration declaring more than one variable, such as `int a = 1, b = 2;`

#### Scenario: An uninitialized declaration is not reported
- **WHEN** a method body contains `int i;`
- **THEN** the rule reports no violation

#### Scenario: A null-initialized declaration is not reported
- **WHEN** a method body contains `String s = null;`
- **THEN** the rule reports no violation

#### Scenario: An array-initializer shorthand is not reported
- **WHEN** a method body contains `int[] a = {1, 2};`
- **THEN** the rule reports no violation

#### Scenario: A lambda initializer is not reported
- **WHEN** a method body contains `Runnable r = () -> {};`
- **THEN** the rule reports no violation

#### Scenario: A method-reference initializer is not reported
- **WHEN** a method body contains `Supplier<String> s = String::new;`
- **THEN** the rule reports no violation

#### Scenario: A multi-variable declaration is not reported
- **WHEN** a method body contains `int a = 1, b = 2;`
- **THEN** the rule reports no violation

### Requirement: The rule applies only to local variables
The rule SHALL NOT report fields, method parameters, constructor parameters, catch parameters, or
lambda parameters. None of these is a local variable declaration, and `var` is either illegal or
carries different meaning at each.

#### Scenario: A field is not reported
- **WHEN** a class declares `private String name = "joke";`
- **THEN** the rule reports no violation

#### Scenario: A method parameter is not reported
- **WHEN** a method declares `void f(String name)`
- **THEN** the rule reports no violation

#### Scenario: A catch parameter is not reported
- **WHEN** a method body contains `catch (IOException e) { }`
- **THEN** the rule reports no violation

#### Scenario: A lambda parameter is not reported
- **WHEN** a method body contains `list.forEach((String s) -> { });`
- **THEN** the rule reports no violation

### Requirement: The rule is documented in the category file
`category/java/joke.xml` SHALL declare the rule with its implementing class, a message directing the
reader to use `var`, a description, a priority, and both a violating and a compliant example.

#### Scenario: The category entry is complete
- **WHEN** the `UseVarForLocalVariables` entry in `category/java/joke.xml` is inspected
- **THEN** it declares a `class`, a `message`, a `<description>`, a `<priority>` and an
  `<example>`

### Requirement: The rule is covered by isolated unit tests and a smoke fixture
The rule SHALL be tested by a unit specification that isolates the rule class and covers every
reported and every non-reported case above, meeting the project's 100% mutation, coverage and test
strength thresholds on that specification alone. An integration specification SHALL also run the rule
through PMD against at least one violating fixture file.

#### Scenario: Every specified case has a unit case
- **WHEN** the rule's unit specification is inspected
- **THEN** it contains a case for each reported and each non-reported scenario in this spec

#### Scenario: Mutation thresholds are met by the unit specification
- **WHEN** `./gradlew pitest` runs
- **THEN** the rule class meets 100% mutation coverage, line coverage and test strength
- **AND** no specification tagged `integration` contributes to that result

#### Scenario: The rule fires through PMD
- **WHEN** the rule's integration specification runs
- **THEN** PMD reports a violation for the violating fixture

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
- **THEN** it contains a ruleset example that sets `targetJavaVersion` to `8`
