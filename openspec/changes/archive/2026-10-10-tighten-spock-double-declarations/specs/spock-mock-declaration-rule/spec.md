## ADDED Requirements

### Requirement: A typed declaration SHALL NOT repeat its type in the factory call
`DeclareMockWithExplicitType` SHALL report a declaration, local or field, whose declared type is
written on the left and whose `Mock`, `Stub` or `Spy` call passes that same type as its type argument.

```groovy
TypeMirror mirror = Stub(TypeMirror)               // reported
TypeMirror mirror = Stub()                         // compliant
```

Spock infers the double's type from the declared type of the variable, so the argument says nothing
the left side does not already say, and the declaration reads the type twice. The convention is that
the type is written once, on the left.

Types are compared by name with generic arguments ignored. A declaration whose factory call names a
*different* type SHALL NOT be reported: `Collection<String> items = Mock(List)` carries information the
declared type cannot, and removing the argument would change what is created.

A `Spy` whose argument is an instance rather than a type, or whose arguments are only named
(`constructorArgs`), SHALL NOT be reported.

#### Scenario: A typed Stub repeating its type is reported
- **WHEN** a feature method declares `TypeMirror mirror = Stub(TypeMirror)`
- **THEN** the rule reports one violation

#### Scenario: A typed Mock repeating its type is reported
- **WHEN** a feature method declares `CustomerRepository repository = Mock(CustomerRepository)`
- **THEN** the rule reports one violation

#### Scenario: A typed Spy repeating its type with constructor arguments is reported
- **WHEN** a feature method declares
  `OrderService service = Spy(OrderService, constructorArgs: [repository])`
- **THEN** the rule reports one violation

#### Scenario: A repeated type with a generic declaration is reported
- **WHEN** a feature method declares `List<String> items = Mock(List)`
- **THEN** the rule reports one violation

#### Scenario: A repeated type on a field is reported
- **WHEN** a specification declares the field `CustomerRepository repository = Mock(CustomerRepository)`
- **THEN** the rule reports one violation

#### Scenario: A different type in the factory call is not reported
- **WHEN** a feature method declares `Collection<String> items = Mock(List)`
- **THEN** the rule reports no violation

#### Scenario: A spy over a real instance is not reported
- **WHEN** a feature method declares `OrderService service = Spy(realService)`
- **THEN** the rule reports no violation

#### Scenario: A declaration with no type argument is not reported
- **WHEN** a feature method declares `TypeMirror mirror = Stub()`
- **THEN** the rule reports no violation

#### Scenario: A declaration is reported once when both conditions could apply
- **WHEN** a feature method declares `def mirror = Stub(TypeMirror)`
- **THEN** the rule reports one violation
