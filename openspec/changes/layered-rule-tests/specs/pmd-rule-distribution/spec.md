# Spec Delta

## MODIFIED Requirements

### Requirement: Rule test fixtures are Java files outside the Java source sets
The sample sources that integration specifications run a rule over SHALL be real `.java` files under
`pmd-rules/src/test/resources`, one sample per file, and SHALL NOT sit in a Java or Groovy source set.
Resources are on no analysis path, so the build neither compiles nor flags them and no path needs to
be excluded.

Each rule SHALL have at least one violating fixture. The set is a smoke check, not an inventory of
cases: a fixture SHALL exist only where it shows something that only the real PMD can show, such as
the rule firing at all, or PMD's own model of a construct the rule depends on. A case that exercises
only the rule's own logic belongs in a unit specification.

#### Scenario: Fixtures are invisible to the build's analysis
- **WHEN** `compileTestJava`, `compileTestGroovy`, `pmdMain` and `pmdTest` run over this repository
- **THEN** no violation or diagnostic is reported against rule test data

#### Scenario: Test data lives in fixture files
- **WHEN** a rule's integration test data is inspected
- **THEN** every sample is a `.java` file under `src/test/resources`
- **AND** no `.java` file in a source set exists whose purpose is to carry a deliberate violation

#### Scenario: A fixture is read as a real file
- **WHEN** a fixture file is opened
- **THEN** it contains plain Java source with no XML or CDATA wrapper

#### Scenario: Every rule has a violating fixture
- **WHEN** the fixtures of each rule are inspected
- **THEN** at least one makes the rule report

#### Scenario: A fixture does not test PMD itself
- **WHEN** the fixtures are inspected
- **THEN** none exists only to show that suppression, report ordering or a per-file count behaves as
  PMD documents

### Requirement: Rule fixtures run at a chosen Java language version
An integration specification SHALL be able to run a fixture at a stated Java language version, so that
a case which depends on the language level, such as `var` at Java 8 or 10, is expressed in the
specification.

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
tests SHALL be Spock specifications in two layers: unit specifications that test each rule class in
isolation, and integration specifications that run each rule through PMD against the fixture files.

Because the module applies the `groovy` plugin, the convention plugin's CodeNarc task SHALL run for
`:pmd-rules`, analysing its specifications with the ruleset published by `codenarc-rules`. This
SHALL NOT create a task cycle with `codenarc-rules`' own use of the `pmd-rules` jar.

#### Scenario: The PMD module is a Spock module
- **WHEN** `pmd-rules/build.gradle` is inspected
- **THEN** it applies the `groovy` plugin
- **AND** it declares Spock on test configurations only

#### Scenario: Each rule has both layers
- **WHEN** the specifications of a PMD rule are inspected
- **THEN** a `unit` specification tests the rule class with its AST collaborators replaced
- **AND** an `integration` specification runs the rule through PMD against a fixture

#### Scenario: CodeNarc analyses the PMD module's specifications
- **WHEN** `./gradlew check` runs
- **THEN** a `CodeNarc` task executes for `:pmd-rules`
- **AND** it uses the ruleset published by `codenarc-rules`

#### Scenario: The two modules build together without a cycle
- **WHEN** `./gradlew check` runs from the repository root
- **THEN** the build completes without a circular task dependency between `:pmd-rules` and
  `:codenarc-rules`
