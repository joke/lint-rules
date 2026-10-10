## ADDED Requirements

### Requirement: AvoidSharedOrStaticMock reports a shared or static double
The artifact SHALL provide a CodeNarc rule named `AvoidSharedOrStaticMock` that reports a field that is
annotated `@Shared` or declared `static` and is initialised from a call to `Mock`, `Stub` or `Spy`.

```groovy
@Shared CustomerRepository repository = Mock()     // reported
static CustomerRepository repository = Stub()      // reported
CustomerRepository repository = Mock()             // compliant
```

A double that outlives the feature method is outside the scope in which Spock verifies interactions,
so strict mocking cannot hold for it. The convention is that every double is created per feature
method, where its interactions are verified.

The rule SHALL be implemented in Java against the Groovy AST, extending `AbstractSpockRule` with a
nested `AbstractAstVisitor`.

#### Scenario: A shared Mock field is reported
- **WHEN** a specification declares `@Shared CustomerRepository repository = Mock()`
- **THEN** the rule reports one violation

#### Scenario: A shared Stub field is reported
- **WHEN** a specification declares `@Shared CustomerRepository repository = Stub()`
- **THEN** the rule reports one violation

#### Scenario: A shared Spy field is reported
- **WHEN** a specification declares `@Shared OrderService service = Spy(constructorArgs: [repository])`
- **THEN** the rule reports one violation

#### Scenario: A static Mock field is reported
- **WHEN** a specification declares `static CustomerRepository repository = Mock()`
- **THEN** the rule reports one violation

#### Scenario: A static final field is reported
- **WHEN** a specification declares `static final CustomerRepository repository = Stub()`
- **THEN** the rule reports one violation

#### Scenario: An instance field is not reported
- **WHEN** a specification declares `CustomerRepository repository = Mock()`
- **THEN** the rule reports no violation

#### Scenario: One declaration is reported once
- **WHEN** a specification declares the non-private field `@Shared CustomerRepository repository = Mock()`
- **THEN** the rule reports exactly one violation, not one per property and field

### Requirement: Only a double initialiser is reported
The rule SHALL NOT report a `@Shared` or `static` field whose initialiser is not a `Mock`, `Stub` or
`Spy` call, and SHALL NOT report a double assigned to such a field outside its declaration.

The assignment form — `@Shared Foo x` followed by `x = Mock()` in `setupSpec` — has no initialiser to
inspect. Catching it needs a write-tracking pass over the specification, and the family's posture is to
under-report rather than infer. The gap is documented in the README.

#### Scenario: A shared field of something else is not reported
- **WHEN** a specification declares `@Shared Clock clock = Clock.systemUTC()`
- **THEN** the rule reports no violation

#### Scenario: A shared field assigned a double in setupSpec is not reported
- **WHEN** a specification declares `@Shared Foo foo` and assigns `foo = Mock()` in `setupSpec`
- **THEN** the rule reports no violation

### Requirement: The rule is gated on the class being a specification
The rule SHALL report only within a class matching `specificationSuperclassNames`, defaulting to
`*Specification`, or `specificationClassNames`, defaulting to unset.

#### Scenario: A class that is not a specification is not reported
- **WHEN** a Groovy class that does not extend `*Specification` declares `static Foo foo = Mock()`
- **THEN** the rule reports no violation
