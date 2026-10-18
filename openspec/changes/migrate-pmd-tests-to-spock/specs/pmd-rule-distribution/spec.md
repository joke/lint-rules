# Spec Delta

## MODIFIED Requirements

### Requirement: The published POM declares no dependencies
The published artifact SHALL declare no `compile`- or `runtime`-scope dependency. The consuming
project supplies PMD at a version it chooses, via Gradle's `pmd` configuration, and this project
SHALL NOT impose one transitively.

This SHALL be secured by configuration choice rather than by a verification task. Every dependency
the `pmd-rules` module declares SHALL sit on a configuration that structurally cannot reach the POM —
`compileOnly`, `annotationProcessor`, `pmd`, `codenarc`, `testImplementation`, `testCompileOnly` or
`testRuntimeOnly`. No `implementation`, `api` or `runtimeOnly` declaration SHALL be added to the
`pmd-rules` module.

The previous `verifyPomHasNoDependencies` task, which parsed the generated POM and failed `check` on
any declared dependency, SHALL NOT be reinstated. The property it defended is stated here; a
`check`-time XML parse of a build output is a heavy way to state "do not use `implementation` here".

#### Scenario: The published POM is empty of dependencies
- **WHEN** the generated POM is inspected
- **THEN** it declares no dependency
- **AND** neither `pmd-core` nor `pmd-java` appears in it

#### Scenario: Every declaration is on a non-publishing configuration
- **WHEN** the `dependencies` block of `pmd-rules/build.gradle` is inspected
- **THEN** every declaration is `compileOnly`, `annotationProcessor`, `pmd`, `codenarc`,
  `testImplementation`, `testCompileOnly` or `testRuntimeOnly`
- **AND** none is `implementation`, `api` or `runtimeOnly`

#### Scenario: Test and processor dependencies do not reach the POM
- **WHEN** Spock is declared `testImplementation` and Lombok `annotationProcessor`
- **THEN** the published POM still declares no dependency

#### Scenario: No verification task guards this
- **WHEN** the `pmd-rules` module's tasks are inspected
- **THEN** no task parses the generated POM
- **AND** `check` depends on no such task

### Requirement: The ruleset is loaded by classpath reference
The integration tests SHALL load `rulesets/java/joke.xml` and `rulesets/java/joke-strict.xml` by
classpath reference, exactly as a consuming Gradle build does, rather than by filesystem path.
Classpath resolution from the `pmd` configuration is the single mechanism the distribution model
depends on, so it SHALL be exercised on every build rather than discovered by the first consumer.

The two resources SHALL be exercised by separate integration specifications, so that a failure names
which resource broke. The split SHALL NOT be made by a skip or assumption on the running PMD version,
because an assumption that stops holding produces a skipped test that a build log does not
distinguish from a test that never ran.

These tests SHALL NOT use `pmd-test`, because `RuleTst` is itself versioned and would confound a
PMD-version failure with a harness-version failure. They SHALL drive `PMDConfiguration`,
`RuleSetLoader`, `PmdAnalysis` and `RuleViolation` directly.

#### Scenario: Loading mirrors consumer wiring
- **WHEN** an integration specification loads a ruleset
- **THEN** it uses a classpath reference such as `rulesets/java/joke.xml` and not an absolute or
  project-relative file path

#### Scenario: A missing or misnamed resource fails the build
- **WHEN** a ruleset resource is absent from the jar or named differently
- **THEN** the integration tests fail

#### Scenario: Each resource has its own test class
- **WHEN** the integration tests are inspected
- **THEN** `rulesets/java/joke.xml` and `rulesets/java/joke-strict.xml` are exercised by separate
  specifications
- **AND** neither skips itself based on the running PMD version

#### Scenario: The tests use the stable API subset only
- **WHEN** the integration tests are inspected
- **THEN** they do not reference `pmd-test`

## REMOVED Requirements

### Requirement: Rule test fixtures stay in XML
**Reason**: The XML descriptors are hard to read, write and debug, and the constraint they served —
that the build must not flag its own deliberate violations — is met equally by `.java` files kept
outside every Java source set.
**Migration**: Replaced by "Rule test fixtures are Java files outside the Java source sets". Each XML
`test-code` case becomes one fixture file plus one row in a specification's `where:` table.

### Requirement: The PMD rules are tested with JUnit, not Spock
**Reason**: Spock is now the test framework of both modules, so the repository tests uniformly, and
the `pmd-test` harness the old requirement protected is replaced by direct use of PMD's stable API.
**Migration**: Replaced by "The PMD rules are tested with Spock". The `groovy` plugin is applied to
`pmd-rules` and Spock is declared on its test configurations.

## ADDED Requirements

### Requirement: Rule test fixtures are Java files outside the Java source sets
The deliberate rule violations and compliant samples that make up a rule's test data SHALL be real
`.java` files under `pmd-rules/src/test/resources`, one sample per file, and SHALL NOT sit in a Java
or Groovy source set. Resources are on no analysis path, so the build neither compiles nor flags
them and no path needs to be excluded.

#### Scenario: Fixtures are invisible to the build's analysis
- **WHEN** `compileTestJava`, `compileTestGroovy`, `pmdMain` and `pmdTest` run over this repository
- **THEN** no violation or diagnostic is reported against rule test data

#### Scenario: Test data lives in fixture files
- **WHEN** a rule's test data is inspected
- **THEN** every violating and compliant sample is a `.java` file under `src/test/resources`
- **AND** no `.java` file in a source set exists whose purpose is to carry a deliberate violation

#### Scenario: A fixture is read as a real file
- **WHEN** a fixture file is opened
- **THEN** it contains plain Java source with no XML or CDATA wrapper

### Requirement: Rule fixtures run at a chosen Java language version
A rule specification SHALL be able to run a fixture at a stated Java language version, so that a case
which depends on the language level, such as `var` at Java 8 or 10, is expressed in the specification.

The helper that runs a rule through PMD SHALL use only the API subset stable across PMD 7, so that
`integrationTest` and the unit specifications resolve at the 7.0.0 floor.

#### Scenario: A language-level case pins its version
- **WHEN** a specification checks `UseVarForLocalVariables` against a fixture at Java 8
- **THEN** it names that version in its `where:` row and the rule reports no violation

#### Scenario: The helper works at the floor
- **WHEN** the rule specifications run with PMD at the 7.0.0 floor
- **THEN** the helper runs without referencing an API added after 7.0.0

### Requirement: The PMD rules are tested with Spock
The `pmd-rules` module SHALL apply the `groovy` plugin and declare Spock on test configurations. Its
tests SHALL be Spock specifications that run each rule through PMD against the fixture files.

Because the module applies the `groovy` plugin, the convention plugin's CodeNarc task SHALL run for
`:pmd-rules`, analysing its specifications with the ruleset published by `codenarc-rules`. This
SHALL NOT create a task cycle with `codenarc-rules`' own use of the `pmd-rules` jar.

#### Scenario: The PMD module is a Spock module
- **WHEN** `pmd-rules/build.gradle` is inspected
- **THEN** it applies the `groovy` plugin
- **AND** it declares Spock on test configurations only

#### Scenario: CodeNarc analyses the PMD module's specifications
- **WHEN** `./gradlew check` runs
- **THEN** a `CodeNarc` task executes for `:pmd-rules`
- **AND** it uses the ruleset published by `codenarc-rules`

#### Scenario: The two modules build together without a cycle
- **WHEN** `./gradlew check` runs from the repository root
- **THEN** the build completes without a circular task dependency between `:pmd-rules` and
  `:codenarc-rules`
