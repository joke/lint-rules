# Spec Delta

## ADDED Requirements

### Requirement: Unit specifications isolate one class and are exhaustive
A specification tagged `unit` SHALL test one production class with every collaborator replaced by a
double, SHALL NOT run PMD or CodeNarc over source, and SHALL cover every branch and decision of that
class. Unit specifications are the project's proof of completeness and the only ones Pitest selects.

A unit specification SHALL be named for the class it tests, with the suffix `Spec`.

#### Scenario: A unit specification never starts the tool
- **WHEN** a specification tagged `unit` is inspected
- **THEN** it neither parses source text nor starts a PMD or CodeNarc analysis
- **AND** it replaces every collaborator of the class under test with a double

#### Scenario: Every rule class has its own unit specification
- **WHEN** the test sources of either module are inspected
- **THEN** each rule class has a unit specification named for it

#### Scenario: A decision with no reachable input is still covered
- **WHEN** a rule contains a guard that parsed source cannot reach
- **THEN** a unit specification drives that guard directly

### Requirement: Integration specifications are a smoke check
A specification tagged `integration` SHALL run a shipped rule through the real tool against a
fixture file and assert that the rule reports. It SHALL NOT aim to cover every case: completeness is
the unit specifications' job. Integration specifications are outside Pitest and carry no mutation
threshold.

An integration specification SHALL be named for the rule it runs, with the suffix `IntegrationSpec`.
Every rule SHALL have one.

#### Scenario: Every rule has an integration specification
- **WHEN** the test sources of either module are inspected
- **THEN** each rule has an integration specification that runs it through the real tool against at
  least one violating fixture file

#### Scenario: Integration specifications are not mutation-tested
- **WHEN** `pitest` runs
- **THEN** it executes no specification tagged `integration`

#### Scenario: The tag states what the specification does
- **WHEN** a specification that runs the real tool is inspected
- **THEN** it is tagged `integration`, never `unit`

## MODIFIED Requirements

### Requirement: Both modules test with Spock
Tests SHALL be Spock specifications in Groovy under `src/test/groovy` in both published modules. Both
modules SHALL apply the `groovy` plugin, with rule classes under `src/main/java`, and SHALL declare
Spock and Groovy on test configurations only. Every specification SHALL carry a `@spock.lang.Tag`
of `unit` or `integration`, and the tag SHALL state what the specification does rather than which
build task selects it.

#### Scenario: Both modules test in Spock
- **WHEN** `pmd-rules/build.gradle` and `codenarc-rules/build.gradle` are inspected
- **THEN** each applies the `groovy` plugin
- **AND** Spock and Groovy are declared on test configurations only

#### Scenario: The PMD module has no JUnit-style rule tests
- **WHEN** `pmd-rules/src/test` is inspected
- **THEN** it contains no Java test class and no `pmd-test` XML descriptor
- **AND** `pmd-test` is not declared in `pmd-rules/build.gradle`

#### Scenario: Specifications are tagged
- **WHEN** a Spock specification is inspected
- **THEN** it carries `@spock.lang.Tag` naming either `unit` or `integration`

#### Scenario: Rules are exercised through PMD
- **WHEN** the PMD rule specifications that run a rule through PMD are inspected
- **THEN** each is tagged `integration`, runs the rule against fixture files and asserts its violations
- **AND** none is tagged `unit`

### Requirement: Spock mocks isolate the unit under test
A method under test SHALL be isolated from the sibling methods it calls by spying the subject and
stubbing those siblings, the style `static-method-state-rule`, `method-visibility-rule` and
`visible-for-testing-rule` exist to keep possible. In `pmd-rules`, a test needing a chain of nested
collaborators SHALL declare one `DeepMock()` instead of a mock for each link.

`codenarc-rules` SHALL NOT use `DeepMock`: its transformation rewrites the target of every
interaction in any Groovy source compiled with it on the classpath, which would corrupt the source
the rules are meant to analyse. There a Groovy AST node that is plain data SHALL be built as a real
node, and one whose state a test cannot build SHALL be a `Mock` or `Stub`.

A static method of a framework the rule depends on, such as PMD's or CodeNarc's helpers, SHALL be
isolated with `SpyStatic` declared with the unlabelled setup statements. A static method of the JDK
SHALL NOT be stubbed: it is a function of its arguments, not a collaborator.

#### Scenario: A sibling is stubbed on a spy
- **WHEN** a specification tests a rule's `visit` method that delegates to a helper
- **THEN** it spies the rule and stubs the helper

#### Scenario: Nested collaborators use a deep mock
- **WHEN** a `pmd-rules` test needs a chain of two or more nested collaborators
- **THEN** it declares one `DeepMock()` and stubs the end of the chain
- **AND** it does not declare an intermediate mock for each link

#### Scenario: CodeNarc specifications do not use DeepMock
- **WHEN** the specifications under `codenarc-rules/src/test` are inspected
- **THEN** none calls `DeepMock` or `GroovyDeepMock`

#### Scenario: Plain data nodes are built, not mocked
- **WHEN** a `codenarc-rules` unit specification needs a Groovy AST node that is plain data
- **THEN** it constructs the real node

#### Scenario: A framework static is isolated
- **WHEN** a unit specification tests a method that calls a static helper of PMD or CodeNarc
- **THEN** it declares `SpyStatic` for that helper's class before any label
- **AND** it stubs the call in a `then:` block

#### Scenario: A JDK static is left alone
- **WHEN** a unit specification tests a method that calls a static method of the JDK
- **THEN** the specification does not stub that call

### Requirement: Spock mocking dependencies are managed by the platform
The test classpath of both modules SHALL provide `byte-buddy` and `mockito-core`, so that Spock can
mock classes, final classes and statics, and spy concrete ones. The test classpath of `pmd-rules`
SHALL also provide `io.github.joke:spock-deepmock` for nested mocks; the test classpath of
`codenarc-rules` SHALL NOT. Their versions SHALL be constrained in the `dependencies` platform, and
none SHALL be `api`, `implementation` or `runtimeOnly`.

#### Scenario: Mocking dependencies are managed by the platform
- **WHEN** the `dependencies` platform is inspected
- **THEN** it constrains `net.bytebuddy:byte-buddy`, `org.mockito:mockito-core` and
  `io.github.joke:spock-deepmock`
- **AND** the test classpath of each module resolves the ones it uses without a version of its own

#### Scenario: Final classes can be mocked
- **WHEN** a specification mocks a final PMD AST class with `Mock()`
- **THEN** the mock is created

#### Scenario: DeepMock stays out of the CodeNarc module
- **WHEN** the test classpath of `codenarc-rules` is resolved
- **THEN** it contains no `spock-deepmock` jar

#### Scenario: Test dependencies do not reach the POM
- **WHEN** the generated POM of either module is inspected
- **THEN** it declares no dependency

### Requirement: Mutation testing thresholds
Pitest SHALL be configured with `mutationThreshold`, `coverageThreshold` and
`testStrengthThreshold` all at 100, and `check` SHALL depend on `pitest`. The thresholds SHALL apply
unchanged to both published modules.

Pitest SHALL select only specifications tagged `unit`, through `includedGroups`. Each rule class
SHALL therefore meet every threshold on its unit specification alone, with no help from an
integration specification.

Mutation analysis in `codenarc-rules` runs against Java bytecode, because that module's rules are
Java. That its tests are Spock changes only which engine executes them: Pitest's JUnit 5 plugin runs
the JUnit Platform, and Spock specifications carrying `@spock.lang.Tag('unit')` are selected by
`includedGroups`. No threshold SHALL be lowered for that module on the grounds that its tests are
Groovy.

The `-Dspock.parallel.disabled=true` JVM argument SHALL NOT be carried over.

#### Scenario: Check runs mutation testing
- **WHEN** `./gradlew check` runs
- **THEN** the `pitest` task executes and fails the build below any 100% threshold

#### Scenario: Thresholds are uniform across modules
- **WHEN** the Pitest configuration is inspected
- **THEN** no module lowers any of the three thresholds

#### Scenario: Spock specifications are selected by mutation analysis
- **WHEN** `pitest` runs for `:codenarc-rules`
- **THEN** it executes the Spock specifications rather than reporting no tests

#### Scenario: Only unit specifications are selected
- **WHEN** the Pitest configuration is inspected
- **THEN** `includedGroups` names `unit` and does not name `integration`

#### Scenario: A rule class is complete without integration specifications
- **WHEN** every specification tagged `integration` is excluded and `pitest` runs
- **THEN** each rule class still meets all three thresholds

#### Scenario: No Spock parallel argument is set
- **WHEN** the Pitest configuration is inspected
- **THEN** its `jvmArgs` contain no `spock.parallel.disabled` property
