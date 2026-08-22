## ADDED Requirements

### Requirement: ValueAssertionsBelongInExpectBlock reports a value assertion in a then block
The artifact SHALL provide a CodeNarc rule named `ValueAssertionsBelongInExpectBlock` that reports a
boolean expression appearing as a top-level statement in a `then:` block or one of its `and:`
continuations.

`then:` states who the subject called. `expect:` states what it returned. A `then:` block full of
`==` buries the interaction contract in the middle of value checks, and the contract is the half a
reader most needs to find.

The rule SHALL be implemented in Java against the Groovy AST, extending `AbstractSpockRule` with a
nested `AbstractAstVisitor`.

#### Scenario: An equality assertion in a then block is reported
- **WHEN** a `then:` block contains `receipt.transactionId == 'txn-123'`
- **THEN** the rule reports one violation

#### Scenario: A relational assertion in a then block is reported
- **WHEN** a `then:` block contains `total > 0`
- **THEN** the rule reports one violation

#### Scenario: An assertion in an and block after then is reported
- **WHEN** an `and:` block following `then:` contains `receipt.total == 49.99G`
- **THEN** the rule reports one violation

#### Scenario: Several assertions are reported separately
- **WHEN** a `then:` block contains two boolean expressions
- **THEN** the rule reports two violations

#### Scenario: The same assertions in an expect block are not reported
- **WHEN** an `expect:` block contains `receipt.transactionId == 'txn-123'`
- **THEN** the rule reports no violation

### Requirement: Interactions and exception captures are not value assertions
The rule SHALL NOT report an interaction, a `thrown(...)` or `notThrown(...)` call, or a declaration
initialised from one.

#### Scenario: An interaction is not reported
- **WHEN** a `then:` block contains `1 * repository.save(customer) >> customer`
- **THEN** the rule reports no violation

#### Scenario: The strict mocking terminator is not reported
- **WHEN** a `then:` block contains `0 * _`
- **THEN** the rule reports no violation

#### Scenario: A thrown call is not reported
- **WHEN** a `then:` block contains `thrown(IllegalArgumentException)`
- **THEN** the rule reports no violation

#### Scenario: A captured exception declaration is not reported
- **WHEN** a `then:` block contains `def error = thrown(IllegalArgumentException)`
- **THEN** the rule reports no violation

#### Scenario: The exception message assertion is reported
- **WHEN** a `then:` block contains `error.message == 'order must not be null'`
- **THEN** the rule reports one violation, because the message belongs in `expect:`

### Requirement: The test is positive and its gap is stated
The rule SHALL decide what is a value assertion with CodeNarc's own boolean-expression test rather
than by treating everything that is not an interaction as one.

Sharing CodeNarc's judgement means this rule and `SpockMissingAssert` agree by construction about what
counts as a boolean expression, so a consumer running both never sees them disagree about one line.
The alternative — report anything that is not an interaction and not `thrown` — would report an
ordinary helper call in a `then:` block, which is a false positive on code that is not wrong.

The consequence SHALL be recorded rather than worked around: an expression CodeNarc's test does not
recognise as boolean is not reported. A bare truthiness check such as `receipt.valid` is the known
case. The rule under-reports rather than guesses.

#### Scenario: A helper call in a then block is not reported
- **WHEN** a `then:` block contains `service.warmUp()`
- **THEN** the rule reports no violation

#### Scenario: A bare truthiness check is not reported
- **WHEN** a `then:` block contains `receipt.valid`
- **THEN** the rule reports no violation
- **AND** the README records this as a known gap

#### Scenario: A boolean-named method call is reported
- **WHEN** a `then:` block contains `receipt.isValid()`
- **THEN** the rule reports one violation, because CodeNarc's test recognises the name as boolean

### Requirement: The rule is gated on the class and on the feature method
The rule SHALL report only within a class matching `specificationSuperclassNames` or
`specificationClassNames`, and only inside a method CodeNarc identifies as a Spock feature method.

#### Scenario: A class that is not a specification is not reported
- **WHEN** a Groovy class that does not extend `*Specification` contains a `then:` labelled comparison
- **THEN** the rule reports no violation
