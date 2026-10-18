# Spec Delta

## MODIFIED Requirements

### Requirement: The rule is covered by unit tests and mutation testing
The rule SHALL be tested by a Spock specification that runs it through PMD against fixture files
covering every reported and every non-reported case above, and SHALL meet the project's 100%
mutation, coverage and test strength thresholds.

The fixtures SHALL be treated as the only evidence this rule works: every fully-qualified name in
this repository's Java source sits inside an import declaration, so dogfooding cannot catch a mistake
in it.

#### Scenario: Every specified case has a fixture
- **WHEN** the rule's test data is inspected
- **THEN** it contains a case for each reported and each non-reported scenario in this spec

#### Scenario: Mutation thresholds are met
- **WHEN** `./gradlew pitest` runs
- **THEN** the rule class meets 100% mutation coverage, line coverage and test strength
