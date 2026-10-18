# Spec Delta

## REMOVED Requirements

### Requirement: The rule is covered by unit tests and mutation testing
**Reason**: The fixture-per-case evidence model is replaced. Completeness now rests on isolated unit
specifications and the integration specification is a smoke check.
**Migration**: Replaced by "The rule is covered by isolated unit tests and a smoke fixture". Every
case the old fixtures carried either becomes a unit case or is dropped because it tested PMD itself.

## ADDED Requirements

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

### Requirement: The integration fixture is the evidence the rule works in PMD
The rule's integration fixture SHALL be treated as the only evidence that the rule works in PMD:
this repository declares no anonymous class, so dogfooding cannot catch a mistake in this rule.

#### Scenario: The fixture is the only check against real PMD
- **WHEN** the rule's evidence is inspected
- **THEN** the integration specification is the only specification that runs the rule in PMD

### Requirement: The rule class complies with this artifact's own rules
The rule class SHALL itself comply with every rule this artifact publishes.

#### Scenario: The rule class is analysed by the published rules
- **WHEN** `./gradlew check` runs
- **THEN** `pmdMain` reports no violation against the rule class
