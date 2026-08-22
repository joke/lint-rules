# spock-block-label-rule Specification

## Purpose
TBD - created by archiving change add-spock-fixture-rules. Update Purpose after archive.
## Requirements
### Requirement: AvoidSetupAndGivenLabels reports a setup or given label
The artifact SHALL provide a CodeNarc rule named `AvoidSetupAndGivenLabels` that reports a `setup:`
or `given:` statement label inside a Spock feature method.

Spock treats unlabelled statements at the top of a feature method as the implicit setup block, so
the label states what the statement's position already says. Removed, the blank line before `when:`
carries the boundary.

The rule SHALL be implemented in Java against the Groovy AST, extending `AbstractSpockRule` with a
nested `AbstractAstVisitor`.

#### Scenario: A setup label is reported
- **WHEN** a feature method contains a statement labelled `setup:`
- **THEN** the rule reports one violation

#### Scenario: A given label is reported
- **WHEN** a feature method contains a statement labelled `given:`
- **THEN** the rule reports one violation

#### Scenario: Unlabelled fixture statements are not reported
- **WHEN** a feature method opens with unlabelled statements followed by `when:`
- **THEN** the rule reports no violation

#### Scenario: Both labels in one method are reported separately
- **WHEN** a feature method contains a `given:` label and a later `setup:` label
- **THEN** the rule reports two violations

### Requirement: Only setup and given are reported
The rule SHALL NOT report `when:`, `then:`, `expect:`, `where:`, `cleanup:`, `and:`, `filter:` or
`combined:`.

`and:` is a continuation with no semantic weight and is excluded from CodeNarc's own label
vocabulary for that reason. `cleanup:` and `where:` have no unlabelled equivalent — the label is the
only way to declare them — so reporting them would demand a rewrite that does not exist.

#### Scenario: The other Spock labels are not reported
- **WHEN** a feature method uses `when:`, `then:`, `expect:`, `cleanup:`, `and:` and `where:`
- **THEN** the rule reports no violation

#### Scenario: A non-Spock label is not reported
- **WHEN** a method contains a loop labelled `outer:`
- **THEN** the rule reports no violation

### Requirement: A setup fixture method is not a feature method
The rule SHALL report only inside a method CodeNarc identifies as a Spock feature method, which is a
method carrying at least one Spock statement label. A `def setup()` or `def setupSpec()` fixture
method carrying no labels SHALL NOT be reported.

Spock's own `SpecParser` identifies a feature method by the presence of a statement label, and
matching that definition is what keeps the rule from reporting the fixture methods whose *name* it
shares.

#### Scenario: A setup fixture method is not reported
- **WHEN** a specification declares `def setup() { … }` with no statement labels
- **THEN** the rule reports no violation

#### Scenario: A setupSpec fixture method is not reported
- **WHEN** a specification declares `def setupSpec() { … }` with no statement labels
- **THEN** the rule reports no violation

### Requirement: Stacked labels are reported once each
The rule SHALL report each reportable label a statement carries, and SHALL NOT report one statement
twice for the same label.

Where a label introduces an empty block, Groovy attaches every label to the following statement, so
one statement can carry several.

#### Scenario: An empty given block followed by when is reported once
- **WHEN** a feature method contains `given:` immediately followed by `when:` and one statement
- **THEN** the rule reports one violation

### Requirement: The rule is gated on the class being a specification
The rule SHALL report only within a class matching its `specificationSuperclassNames` property,
defaulting to `*Specification`, or its `specificationClassNames` property, defaulting to unset.

#### Scenario: A class that is not a specification is not reported
- **WHEN** a Groovy class that does not extend `*Specification` contains a `given:` label
- **THEN** the rule reports no violation

#### Scenario: A consumer can widen the gate
- **WHEN** `specificationClassNames` is set to a pattern matching the class
- **THEN** the rule reports the violation
