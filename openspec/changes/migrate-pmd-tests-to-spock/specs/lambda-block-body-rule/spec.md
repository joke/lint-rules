# Spec Delta

## MODIFIED Requirements

### Requirement: The rule is covered by unit tests and mutation testing
The rule SHALL be tested by a Spock specification that runs it through PMD against fixture files
covering every reported and every non-reported case above, and SHALL meet the project's 100%
mutation, coverage and test strength thresholds.

The fixtures SHALL be treated as the only evidence the rule works. This repository contains no block
lambda in Java source, so dogfooding cannot catch a mistake in this rule.

The rule class SHALL itself comply with every rule this artifact publishes.

#### Scenario: Every specified case has a fixture
- **WHEN** the rule's test data is inspected
- **THEN** it contains a case for each reported and each non-reported scenario in this spec

#### Scenario: Mutation thresholds are met
- **WHEN** `./gradlew pitest` runs
- **THEN** the rule class meets 100% mutation coverage, line coverage and test strength
