## ADDED Requirements

### Requirement: DeclareMockWithExplicitType reports an untyped mock declaration
The artifact SHALL provide a CodeNarc rule named `DeclareMockWithExplicitType` that reports a
dynamically-typed declaration whose initialiser is a call to `Mock`, `Stub` or `Spy`.

```groovy
def repository = Mock(CustomerRepository)      // reported
CustomerRepository repository = Mock()         // compliant
```

The declared type is what a reader looks at to learn who the subject collaborates with. Moved into
the initialiser it is still present but no longer in the position that answers the question, and the
variable itself is untyped for every later line that uses it.

The rule SHALL be implemented in Java against the Groovy AST, extending `AbstractSpockRule` with a
nested `AbstractAstVisitor`.

#### Scenario: An untyped Mock declaration is reported
- **WHEN** a feature method declares `def repository = Mock(CustomerRepository)`
- **THEN** the rule reports one violation

#### Scenario: An untyped Stub declaration is reported
- **WHEN** a feature method declares `def repository = Stub(CustomerRepository)`
- **THEN** the rule reports one violation

#### Scenario: An untyped Spy declaration is reported
- **WHEN** a feature method declares `def service = Spy(OrderService, constructorArgs: [repository])`
- **THEN** the rule reports one violation

#### Scenario: A typed declaration is not reported
- **WHEN** a feature method declares `CustomerRepository repository = Mock()`
- **THEN** the rule reports no violation

#### Scenario: A typed Spy with constructor arguments is not reported
- **WHEN** a feature method declares `OrderService service = Spy(constructorArgs: [repository])`
- **THEN** the rule reports no violation

### Requirement: Only declarations are reported, not every mock call
The rule SHALL report the *declaration*. A `Mock`, `Stub` or `Spy` call in any other position SHALL
NOT be reported, because there is no variable whose type could have been declared.

```groovy
def service = new CheckoutService(Mock(PaymentGateway))   // not reported
```

This form appears in the conventions this rule enforces, as the way to supply a collaborator that
the specification never refers to again. A rule matching "a `Mock` call with a class-literal
argument" would report it.

#### Scenario: A mock passed inline as a constructor argument is not reported
- **WHEN** a feature method contains `def service = new CheckoutService(Mock(PaymentGateway))`
- **THEN** the rule reports no violation

#### Scenario: A mock passed inline as a method argument is not reported
- **WHEN** a feature method contains `service.register(Mock(Listener))`
- **THEN** the rule reports no violation

#### Scenario: A dynamically typed declaration of something else is not reported
- **WHEN** a feature method declares `def service = new CheckoutService(gateway)`
- **THEN** the rule reports no violation

### Requirement: Fields are reported as well as local variables
The rule SHALL report a dynamically-typed *field* initialised from `Mock`, `Stub` or `Spy`, as well
as a local variable.

A local is a `DeclarationExpression` and a field is a `FieldNode` carrying an initial expression —
different AST nodes reached by different visitor methods. Declaring collaborators as fields is the
more common Spock form, so covering only locals would miss most of what the rule is for.

#### Scenario: An untyped mock field is reported
- **WHEN** a specification declares the field `def repository = Mock(CustomerRepository)`
- **THEN** the rule reports one violation

#### Scenario: A typed mock field is not reported
- **WHEN** a specification declares the field `CustomerRepository repository = Mock()`
- **THEN** the rule reports no violation

#### Scenario: A field declared outside a feature method is still reported
- **WHEN** the untyped mock field is declared at class level, in no method at all
- **THEN** the rule reports one violation

### Requirement: The rule is gated on the class being a specification
The rule SHALL report only within a class matching its `specificationSuperclassNames` property,
defaulting to `*Specification`, or its `specificationClassNames` property, defaulting to unset.

`Mock`, `Stub` and `Spy` are ordinary identifiers. Without the gate, any Groovy class with a method
of one of those names would be reported, and this rule is the one in the family for which that is a
realistic collision rather than a theoretical one.

#### Scenario: A class that is not a specification is not reported
- **WHEN** a Groovy class that does not extend `*Specification` declares `def x = Mock(Foo)`
- **THEN** the rule reports no violation
