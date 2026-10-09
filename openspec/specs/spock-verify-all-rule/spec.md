# spock-verify-all-rule Specification

## Purpose

The `UseVerifyAllForMultipleProperties` rule: the two constraint-closure shapes it reports, the
single-property and disjunction exemptions, the wrappers that satisfy it, why it cannot collide with
`RequireValidatedInteractionArguments`, and why `expect:` blocks are out of scope.

## Requirements

### Requirement: UseVerifyAllForMultipleProperties reports a constraint closure asserting several properties
The artifact SHALL provide a CodeNarc rule named `UseVerifyAllForMultipleProperties` that reports an
interaction's constraint closure asserting more than one property without `verifyAll`.

```groovy
1 * repository.save({ it.name == 'Ada' && it.region == 'eu' })   // reported
1 * repository.save({ Customer c ->                              // compliant
    verifyAll(c) {
        name == 'Ada'
        region == 'eu'
    }
})
```

`verifyAll` evaluates every condition and reports all failures at once. A chain stops at the first,
so a customer wrong in two fields reports one — and the second failure only appears after the first
is fixed and the test is run again.

The rule SHALL be implemented in Java against the Groovy AST, extending `AbstractSpockRule` with a
nested `AbstractAstVisitor`, and SHALL read the shared feature-method model.

#### Scenario: A chained conjunction is reported
- **WHEN** an interaction argument is `{ it.name == 'Ada' && it.region == 'eu' }`
- **THEN** the rule reports one violation

#### Scenario: A three-way conjunction is reported once
- **WHEN** an interaction argument is `{ it.name == 'Ada' && it.region == 'eu' && it.active }`
- **THEN** the rule reports one violation

#### Scenario: Two separate conditions are reported
- **WHEN** an interaction argument is a closure whose body is two boolean statements
- **THEN** the rule reports one violation

#### Scenario: A closure delegating to verifyAll is not reported
- **WHEN** an interaction argument is a closure whose body is `verifyAll(c) { … }`
- **THEN** the rule reports no violation

#### Scenario: A stubbed-return interaction is covered
- **WHEN** `repository.findById({ it.id == 'x' && it.active }) >> customer` appears in a `then:` block
- **THEN** the rule reports one violation

### Requirement: A single property is exempt
The rule SHALL NOT report a constraint closure asserting exactly one property.

The convention is explicit that the inline boolean closure is enough for one property and that
`verifyAll` should not be reached for. A rule pushing `verifyAll` onto one condition would make the
common case wordier for no diagnostic gain — there is only one failure to report.

#### Scenario: A single-property closure is not reported
- **WHEN** an interaction argument is `{ it.action == 'CHECKOUT' }`
- **THEN** the rule reports no violation

#### Scenario: A single condition using a boolean operator other than && is not reported
- **WHEN** an interaction argument is `{ it.total > 0 }`
- **THEN** the rule reports no violation

#### Scenario: A disjunction is not reported
- **WHEN** an interaction argument is `{ it.region == 'eu' || it.region == 'uk' }`
- **THEN** the rule reports no violation, because a disjunction is one condition about one property
  rather than several assertions that could each fail

### Requirement: with and verifyEach are accepted alongside verifyAll
The rule SHALL accept `with`, `verifyAll` and `verifyEach` as the wrapper that satisfies it, matching
CodeNarc's own list of methods carrying implicit assertions. A closure delegating to any of them
SHALL NOT be reported however many conditions it holds.

#### Scenario: A closure delegating to with is not reported
- **WHEN** an interaction argument is a closure whose body is `with(c) { … }` holding three conditions
- **THEN** the rule reports no violation

#### Scenario: A closure delegating to verifyEach is not reported
- **WHEN** an interaction argument is a closure whose body is `verifyEach(items) { … }`
- **THEN** the rule reports no violation

### Requirement: The rule cannot collide with RequireValidatedInteractionArguments
The rule SHALL NOT report a closure that `RequireValidatedInteractionArguments` reports, and the two
SHALL be mutually exclusive by construction rather than by suppression.

A body of a single truthy constant is one statement asserting nothing, which is what the argument
rule reports; this rule requires more than one property, which that body does not have. No closure
satisfies both predicates.

#### Scenario: An always-true closure is reported by only the argument rule
- **WHEN** an interaction argument is `{ true }`
- **AND** both rules are in the active rule set
- **THEN** `RequireValidatedInteractionArguments` reports one violation
- **AND** `UseVerifyAllForMultipleProperties` reports none

### Requirement: Expect blocks are out of scope
The rule SHALL examine interaction constraint closures only. It SHALL NOT report several assertions
in an `expect:` block.

The convention names `verifyAll` as the right tool for several properties of a returned value without
making it a requirement, and the distinction it rests on is not available to static analysis: several
assertions about one value and assertions about several different values have the same shape unless
the rule knows what the expressions denote. A rule reporting every multi-assertion `expect:` block
would report almost every specification this artifact ships.

#### Scenario: Several assertions in an expect block are not reported
- **WHEN** an `expect:` block contains three boolean expressions
- **THEN** the rule reports no violation

#### Scenario: A chained conjunction in an expect block is not reported
- **WHEN** an `expect:` block contains `receipt.total == 49.99G && receipt.currency == 'EUR'`
- **THEN** the rule reports no violation

### Requirement: The rule is gated on the class and on the feature method
The rule SHALL report only within a class matching `specificationSuperclassNames` or
`specificationClassNames`, and only inside a method CodeNarc identifies as a Spock feature method.

#### Scenario: A class that is not a specification is not reported
- **WHEN** a Groovy class that does not extend `*Specification` contains a multi-condition closure
- **THEN** the rule reports no violation
