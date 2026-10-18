# Spec Delta

## MODIFIED Requirements

### Requirement: Rule tests are Spock specifications
The `codenarc-rules` module SHALL test its rules with Spock specifications under
`codenarc-rules/src/test/groovy`, driving CodeNarc's `AbstractRuleTestCase` where it is useful.

Violating and compliant samples are embedded in the specification as Groovy strings, because a
CodeNarc sample is a short snippet fed to the rule, not a file the build would otherwise compile or
analyse.

Every specification SHALL carry a `@spock.lang.Tag`, either `unit` or `integration`, because the
`test` / `integrationTest` split selects on it and an untagged specification runs in neither task.
Rule specifications SHALL be tagged `unit`, so that Pitest's `includedGroups` selects them.

The classpath resolution of the shipped rulesets SHALL be exercised by a specification tagged
`integration`, mirroring the PMD module's ruleset distribution tests: resolution from the `codenarc`
configuration is the single mechanism the distribution model depends on, so it SHALL be exercised on
every build rather than discovered by the first consumer.

That specification SHALL initialise CodeNarc's rule registry before loading a ruleset.
`CodeNarcRunner` populates the registry on a real run and nothing populates it when `RuleSetUtil` is
driven directly, so without the initialisation the strict ruleset fails to load with `No such rule
named [...]` — a message that reads as a distribution fault rather than an uninitialised registry.

#### Scenario: Specifications are tagged
- **WHEN** a Spock specification in `codenarc-rules` is inspected
- **THEN** it carries `@spock.lang.Tag` naming either `unit` or `integration`

#### Scenario: Rule specifications reach mutation analysis
- **WHEN** `./gradlew test` and `./gradlew pitest` run
- **THEN** both execute the `unit`-tagged rule specifications

#### Scenario: A missing or misnamed ruleset resource fails the build
- **WHEN** a shipped ruleset is absent from the jar or named differently
- **THEN** the `integration`-tagged specification fails

#### Scenario: Samples live in the specification
- **WHEN** a rule's test data is inspected
- **THEN** every violating and compliant sample is embedded in the specification
- **AND** no separate fixture file carries a deliberate violation
