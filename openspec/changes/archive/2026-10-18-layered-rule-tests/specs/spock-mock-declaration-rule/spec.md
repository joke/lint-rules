# Spec Delta

## ADDED Requirements

### Requirement: DeepMock is a mock factory for the declaration rule
`DeclareMockWithExplicitType` SHALL treat a call to `DeepMock` as it treats a call to `Mock`: a
dynamically-typed declaration initialised from `DeepMock(...)` is reported, and a typed declaration
whose `DeepMock` call passes that same type is reported as repeating it.

CodeNarc analyses source without the `DeepMock` rewrite, which only runs when `spock-deepmock` is on
the compiler's classpath, so the rule SHALL recognise the name as written.

#### Scenario: An untyped DeepMock declaration is reported
- **WHEN** a feature method declares `def repository = DeepMock(CustomerRepository)`
- **THEN** the rule reports one violation

#### Scenario: A typed DeepMock declaration is not reported
- **WHEN** a feature method declares `CustomerRepository repository = DeepMock()`
- **THEN** the rule reports no violation

#### Scenario: A typed DeepMock repeating its type is reported
- **WHEN** a feature method declares `CustomerRepository repository = DeepMock(CustomerRepository)`
- **THEN** the rule reports one violation

#### Scenario: A DeepMock passed as an argument is not reported
- **WHEN** a feature method contains `def service = new CheckoutService(DeepMock(PaymentGateway))`
- **THEN** the rule reports no violation

### Requirement: Only the exact factory names are recognised
A call SHALL be recognised as a mock factory only when its whole name is `Mock`, `Stub`, `Spy` or
`DeepMock`. A name that merely contains or begins with one of them, such as `SpyStatic`, SHALL NOT be
recognised, because `SpyStatic(Type)` arranges static mocking for the feature method and creates no
double.

#### Scenario: SpyStatic is not a factory
- **WHEN** a feature method declares `def rules = SpyStatic(PricingRules)`
- **THEN** the rule reports no violation

#### Scenario: A name containing a factory name is not a factory
- **WHEN** a feature method declares `def repository = MockRepository(CustomerRepository)`
- **THEN** the rule reports no violation
