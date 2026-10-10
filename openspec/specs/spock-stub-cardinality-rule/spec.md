# spock-stub-cardinality-rule Specification

## Purpose
TBD - created by syncing change avoid-cardinality-on-stub. Update Purpose after archive.
## Requirements
### Requirement: AvoidCardinalityOnStub reports a required interaction on a stub
The artifact SHALL provide a CodeNarc rule named `AvoidCardinalityOnStub` that reports an interaction
on a stub that carries a cardinality other than `_`.

```groovy
1 * stub.foo()                       // reported
0 * stub.foo()                       // reported
(1..3) * stub.foo()                  // reported
stub.foo() >> value                  // compliant
_ * stub.foo()                       // compliant
```

A stub is not verified, and Spock rejects an interaction that is required on one with
`InvalidSpecException: Stub '…' matches the following required interaction`. An interaction is
required unless its cardinality is unbounded, so even `0 *` is reported. The convention is that a
double whose calls are counted is a `Mock`.

The rule SHALL be implemented in Java against the Groovy AST, extending `AbstractSpockRule` with a
nested `AbstractAstVisitor`, and SHALL read the shared interaction classifier rather than deciding for
itself what `*` and `>>` mean.

#### Scenario: A counted stub interaction is reported
- **WHEN** a feature method declares `CustomerRepository repository = Stub()`
- **AND** its `then:` block declares `1 * repository.find(id)`
- **THEN** the rule reports one violation

#### Scenario: A zero cardinality is reported
- **WHEN** the `then:` block declares `0 * repository.find(id)`
- **THEN** the rule reports one violation

#### Scenario: A range cardinality is reported
- **WHEN** the `then:` block declares `(1..3) * repository.find(id)`
- **THEN** the rule reports one violation

#### Scenario: A range reaching zero is reported
- **WHEN** the `then:` block declares `(0..2) * repository.find(id)`
- **THEN** the rule reports one violation

#### Scenario: Each counted interaction is reported
- **WHEN** the `then:` block declares two counted interactions on the stub
- **THEN** the rule reports two violations

#### Scenario: A property access on a stub is reported
- **WHEN** the `then:` block declares `1 * repository.name`
- **THEN** the rule reports one violation

### Requirement: A response does not excuse the cardinality
The rule SHALL report an interaction that carries a cardinality and also a `>>` or `>>>` response.

`1 * stub.foo() >> value` is the form people write: the response is what they want and the count is
habit from a mock. Spock rejects it for the count, so a response does not make it compliant.

#### Scenario: A counted and stubbed interaction is reported
- **WHEN** the `then:` block declares `1 * repository.find(id) >> customer`
- **THEN** the rule reports one violation

#### Scenario: A counted interaction with a response sequence is reported
- **WHEN** the `then:` block declares `1 * repository.find(id) >>> [first, second]`
- **THEN** the rule reports one violation

#### Scenario: A stubbed interaction with no cardinality is not reported
- **WHEN** the `then:` block declares `repository.find(id) >> customer`
- **THEN** the rule reports no violation

### Requirement: The unbounded cardinality is not reported
The rule SHALL NOT report an interaction whose cardinality is the wildcard `_`, with or without a
response, because Spock does not treat it as required.

#### Scenario: A wildcard cardinality is not reported
- **WHEN** the `then:` block declares `_ * repository.find(id)`
- **THEN** the rule reports no violation

#### Scenario: A wildcard cardinality with a response is not reported
- **WHEN** the `then:` block declares `_ * repository.find(id) >> customer`
- **THEN** the rule reports no violation

### Requirement: An interaction that names no stub is not reported
The rule SHALL report only an interaction whose receiver is a recognised stub. The strict mocking
terminator `0 * _` names no double and SHALL NOT be reported.

#### Scenario: The strict mocking terminator is not reported
- **WHEN** a feature method declares a stub and its `then:` block ends with `0 * _`
- **THEN** the rule reports no violation

#### Scenario: A wildcard receiver with a method is not reported
- **WHEN** the `then:` block declares `1 * _.find(id)`
- **THEN** the rule reports no violation

### Requirement: Only stubs are in scope
The rule SHALL treat a variable as a stub only when it is declared from a `Stub(...)` call, as a local
in the feature method or a field on the specification, and SHALL stay silent for any other receiver.

A `Mock` and a `Spy` are verified and accept a cardinality. A stub arriving from a helper method, a
base class or a parameter is not recognised, and the rule stays silent rather than infer one.

#### Scenario: A mock interaction with a cardinality is not reported
- **WHEN** a feature method declares `CustomerRepository repository = Mock()`
- **AND** its `then:` block declares `1 * repository.find(id)`
- **THEN** the rule reports no violation

#### Scenario: A spy interaction with a cardinality is not reported
- **WHEN** a feature method declares `OrderService service = Spy(constructorArgs: [repository])`
- **AND** its `then:` block declares `1 * service.validate(order) >> true`
- **THEN** the rule reports no violation

#### Scenario: A stub field is recognised
- **WHEN** the specification declares the field `CustomerRepository repository = Stub()`
- **AND** a feature method's `then:` block declares `1 * repository.find(id)`
- **THEN** the rule reports one violation

#### Scenario: A stub declared with a named argument is recognised
- **WHEN** a feature method declares `CustomerRepository repository = Stub(name: 'users')`
- **AND** its `then:` block declares `1 * repository.find(id)`
- **THEN** the rule reports one violation

#### Scenario: A stub from an unrecognised source is not reported
- **WHEN** a feature method obtains its collaborator from a helper method rather than a `Stub` declaration
- **AND** its `then:` block declares `1 * collaborator.find(id)`
- **THEN** the rule reports no violation

#### Scenario: Doubles in different feature methods do not leak
- **WHEN** one feature method declares a stub `repository` and a second declares a `Mock` named `repository`
- **THEN** only a counted interaction in the first is reported

### Requirement: A verified stub is not reported
The rule SHALL NOT treat a `Stub` call that carries a `verified` named argument as a stub, whatever
its value.

`Stub(verified: true)` is allowed a required interaction. Its value may be an expression the rule
cannot resolve, and the family under-reports rather than infers.

#### Scenario: A verified stub is not reported
- **WHEN** a feature method declares `CustomerRepository repository = Stub(verified: true)`
- **AND** its `then:` block declares `1 * repository.find(id)`
- **THEN** the rule reports no violation

#### Scenario: A verified stub with a type argument is not reported
- **WHEN** a feature method declares `def repository = Stub(CustomerRepository, verified: true)`
- **AND** its `then:` block declares `1 * repository.find(id)`
- **THEN** the rule reports no violation

### Requirement: Interactions inside an interaction block are not read
The rule SHALL NOT visit an interaction written inside an `interaction { }` closure, the boundary the
other interaction rules document.

#### Scenario: An interaction block is not read
- **WHEN** a feature method declares a stub and an `interaction { 1 * repository.find(id) }` block
- **THEN** the rule reports no violation

### Requirement: The rule is gated on the class and on the feature method
The rule SHALL report only within a class matching `specificationSuperclassNames`, defaulting to
`*Specification`, or `specificationClassNames`, defaulting to unset, and only inside a method CodeNarc
identifies as a Spock feature method.

#### Scenario: A class that is not a specification is not reported
- **WHEN** a Groovy class that does not extend `*Specification` declares `Stub()` and `1 * stub.foo()`
- **THEN** the rule reports no violation

#### Scenario: specificationClassNames widens the gate
- **WHEN** `specificationClassNames` names a class that extends nothing and that class holds the violation
- **THEN** the rule reports one violation
