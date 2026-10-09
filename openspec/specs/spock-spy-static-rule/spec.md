# spock-spy-static-rule Specification

## Purpose

The `AvoidSpyStaticInLabelledBlock` rule: why `SpyStatic` belongs with the unlabelled setup
statements rather than under a Spock label, why the call is matched by name rather than resolved, and
why the rule carries the lowest priority in the artifact.

## Requirements

### Requirement: AvoidSpyStaticInLabelledBlock reports SpyStatic under a Spock label
The artifact SHALL provide a CodeNarc rule named `AvoidSpyStaticInLabelledBlock` that reports a call
to `SpyStatic` appearing under any Spock statement label inside a feature method.

`SpyStatic` enables static mocking for the whole feature method. It belongs with the other unlabelled
setup statements at the top, where the reader looks for what the method arranges. Placed in `then:`
it sits among interaction verifications and reads as one, which it is not — it declares nothing about
what was called.

```groovy
def 'applies the configured regional tax rate'() {
    SpyStatic(PricingRules)                 // compliant — unlabelled setup
    def service = new InvoiceService()

    when:
    def total = service.totalWithTax(100G, 'eu')

    then:
    1 * PricingRules.taxRate('eu') >> 0.20G
    0 * _
}
```

The rule SHALL be implemented in Java against the Groovy AST, extending `AbstractSpockRule` with a
nested `AbstractAstVisitor`, and SHALL read the shared feature-method model.

#### Scenario: SpyStatic in a then block is reported
- **WHEN** a `then:` block contains `SpyStatic(PricingRules)`
- **THEN** the rule reports one violation

#### Scenario: SpyStatic in a when block is reported
- **WHEN** a `when:` block contains `SpyStatic(PricingRules)`
- **THEN** the rule reports one violation

#### Scenario: SpyStatic in a given block is reported
- **WHEN** a `given:` block contains `SpyStatic(PricingRules)`
- **THEN** the rule reports one violation
- **AND** `AvoidSetupAndGivenLabels` also reports the label, because each rule is individually
  selectable and must be correct on its own

#### Scenario: SpyStatic in the unlabelled setup region is not reported
- **WHEN** a feature method opens with `SpyStatic(PricingRules)` before any label
- **THEN** the rule reports no violation

#### Scenario: An unrelated call under a label is not reported
- **WHEN** a `when:` block contains `service.totalWithTax(100G, 'eu')`
- **THEN** the rule reports no violation

### Requirement: The call is matched by name, not resolved
The rule SHALL match `SpyStatic` on the name as written and SHALL NOT attempt to resolve it to a
method.

CodeNarc analyses source without a compile classpath, so name-matching is the only mechanism
available — the same choice `AvoidUnrollAnnotation` makes for `@Unroll`.

`SpyStatic` is real API in the Spock this repository builds against. `spock-core:2.4-groovy-5.0`
declares `spock.mock.MockingApi.SpyStatic(Class)` and an overload taking `IMockMakerSettings`, backed
by `SpecInternals.SpyStaticImpl` and listed as `SPY_STATIC` in Spock's own `Identifiers`. The rule
therefore governs the placement of a call consumers can make, and matching by name is a limitation of
the analysis rather than a hedge against a missing API.

The rule SHALL carry the lowest priority of the rules added alongside it, because it is the only one
with no corpus beyond its own fixtures: no specification in this repository calls `SpyStatic`.

#### Scenario: The name is matched without resolution
- **WHEN** a `then:` block contains `SpyStatic(PricingRules)` and no Spock class is on the classpath
- **THEN** the rule reports one violation

#### Scenario: A name that merely ends in SpyStatic is not matched
- **WHEN** a `then:` block contains `mySpyStatic(PricingRules)`
- **THEN** the rule reports no violation

#### Scenario: The evidence gap is documented
- **WHEN** the README's section for this rule is read
- **THEN** it states that no specification in this artifact's own corpus calls `SpyStatic`, so the
  rule's fixtures are its only evidence

### Requirement: The rule is gated on the class and on the feature method
The rule SHALL report only within a class matching `specificationSuperclassNames` or
`specificationClassNames`, and only inside a method CodeNarc identifies as a Spock feature method.

#### Scenario: A class that is not a specification is not reported
- **WHEN** a Groovy class that does not extend `*Specification` contains a labelled `SpyStatic` call
- **THEN** the rule reports no violation

#### Scenario: A fixture method is not reported
- **WHEN** `def setup()` carrying no statement labels contains `SpyStatic(PricingRules)`
- **THEN** the rule reports no violation
