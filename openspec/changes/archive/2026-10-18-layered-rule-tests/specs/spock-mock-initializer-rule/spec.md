# Spec Delta

## ADDED Requirements

### Requirement: DeepMock is a mock factory for the initialiser rule
`AvoidMockInitializerClosure` SHALL report a call to `DeepMock` whose last argument is a closure, as it
reports one to `Mock`, and SHALL NOT report `DeepMock()`, `DeepMock(Type)` or a call whose arguments
are named parameters.

The rule SHALL use the same list of factory names as the other rules that recognise doubles, so that
adding a factory changes every rule at once. It SHALL recognise the name as written, because CodeNarc
analyses source without the rewrite `spock-deepmock` applies.

#### Scenario: A DeepMock initialiser closure is reported
- **WHEN** a feature method contains `Repo repository = DeepMock() { findById(_) >> row }`
- **THEN** the rule reports one violation

#### Scenario: A typed DeepMock initialiser closure is reported
- **WHEN** a feature method contains `def repository = DeepMock(Repo) { findById(_) >> row }`
- **THEN** the rule reports one violation from this rule

#### Scenario: A bare DeepMock call is not reported
- **WHEN** a feature method contains `Repo repository = DeepMock()`
- **THEN** the rule reports no violation

#### Scenario: A DeepMock with named options is not reported
- **WHEN** a feature method contains `Repo repository = DeepMock(name: 'repository')`
- **THEN** the rule reports no violation

### Requirement: SpyStatic is not a factory
A call to `SpyStatic` SHALL NOT be reported, even when its last argument is a closure, because it
creates no double.

#### Scenario: A SpyStatic call is not reported
- **WHEN** a feature method contains `SpyStatic(PricingRules)`
- **THEN** the rule reports no violation
