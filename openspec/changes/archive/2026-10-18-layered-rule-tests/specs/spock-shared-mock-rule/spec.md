# Spec Delta

## ADDED Requirements

### Requirement: DeepMock is a double for the shared-mock rule
`AvoidSharedOrStaticMock` SHALL treat a field initialised from `DeepMock(...)` as it treats one
initialised from `Mock(...)`: when the field is `@Shared` or `static` it is reported.

A deep mock outlives the feature method in the same way, so strict mocking cannot hold for it either.
The rule SHALL recognise the name as written, because CodeNarc analyses source without the rewrite
`spock-deepmock` applies.

#### Scenario: A shared DeepMock field is reported
- **WHEN** a specification declares `@Shared CustomerRepository repository = DeepMock()`
- **THEN** the rule reports one violation

#### Scenario: A static DeepMock field is reported
- **WHEN** a specification declares `static CustomerRepository repository = DeepMock()`
- **THEN** the rule reports one violation

#### Scenario: An instance DeepMock field is not reported
- **WHEN** a specification declares `CustomerRepository repository = DeepMock()`
- **THEN** the rule reports no violation

### Requirement: SpyStatic is not a double
A `@Shared` or `static` field initialised from `SpyStatic(...)` SHALL NOT be reported. The call
arranges static mocking and creates no double, and only the exact factory names are recognised.

#### Scenario: A shared field initialised from SpyStatic is not reported
- **WHEN** a specification declares `@Shared Object rules = SpyStatic(PricingRules)`
- **THEN** the rule reports no violation
