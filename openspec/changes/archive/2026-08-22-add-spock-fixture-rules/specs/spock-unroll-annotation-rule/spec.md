## MODIFIED Requirements

### Requirement: The rule carries a configurable name and priority
The rule SHALL expose `name` and `priority` as read-write properties, defaulting to
`AvoidUnrollAnnotation` and priority 2, because CodeNarc configures a rule by setting them from the
ruleset that declares it.

Those accessors SHALL be inherited from `AbstractSpockRule` rather than declared on this class. The
`@SuppressWarnings("PMD.DataClass")` that answers PMD for the mandated shape moves with them, so the
suppression is stated once for every rule this artifact ships rather than once per rule. It remains a
suppression rather than an exclusion in `rulesets/java/joke-strict.xml`, because that ruleset is
published to consumers and the collision is internal to this repository.

The rule SHALL additionally inherit `specificationSuperclassNames` and `specificationClassNames` from
the base, and SHALL report only within a matching class. This narrows the rule: `@Unroll` outside a
Spock specification is no longer reported. Nothing else annotates with `Unroll`, so the narrowing
costs nothing and buys one answer to "when does a rule in this artifact apply" instead of two.

#### Scenario: The defaults are the documented ones
- **WHEN** a freshly constructed rule is inspected
- **THEN** its name is `AvoidUnrollAnnotation` and its priority is 2

#### Scenario: A ruleset can override both
- **WHEN** a ruleset sets the rule's name and priority
- **THEN** the rule reports them as set

#### Scenario: The accessors are not declared on the rule class
- **WHEN** `AvoidUnrollAnnotationRule` is inspected
- **THEN** it declares no `getName`, `setName`, `getPriority` or `setPriority`
- **AND** it carries no `@SuppressWarnings("PMD.DataClass")` of its own

#### Scenario: The suppression is local and explained
- **WHEN** `AbstractSpockRule` is inspected
- **THEN** it carries `@SuppressWarnings("PMD.DataClass")` with a comment giving the reason
- **AND** `rulesets/java/joke-strict.xml` does not exclude `DataClass`

#### Scenario: A non-specification class is not reported
- **WHEN** a Groovy class that does not extend `*Specification` carries `@Unroll`
- **THEN** the rule reports no violation
