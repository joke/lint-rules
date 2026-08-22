# spock-interaction-argument-rule Specification

## Purpose
TBD - created by archiving change add-spock-interaction-rules. Update Purpose after archive.
## Requirements
### Requirement: RequireValidatedInteractionArguments reports an argument that matches anything
The artifact SHALL provide a CodeNarc rule named `RequireValidatedInteractionArguments` that reports
an interaction argument which places no constraint on the value passed.

A mocked interaction is a contract about what the subject passes its collaborator. An argument that
matches anything lets a wrong value through and leaves the contract asserting only that a call
happened, which is the weaker half.

The rule SHALL be implemented in Java against the Groovy AST, extending `AbstractSpockRule` with a
nested `AbstractAstVisitor`.

#### Scenario: A bare wildcard argument is reported
- **WHEN** a `then:` block contains `1 * repository.save(_)`
- **THEN** the rule reports one violation

#### Scenario: An exact value is not reported
- **WHEN** a `then:` block contains `1 * repository.save(expectedCustomer)`
- **THEN** the rule reports no violation

#### Scenario: A discriminating constraint closure is not reported
- **WHEN** a `then:` block contains `1 * repository.save({ it.id == 'cust-1' })`
- **THEN** the rule reports no violation

#### Scenario: A verifyAll constraint closure is not reported
- **WHEN** a `then:` block contains a constraint closure whose body is `verifyAll(c) { … }`
- **THEN** the rule reports no violation

#### Scenario: Each unconstrained argument is reported
- **WHEN** a `then:` block contains `1 * repository.saveBoth(_, _)`
- **THEN** the rule reports two violations

### Requirement: The rejected forms are the wildcard, the typed wildcard, the spread wildcard and the always-true closure
The rule SHALL report these four argument forms:

| form | why |
|---|---|
| `_` | matches any value |
| `_ as Type` | matches every instance of the type; the argument itself is still unchecked |
| `*_` | matches any number of any arguments |
| a closure whose body is a single truthy constant | constrains nothing while looking as though it does |

`_ as Type` is included. It carries more information than a bare `_`, which makes it the closest call
in the set, but the rule's subject is whether the *argument* was the right one and a type accepts every
instance. A project that disagrees excludes the rule.

For the closure form the parameter list SHALL be irrelevant: `{ true }`, `{ _ -> true }` and
`{ it -> true }` differ only in a parameter that the body never reads, so the rule SHALL examine the
body alone. A body of one statement whose expression is a constant truthy under Groovy truth — `true`,
a non-zero number, a non-empty string — SHALL be reported.

#### Scenario: A typed wildcard is reported
- **WHEN** a `then:` block contains `1 * repository.save(_ as Customer)`
- **THEN** the rule reports one violation

#### Scenario: A spread wildcard is reported
- **WHEN** a `then:` block contains `1 * repository.save(*_)`
- **THEN** the rule reports one violation

#### Scenario: An always-true closure is reported
- **WHEN** a `then:` block contains `1 * repository.save({ true })`
- **THEN** the rule reports one violation

#### Scenario: An always-true closure with an underscore parameter is reported
- **WHEN** a `then:` block contains `1 * repository.save({ _ -> true })`
- **THEN** the rule reports one violation

#### Scenario: An always-true closure with an it parameter is reported
- **WHEN** a `then:` block contains `1 * repository.save({ it -> true })`
- **THEN** the rule reports one violation

#### Scenario: A truthy constant other than true is reported
- **WHEN** a `then:` block contains `1 * repository.save({ 1 })`
- **THEN** the rule reports one violation

#### Scenario: A closure returning its own parameter is not reported
- **WHEN** a `then:` block contains `1 * repository.save({ it })`
- **THEN** the rule reports no violation
- **AND** the README records this as a known gap, because detecting it would begin a general
  truthiness analysis the rule declines to start

### Requirement: Every interaction form and every kind of double is covered
The rule SHALL apply to the cardinality form, the stubbed-return form and the two combined, wherever
the interaction appears, and regardless of whether the target is a mock, a stub or a spy.

The rule SHALL NOT attempt to determine which factory produced the target. CodeNarc analyses source
without a compile classpath, and the shape of an interaction is what identifies it — so covering all
three kinds costs nothing rather than requiring three code paths.

#### Scenario: A stubbed return without cardinality is reported
- **WHEN** a `then:` block contains `repository.findById(_) >> customer`
- **THEN** the rule reports one violation

#### Scenario: A combined cardinality and stub is reported
- **WHEN** a `then:` block contains `1 * repository.findById(_) >> customer`
- **THEN** the rule reports one violation

#### Scenario: An interaction on a stub is reported
- **WHEN** the target was declared with `Stub()` and the interaction passes `_`
- **THEN** the rule reports one violation

#### Scenario: An interaction on a spy is reported
- **WHEN** the target was declared with `Spy()` and the interaction passes `_`
- **THEN** the rule reports one violation

#### Scenario: An interaction inside a mock initialiser closure is reported
- **WHEN** a declaration contains `Mock() { findById(_) >> row }`
- **THEN** the rule reports one violation
- **AND** `AvoidMockInitializerClosure` also reports the declaration, because each rule is
  individually selectable and must be correct on its own

### Requirement: Only argument positions are examined
The rule SHALL examine the arguments of the interaction's target call and nothing else. `_` in target
position SHALL NOT be reported.

`0 * _` is required by `RequireStrictMockingTerminator`, and `1 * subject._` is the spy entry
interaction. A rule that reported `_` wherever it appeared would report what its own family mandates.

#### Scenario: The strict mocking terminator is not reported
- **WHEN** a `then:` block contains `0 * _`
- **THEN** the rule reports no violation

#### Scenario: A spy entry interaction is not reported
- **WHEN** a `then:` block contains `1 * service._`
- **THEN** the rule reports no violation

#### Scenario: A wildcard method name is not reported
- **WHEN** a `then:` block contains `1 * repository._(expectedCustomer)`
- **THEN** the rule reports no violation, because the wildcard is in the method position

### Requirement: The rule is gated on the class and on the feature method
The rule SHALL report only within a class matching `specificationSuperclassNames` or
`specificationClassNames`. It SHALL report an interaction wherever it appears in such a class,
including in a field initialiser, so that an interaction moved out of a `then:` block to escape the
rule is still reported.

#### Scenario: A class that is not a specification is not reported
- **WHEN** a Groovy class that does not extend `*Specification` contains `1 * repo.save(_)`
- **THEN** the rule reports no violation

#### Scenario: An interaction in the setup region is still checked
- **WHEN** a feature method declares `1 * repository.save(_)` before any label
- **THEN** the rule reports one violation
- **AND** `InteractionsBelongInThenBlock` also reports it
