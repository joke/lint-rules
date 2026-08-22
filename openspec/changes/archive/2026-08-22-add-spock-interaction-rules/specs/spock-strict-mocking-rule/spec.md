## ADDED Requirements

### Requirement: RequireStrictMockingTerminator reports a then block that does not end with 0 * _
The artifact SHALL provide a CodeNarc rule named `RequireStrictMockingTerminator` that reports a
`then:` block whose last top-level statement is not `0 * _`.

`0 * _` asserts that no interaction other than the declared ones happened on any double. Without it a
specification silently tolerates an extra call, which is the regression strict mocking exists to
catch: an unplanned collaborator call should break a test until someone declares it on purpose.

The rule SHALL be implemented in Java against the Groovy AST, extending `AbstractSpockRule` with a
nested `AbstractAstVisitor`.

#### Scenario: A then block ending with the terminator is not reported
- **WHEN** a `then:` block contains an interaction followed by `0 * _`
- **THEN** the rule reports no violation

#### Scenario: A then block with no terminator is reported
- **WHEN** a `then:` block contains `1 * repository.save(customer)` and nothing else
- **THEN** the rule reports one violation

#### Scenario: A terminator that is not last is reported
- **WHEN** a `then:` block contains `0 * _` followed by `1 * repository.save(customer)`
- **THEN** the rule reports one violation

#### Scenario: A non-zero cardinality on the wildcard is not a terminator
- **WHEN** a `then:` block ends with `1 * _`
- **THEN** the rule reports one violation

#### Scenario: Each then block is judged separately
- **WHEN** a feature method has two `then:` blocks and only the first ends with `0 * _`
- **THEN** the rule reports one violation

### Requirement: The terminator is required unconditionally
The rule SHALL report an unterminated `then:` block whether or not the feature method declares a
`Mock`, `Stub` or `Spy`.

Requiring it only when a double is in scope was rejected on the failing case. A specification with no
collaborators today acquires one the moment the subject grows a dependency, and a conditional rule
goes quiet exactly then: the `then:` block that was compliant yesterday is unterminated and nothing
reports it. The assertion is also not vacuous without doubles — it is cheap, and it stays true by
construction, which is what makes it safe to leave standing.

A `then:` block whose only statement is the terminator is therefore a valid and expected shape.

#### Scenario: A then block with no mocks in scope is still reported
- **WHEN** a feature method declares no `Mock`, `Stub` or `Spy`, and its `then:` block contains only
  `rule.name == 'Renamed'`
- **THEN** the rule reports one violation

#### Scenario: A then block containing only the terminator is not reported
- **WHEN** a `then:` block contains `0 * _` and nothing else
- **THEN** the rule reports no violation

### Requirement: A then and its and continuations are terminated once
The rule SHALL treat a `then:` block and every `and:` block following it as one run, requiring the
terminator at the end of the run rather than at the end of each `and:`.

`and:` carries no semantic weight and is excluded from CodeNarc's own label vocabulary for that
reason. Requiring a terminator per `and:` would assert "nothing else happened" several times in the
middle of a list still being declared.

#### Scenario: A terminator at the end of a trailing and block is accepted
- **WHEN** `then:` contains an interaction, `and:` contains another, and the `and:` ends with `0 * _`
- **THEN** the rule reports no violation

#### Scenario: A terminator before a trailing and block is reported
- **WHEN** `then:` ends with `0 * _` and is followed by `and:` containing a further interaction
- **THEN** the rule reports one violation

#### Scenario: A then and and run with no terminator anywhere is reported once
- **WHEN** `then:` and two following `and:` blocks contain interactions and no `0 * _`
- **THEN** the rule reports one violation

### Requirement: Only then blocks require the terminator
The rule SHALL NOT require `0 * _` in `when:`, `expect:`, `where:`, `cleanup:`, `setup:` or `given:`
blocks, and SHALL NOT report a feature method that has no `then:` block at all.

A feature method with no collaborators and a standalone `expect:` is a documented shape in the
conventions this rule enforces. Demanding a `then:` block there would demand a block with nothing to
verify.

#### Scenario: A feature method with only an expect block is not reported
- **WHEN** a feature method contains unlabelled setup statements and one `expect:` block
- **THEN** the rule reports no violation

#### Scenario: An expect block without a terminator is not reported
- **WHEN** an `expect:` block contains value assertions and no `0 * _`
- **THEN** the rule reports no violation

### Requirement: The rule is gated on the class and on the feature method
The rule SHALL report only within a class matching `specificationSuperclassNames` or
`specificationClassNames`, and only inside a method CodeNarc identifies as a Spock feature method.

#### Scenario: A class that is not a specification is not reported
- **WHEN** a Groovy class that does not extend `*Specification` contains a `then:` labelled statement
- **THEN** the rule reports no violation
