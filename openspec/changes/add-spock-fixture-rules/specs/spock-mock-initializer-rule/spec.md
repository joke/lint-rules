## ADDED Requirements

### Requirement: AvoidMockInitializerClosure reports inline stubbing in a mock initialiser
The artifact SHALL provide a CodeNarc rule named `AvoidMockInitializerClosure` that reports a call to
`Mock`, `Stub` or `Spy` whose last argument is a closure.

```groovy
CustomerRepository repository = Mock() {       // reported
    findById(_) >> customer
}

CustomerRepository repository = Mock()         // compliant — the interaction moves to then:
…
then:
1 * repository.findById('cust-1') >> customer
0 * _
```

An interaction declared in the initialiser sits away from the `when:` it answers and reads as
configuration rather than as verification. It also loses its cardinality: the initialiser form
stubs a return without asserting that the call happened, which is the half of the contract worth
having.

The rule SHALL be implemented in Java against the Groovy AST, extending `AbstractSpockRule` with a
nested `AbstractAstVisitor`.

#### Scenario: A Mock initialiser closure is reported
- **WHEN** a feature method contains `Repo repository = Mock() { findById(_) >> row }`
- **THEN** the rule reports one violation

#### Scenario: A Stub initialiser closure is reported
- **WHEN** a feature method contains `Repo repository = Stub() { findById(_) >> row }`
- **THEN** the rule reports one violation

#### Scenario: A Spy initialiser closure is reported
- **WHEN** a feature method contains `Service service = Spy() { helper() >> 1 }`
- **THEN** the rule reports one violation

#### Scenario: A typed initialiser closure is reported
- **WHEN** a feature method contains `def repository = Mock(Repo) { findById(_) >> row }`
- **THEN** the rule reports one violation from this rule

#### Scenario: A mock initialiser field closure is reported
- **WHEN** a specification declares the field `Repo repository = Mock() { findById(_) >> row }`
- **THEN** the rule reports one violation

### Requirement: A mock call without a trailing closure is not reported
The rule SHALL NOT report `Mock()`, `Mock(Type)`, or a call whose arguments are named parameters
rather than a closure.

`Spy(constructorArgs: [repository])` passes a map, not a closure, and is the documented way to
construct a spy over a real object. Reporting it would leave no way to declare one.

#### Scenario: A bare mock call is not reported
- **WHEN** a feature method contains `Repo repository = Mock()`
- **THEN** the rule reports no violation

#### Scenario: A typed mock call is not reported
- **WHEN** a feature method contains `def repository = Mock(Repo)`
- **THEN** the rule reports no violation from this rule

#### Scenario: A spy with constructor arguments is not reported
- **WHEN** a feature method contains `Service service = Spy(constructorArgs: [repository])`
- **THEN** the rule reports no violation

#### Scenario: A closure argument to an unrelated call is not reported
- **WHEN** a feature method contains `list.each { … }`
- **THEN** the rule reports no violation

### Requirement: Overlap with the declaration rule is deliberate
A declaration violating this rule and `DeclareMockWithExplicitType` at once SHALL be reported by
both, as `def repository = Mock(Repo) { findById(_) >> row }` does.

Each rule is independently selectable from `rulesets/groovy/joke.groovy`, so each SHALL be correct on
its own. Suppressing one report because another rule also fires would leave a hole for the consumer
who adopted only one of the two.

#### Scenario: A declaration that violates both rules is reported by both
- **WHEN** a feature method contains `def repository = Mock(Repo) { findById(_) >> row }`
- **AND** both rules are in the active rule set
- **THEN** two violations are reported, one from each rule

### Requirement: The rule is gated on the class being a specification
The rule SHALL report only within a class matching its `specificationSuperclassNames` property,
defaulting to `*Specification`, or its `specificationClassNames` property, defaulting to unset.

#### Scenario: A class that is not a specification is not reported
- **WHEN** a Groovy class that does not extend `*Specification` contains `def x = Mock(Foo) { }`
- **THEN** the rule reports no violation
