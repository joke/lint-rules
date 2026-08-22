## ADDED Requirements

### Requirement: InteractionsBelongInThenBlock reports an interaction outside a then block
The artifact SHALL provide a CodeNarc rule named `InteractionsBelongInThenBlock` that reports a Spock
interaction appearing anywhere in a feature method other than a `then:` block or one of its `and:`
continuations.

An interaction declares what the subject must call. Placed above `when:` it reads as configuration
rather than verification and sits away from the action it answers; placed in `expect:` it mixes the
collaboration contract into the value assertions.

The rule SHALL be implemented in Java against the Groovy AST, extending `AbstractSpockRule` with a
nested `AbstractAstVisitor`.

#### Scenario: An interaction in the setup region is reported
- **WHEN** a feature method declares `1 * repository.save(customer)` before any label
- **THEN** the rule reports one violation

#### Scenario: A stubbed return in the setup region is reported
- **WHEN** a feature method declares `repository.findById('cust-1') >> customer` before any label
- **THEN** the rule reports one violation

#### Scenario: An interaction in a when block is reported
- **WHEN** a `when:` block contains `1 * repository.save(customer)`
- **THEN** the rule reports one violation

#### Scenario: An interaction in an expect block is reported
- **WHEN** an `expect:` block contains `1 * repository.save(customer)`
- **THEN** the rule reports one violation

#### Scenario: An interaction in a then block is not reported
- **WHEN** a `then:` block contains `1 * repository.save(customer) >> customer`
- **THEN** the rule reports no violation

#### Scenario: A value assertion outside a then block is not reported
- **WHEN** an `expect:` block contains `receipt.total == 49.99G`
- **THEN** the rule reports no violation

### Requirement: An and block continues the block it follows
The rule SHALL treat `and:` as a continuation of the preceding labelled block rather than as a block
of its own, matching CodeNarc's own label vocabulary, which excludes `and:` because it carries no
semantic weight.

#### Scenario: An interaction in an and block after then is not reported
- **WHEN** a `then:` block is followed by `and:` containing `1 * eventBus.publish(event)`
- **THEN** the rule reports no violation

#### Scenario: An interaction in an and block after when is reported
- **WHEN** a `when:` block is followed by `and:` containing `1 * repository.save(customer)`
- **THEN** the rule reports one violation

### Requirement: The interaction forms the rule recognises are stated
The rule SHALL recognise as an interaction a top-level statement whose expression is a cardinality
form (`n * target`), a stubbed-return form (`target >> value` or `target >>> values`), the two
combined (`n * target >> value`), or a call to `interaction` taking a closure.

Cardinality SHALL be a constant integer, a range, or `_`. A target SHALL be a method call, a property
access, or a bare variable.

Recognition SHALL be by shape alone. The rule SHALL NOT attempt to determine whether the target is a
mock, a stub, a spy or a real object, because CodeNarc analyses source without a compile classpath
and the shape is what distinguishes an interaction in the first place.

#### Scenario: A range cardinality is recognised
- **WHEN** a setup statement contains `(1..3) * repository.save(customer)`
- **THEN** the rule reports one violation

#### Scenario: A wildcard cardinality is recognised
- **WHEN** a setup statement contains `_ * repository.save(customer)`
- **THEN** the rule reports one violation

#### Scenario: The combined form is recognised
- **WHEN** a setup statement contains `1 * repository.findById('x') >> customer`
- **THEN** the rule reports one violation

#### Scenario: An interaction block is recognised
- **WHEN** a setup statement contains `interaction { 1 * repository.save(customer) }`
- **THEN** the rule reports one violation

#### Scenario: An ordinary method call is not an interaction
- **WHEN** a setup statement contains `service.warmUp()`
- **THEN** the rule reports no violation

#### Scenario: An arithmetic multiplication is not an interaction
- **WHEN** a setup statement contains `def total = 2 * price`
- **THEN** the rule reports no violation

### Requirement: The rule is gated on the class and on the feature method
The rule SHALL report only within a class matching `specificationSuperclassNames` or
`specificationClassNames`, and only inside a method CodeNarc identifies as a Spock feature method.

#### Scenario: A class that is not a specification is not reported
- **WHEN** a Groovy class that does not extend `*Specification` contains `1 * repo.save(x)`
- **THEN** the rule reports no violation

#### Scenario: A fixture method is not reported
- **WHEN** `def setup()` carrying no statement labels contains `1 * repository.save(customer)`
- **THEN** the rule reports no violation
