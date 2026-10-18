# Spec Delta

## REMOVED Requirements

### Requirement: Test stack
**Reason**: The requirement bans Spock and Groovy from `pmd-rules` and requires JUnit 5, `pmd-test`
and Mockito there. Both modules now test with Spock, so it is replaced by the requirements below.
**Migration**: Replaced by "Both modules test with Spock", "Spock mocks isolate the unit under
test", "Tests are split by tag and run at the compile floor" and "The annotation dependency stays
off the published POM".

## ADDED Requirements

### Requirement: Both modules test with Spock
Tests SHALL be Spock specifications in Groovy under `src/test/groovy` in both published modules. Both
modules SHALL apply the `groovy` plugin, with rule classes under `src/main/java`, and SHALL declare
Spock and Groovy on test configurations only. Every specification SHALL carry a `@spock.lang.Tag`
of `unit` or `integration`; rule specifications SHALL be `unit`, which is what Pitest selects.

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
- **AND** every rule specification is tagged `unit`

#### Scenario: Rules are exercised through PMD
- **WHEN** the PMD rule specifications are inspected
- **THEN** each rule is run through PMD against fixture files and its violations are asserted

### Requirement: Spock mocking dependencies are managed by the platform
The test classpath of both modules SHALL provide `byte-buddy` and `mockito-core`, so that Spock can
mock final classes and spy concrete ones, and `io.github.joke:spock-deepmock` for nested mocks. Their
versions SHALL be constrained in the `dependencies` platform, and none SHALL be `api`,
`implementation` or `runtimeOnly`.

#### Scenario: Mocking dependencies are managed by the platform
- **WHEN** the `dependencies` platform is inspected
- **THEN** it constrains `net.bytebuddy:byte-buddy`, `org.mockito:mockito-core` and
  `io.github.joke:spock-deepmock`
- **AND** the test classpath of each module resolves them without a version of its own

#### Scenario: Final classes can be mocked
- **WHEN** a specification mocks a final PMD AST class with `Mock()`
- **THEN** the mock is created

#### Scenario: Test dependencies do not reach the POM
- **WHEN** the generated POM of either module is inspected
- **THEN** it declares no dependency

### Requirement: Spock mocks isolate the unit under test
A method under test SHALL be isolated from the sibling methods it calls by spying the subject and
stubbing those siblings, the style `static-method-state-rule`, `method-visibility-rule` and
`visible-for-testing-rule` exist to keep possible. A test needing a chain of nested collaborators
SHALL declare one `DeepMock()` instead of a mock for each link.

#### Scenario: A sibling is stubbed on a spy
- **WHEN** a specification tests a rule's `visit` method that delegates to a helper
- **THEN** it spies the rule and stubs the helper

#### Scenario: Nested collaborators use a deep mock
- **WHEN** a test needs a chain of two or more nested collaborators
- **THEN** it declares one `DeepMock()` and stubs the end of the chain
- **AND** it does not declare an intermediate mock for each link

### Requirement: Tests are split by tag and run at the compile floor
The `test` task SHALL run tests tagged `unit` and the `integrationTest` task SHALL run tests tagged
`integration`, and `check` SHALL depend on `integrationTest`. The `integrationTest` task SHALL use
the test source set's own runtime classpath, and therefore SHALL resolve PMD at the floor version the
`dependencies` platform declares.

#### Scenario: Tests are split by tag
- **WHEN** the `test` and `integrationTest` tasks are configured
- **THEN** `test` includes the `unit` tag and `integrationTest` includes the `integration` tag

#### Scenario: Integration tests run at the compile floor
- **WHEN** `integrationTest` runs
- **THEN** the PMD it resolves is the version the `dependencies` platform constrains, not the
  version the convention plugin declares for analysis

### Requirement: The annotation dependency stays off the published POM
The `pmd-rules` module SHALL declare a `compileOnly` dependency providing the `@VisibleForTesting`
annotation. It SHALL NOT be `api` or `implementation`: the published POM declares no dependencies,
and the annotation has no runtime retention requirement for consumers.

#### Scenario: The annotation dependency does not reach the POM
- **WHEN** the generated POM is inspected after the annotation dependency is added
- **THEN** it declares no dependency, because the declaration is `compileOnly`
