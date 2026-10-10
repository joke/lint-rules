# spock-spy-response-rule Specification

## Purpose
TBD - created by syncing change tighten-spock-double-declarations. Update Purpose after archive.
## Requirements
### Requirement: RequireSpyInteractionResponse reports a spy interaction with no response
The artifact SHALL provide a CodeNarc rule named `RequireSpyInteractionResponse` that reports a counted
interaction on a spy that carries neither a `>>` nor a `>>>` response.

Without a response, a spy interaction calls the *real* method. That is rarely what a line such as
`1 * service.validate(order)` reads as, so the call-through must be stated: either a value or closure
response, or `>> { callRealMethod() }` where the real method is meant.

```groovy
1 * service.validate(order)                          // reported
1 * service.validate(order) >> true                  // compliant
1 * service.validate(order) >> { callRealMethod() }  // compliant
```

The rule SHALL be implemented in Java against the Groovy AST, extending `AbstractSpockRule` with a
nested `AbstractAstVisitor`, and SHALL read the shared interaction classifier rather than deciding for
itself what `*` and `>>` mean.

#### Scenario: A spy interaction with no response is reported
- **WHEN** a feature method declares `OrderService service = Spy(constructorArgs: [repository])`
- **AND** its `then:` block declares `1 * service.validate(order)`
- **THEN** the rule reports one violation

#### Scenario: A spy interaction with a value response is not reported
- **WHEN** the `then:` block declares `1 * service.validate(order) >> true`
- **THEN** the rule reports no violation

#### Scenario: A spy interaction with a closure response is not reported
- **WHEN** the `then:` block declares `1 * service.validate(order) >> { callRealMethod() }`
- **THEN** the rule reports no violation

#### Scenario: A spy interaction with a response sequence is not reported
- **WHEN** the `then:` block declares `1 * service.validate(order) >>> [true, false]`
- **THEN** the rule reports no violation

#### Scenario: A wildcard cardinality is reported like a count
- **WHEN** the `then:` block declares `_ * service.validate(order)`
- **THEN** the rule reports one violation

#### Scenario: A range cardinality is reported like a count
- **WHEN** the `then:` block declares `(1..3) * service.validate(order)`
- **THEN** the rule reports one violation

#### Scenario: Each unstubbed interaction is reported
- **WHEN** the `then:` block declares two spy interactions with no response
- **THEN** the rule reports two violations

### Requirement: Only the bare entry interaction is exempt
The rule SHALL NOT report `1 * spy._`, the property-access form whose property is literally `_`, at
any cardinality. It SHALL report every other interaction on a spy that has no response, including
`1 * spy._(argument)` and any interaction whose method name is matched by pattern.

`1 * spy._` is the deliberate, short way to allow the spy's one self-call from `when:`, and
`RequireSpyEntryInteraction` requires it. The argument form is a filter over every method's argument,
and a pattern-matched name selects methods nobody named; either can call real code the author never
listed. The exemption does not read the cardinality, as `RequireSpyEntryInteraction` does not: the two
rules SHALL NOT disagree about `2 * service._`.

#### Scenario: The entry interaction is not reported
- **WHEN** the `then:` block declares `1 * service._`
- **THEN** the rule reports no violation

#### Scenario: The entry interaction at another cardinality is not reported
- **WHEN** the `then:` block declares `2 * service._`
- **THEN** the rule reports no violation

#### Scenario: A wildcard method with an argument is reported
- **WHEN** the `then:` block declares `1 * service._(order)`
- **THEN** the rule reports one violation

#### Scenario: A pattern-matched method name is reported
- **WHEN** the `then:` block declares `1 * service./validate.*/(order)`
- **THEN** the rule reports one violation

#### Scenario: The required tail satisfies both spy rules
- **WHEN** a `then:` block declares `1 * service.validate(order) >> true`, `1 * service._` and `0 * _`
- **THEN** neither this rule nor `RequireSpyEntryInteraction` reports a violation

### Requirement: A zero cardinality is not reported
The rule SHALL NOT report an interaction whose cardinality is the literal `0`, because the call is
asserted never to happen and nothing can call through.

#### Scenario: A never-called spy method is not reported
- **WHEN** the `then:` block declares `0 * service.audit(order)`
- **THEN** the rule reports no violation

### Requirement: Only spies are in scope
The rule SHALL treat a variable as a spy only when it is declared from a `Spy(...)` call, as a local in
the feature method or a field on the specification, and SHALL stay silent for any other receiver.

A `Mock` returns a default and a `Stub` returns an empty value; neither calls real code, so the rule
has nothing to say about them. A spy arriving from a helper method, a base class or a parameter is not
recognised, and the rule stays silent rather than infer one.

#### Scenario: A mock interaction with no response is not reported
- **WHEN** a feature method declares `CustomerRepository repository = Mock()`
- **AND** its `then:` block declares `1 * repository.persist(order)`
- **THEN** the rule reports no violation

#### Scenario: A spy field is recognised
- **WHEN** the specification declares the field `OrderService service = Spy(constructorArgs: [repo])`
- **AND** a feature method's `then:` block declares `1 * service.validate(order)`
- **THEN** the rule reports one violation

#### Scenario: A spy from an unrecognised source is not reported
- **WHEN** a feature method obtains its subject from a helper method rather than a `Spy` declaration
- **AND** its `then:` block declares `1 * subject.validate(order)`
- **THEN** the rule reports no violation

#### Scenario: Spies in different feature methods do not leak
- **WHEN** one feature method declares a spy `service` and a second declares a `Mock` named `service`
- **THEN** only an unstubbed interaction in the first is reported

### Requirement: The rule is gated on the class and on the feature method
The rule SHALL report only within a class matching `specificationSuperclassNames` or
`specificationClassNames`, and only inside a method CodeNarc identifies as a Spock feature method.

#### Scenario: A class that is not a specification is not reported
- **WHEN** a Groovy class that does not extend `*Specification` declares a `Spy` and `1 * spy.foo()`
- **THEN** the rule reports no violation
