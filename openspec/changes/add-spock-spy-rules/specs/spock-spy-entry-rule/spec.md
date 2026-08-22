## ADDED Requirements

### Requirement: RequireSpyEntryInteraction reports a spy interaction list with no entry interaction
The artifact SHALL provide a CodeNarc rule named `RequireSpyEntryInteraction` that reports a `then:`
block declaring one or more specific interactions on a `Spy` without also declaring `1 * spy._` for
that spy.

A spy's own methods count as interactions under strict mocking. The method under test is itself
invoked once, by the `when:` block, and that entry call is an interaction on the spy like any other —
so without `1 * spy._` to account for it, the `0 * _` terminator fails on the very call the feature
method exists to make.

The rule SHALL be implemented in Java against the Groovy AST, extending `AbstractSpockRule` with a
nested `AbstractAstVisitor`, and SHALL read the shared feature-method model rather than partitioning
statements itself.

#### Scenario: A spy with sibling interactions and no entry interaction is reported
- **WHEN** a feature method declares `OrderService service = Spy(constructorArgs: [repository])`
- **AND** its `then:` block declares `1 * service.validate(order)` and `0 * _` but no `1 * service._`
- **THEN** the rule reports one violation

#### Scenario: A spy with the entry interaction is not reported
- **WHEN** the same `then:` block declares `1 * service.validate(order)`, `1 * service._` and `0 * _`
  in that order
- **THEN** the rule reports no violation

#### Scenario: Two spies each require their own entry interaction
- **WHEN** a feature method declares two spies and its `then:` block declares a specific interaction
  on each, with an entry interaction for only one
- **THEN** the rule reports one violation

#### Scenario: Each then block is judged separately
- **WHEN** a feature method has two `then:` blocks, both declaring spy interactions, and only the
  first carries the entry interaction
- **THEN** the rule reports one violation

### Requirement: The entry interaction is required only where a specific spy interaction exists
The rule SHALL NOT report a `then:` block that declares no interaction on the spy, even where a spy
is in scope. `1 * spy._` SHALL NOT itself count as the specific interaction that triggers the
requirement.

A block that constrains nothing on the spy is not about to fail its terminator on the entry call, so
the line would assert nothing. Counting the remedy as its own trigger would satisfy the rule in
exactly the blocks that never needed it.

#### Scenario: A then block with no spy interaction is not reported
- **WHEN** a feature method declares a spy, and its `then:` block declares only
  `1 * repository.persist(order)` and `0 * _`
- **THEN** the rule reports no violation

#### Scenario: A lone entry interaction does not trigger the requirement
- **WHEN** a `then:` block declares `1 * service._` and `0 * _` and no other interaction on the spy
- **THEN** the rule reports no violation

#### Scenario: A feature method with no spy is not reported
- **WHEN** a feature method declares only `Mock()` collaborators
- **THEN** the rule reports no violation

### Requirement: The entry interaction SHALL follow the specific interactions
The rule SHALL report an entry interaction declared before any specific interaction on the same spy.

Spock matches a declared interaction against a call in declaration order, and `1 * spy._` matches
every method on the spy. Declared first it absorbs the sibling calls that the specific interactions
were written to verify, and those then fail their own counts. The specification still runs and still
fails, but somewhere other than where the mistake is.

The required tail of a spy `then:` block is therefore fixed: the specific interactions, then
`1 * spy._`, then the `0 * _` that `RequireStrictMockingTerminator` requires.

#### Scenario: An entry interaction before the specific interactions is reported
- **WHEN** a `then:` block declares `1 * service._`, then `1 * service.validate(order)`, then `0 * _`
- **THEN** the rule reports one violation

#### Scenario: An entry interaction between two specific interactions is reported
- **WHEN** a `then:` block declares a specific spy interaction, then `1 * service._`, then a further
  specific spy interaction
- **THEN** the rule reports one violation

#### Scenario: An entry interaction after a non-spy interaction is not reported
- **WHEN** a `then:` block declares `1 * service.validate(order)`, `1 * repository.persist(order)`,
  `1 * service._` and `0 * _` in that order
- **THEN** the rule reports no violation

### Requirement: A spy is recognised from its declaration
The rule SHALL recognise a spy from a declaration whose initialiser is a `Spy` call, covering
`Spy(Type)`, `Spy(constructorArgs: [...])` and `Spy(realInstance)`, declared either as a local in the
feature method or as a field on the specification.

A spy reaching the feature method from anywhere else — a helper method, a base class, a parameter —
SHALL NOT be recognised, and the rule SHALL stay silent rather than infer one.

Inferring would demand `1 * x._` for a variable that is not a spy, and the only way to satisfy that
demand is to add a line that breaks the specification. Under-reporting is the family's standing
posture and it is the safe direction here in particular.

#### Scenario: A typed spy declaration is recognised
- **WHEN** a feature method declares `OrderService service = Spy(constructorArgs: [repository])`
- **THEN** the rule treats `service` as a spy

#### Scenario: A spy over a real instance is recognised
- **WHEN** a feature method declares `OrderService service = Spy(realService)`
- **THEN** the rule treats `service` as a spy

#### Scenario: A spy field is recognised
- **WHEN** the specification declares the field `OrderService service = Spy(constructorArgs: [repo])`
- **THEN** the rule treats `service` as a spy in every feature method

#### Scenario: A spy from an unrecognised source is not reported
- **WHEN** a feature method obtains its subject from a helper method rather than a `Spy` declaration
- **THEN** the rule reports no violation

### Requirement: The rule is gated on the class and on the feature method
The rule SHALL report only within a class matching `specificationSuperclassNames` or
`specificationClassNames`, and only inside a method CodeNarc identifies as a Spock feature method.

#### Scenario: A class that is not a specification is not reported
- **WHEN** a Groovy class that does not extend `*Specification` declares a `Spy` and a `then:` label
- **THEN** the rule reports no violation
