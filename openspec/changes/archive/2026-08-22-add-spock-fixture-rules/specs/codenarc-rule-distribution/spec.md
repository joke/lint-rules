## ADDED Requirements

### Requirement: Rule classes share an abstract base carrying the CodeNarc contract
Every rule class SHALL extend an abstract base, `AbstractSpockRule`, that carries the four read-write
properties the rule contract and the specification gate require: `name`, `priority`,
`specificationSuperclassNames` (defaulting to `*Specification`) and `specificationClassNames`
(defaulting to unset). A concrete rule SHALL declare its own defaults and its visitor class, and
SHALL NOT redeclare those accessors.

`AbstractRule` declares `name` and `priority` abstract because a ruleset configures a rule by setting
them, and the two specification properties are the gate every stock Spock rule exposes. That is eight
accessors a rule class cannot decline to have. Carried per class they would be duplicated once per
rule and each copy would need a mutation-killing test at the repository's 100/100/100 thresholds.

The base SHALL carry `@SuppressWarnings("PMD.DataClass")` once, with a comment recording that the
shape is mandated by CodeNarc's contract. Individual rule classes SHALL NOT carry the suppression.

The base is `public` because CodeNarc instantiates rule classes reflectively and Java visibility
offers no narrower option. It is NOT supported API: the artifact promises its rules and its
rulesets, and makes no compatibility statement about this class.

Concrete rule classes SHALL keep a public no-argument constructor, because CodeNarc's
`RuleSetBuilder` instantiates a rule class by calling `newInstance()`.

#### Scenario: A rule class declares only its defaults and its visitor
- **WHEN** a rule class in `io.github.joke.lint.codenarc.rules.spock` is inspected
- **THEN** it extends `AbstractSpockRule`
- **AND** it declares no `getName`, `setName`, `getPriority` or `setPriority` of its own

#### Scenario: The suppression is stated once
- **WHEN** the rule classes are inspected
- **THEN** `@SuppressWarnings("PMD.DataClass")` appears on `AbstractSpockRule` and on no rule class

#### Scenario: A ruleset can configure every property
- **WHEN** a ruleset sets `name`, `priority`, `specificationSuperclassNames` or
  `specificationClassNames` on any rule
- **THEN** the rule reports them as set

#### Scenario: CodeNarc can instantiate every rule
- **WHEN** `rulesets/groovy/joke.groovy` is loaded
- **THEN** every rule it references is instantiated through a public no-argument constructor

### Requirement: Rules are gated on the class being a Spock specification
Every rule SHALL report only within a class matching `specificationSuperclassNames` or
`specificationClassNames`, using CodeNarc's `SpockUtil.isSpockSpecification`.

The whole artifact is Spock-focused, but its rules match identifiers — `Mock`, `Stub`, `Spy`, and
Spock's block labels — that are legal elsewhere in Groovy. Gating on the class is what keeps a rule
from reporting production code that happens to use one of those names.

#### Scenario: A non-specification class is not analysed
- **WHEN** a Groovy class that does not extend `*Specification` is analysed
- **THEN** no rule this artifact defines reports a violation

#### Scenario: The gate is configurable
- **WHEN** a consumer sets `specificationSuperclassNames` to their own base class
- **THEN** the rules report within classes extending it

### Requirement: The artifact depends on one CodeNarc internal class, deliberately
Rule classes SHALL use no CodeNarc class beyond the documented rule base types and
`org.codenarc.rule.junit.SpockUtil`, whose Javadoc states it is not intended for general use.

`SpockUtil` supplies the specification gate, the feature-method predicate, the Spock label vocabulary
and the boolean-expression heuristic. Reimplementing them would make this artifact's judgement drift
silently from the stock Spock rules a consumer composes it with; depending on `SpockUtil` makes the
same disagreement a loud failure instead.

The exposure SHALL be recorded rather than mitigated away: a CodeNarc release that renames or removes
`SpockUtil` breaks rule classes compiled against the floor inside a *consumer's* analysis, not in this
build. What the dependency update flow buys is that this repository's own build fails first, because
Dependabot raises `org.codenarc:CodeNarc` in `dependencies/build.gradle`. It does not protect a
consumer already running a newer CodeNarc than this artifact's latest release.

#### Scenario: The dependency is confined
- **WHEN** the rule classes are inspected
- **THEN** `SpockUtil` is the only CodeNarc class used beyond `AbstractRule`,
  `AbstractAstVisitorRule` and `AbstractAstVisitor`

#### Scenario: The trade is documented
- **WHEN** the README's CodeNarc support window section is read
- **THEN** it states that the artifact uses one CodeNarc class marked as internal, and what breaks if
  it changes

### Requirement: Rules land in small thematic changes
The remaining Spock rules SHALL land as separate changes grouped by theme, not one rule per change.

This restates the cadence the superseded requirement described as "one rule at a time, matching how
the PMD rules landed". The PMD precedent it cited did not do that: `add-testability-rules` landed
three rules, `add-import-rules` and `add-inline-logic-rules` two each. Rules that share machinery
land together, so the change that builds the machinery also has more than one consumer for it.

#### Scenario: A change ships a coherent group
- **WHEN** a change adding Spock rules is inspected
- **THEN** its rules share a theme or shared machinery
- **AND** each rule has its own capability and its own specification

## MODIFIED Requirements

### Requirement: Shipped rulesets use CodeNarc's Groovy DSL
The artifact SHALL ship exactly two rule resources:

- `rulesets/groovy/joke.groovy` — every rule this artifact defines, each referenced by class.
- `rulesets/groovy/joke-strict.groovy` — the whole analysis this project runs on itself: the stock
  composition together with `ruleset('rulesets/groovy/joke.groovy')`.

Both SHALL be written in CodeNarc's Groovy ruleset DSL rather than its XML form. CodeNarc's
`RuleRegistryInitializer` instantiates only `PropertiesFileRuleRegistry`, whose properties filename
is hardcoded to `codenarc-base-rules.properties`, so bare rule names resolve only for rules
registered inside CodeNarc's own jar. The XML form would therefore require a fully-qualified class
name for each of the stock rules in the composition, where the DSL accepts the bare names.

This artifact's own rules SHALL be referenced by class rather than by bare name, because that same
registry mechanism offers no way for a third-party jar to register a name.

Rule names SHALL NOT collide with a CodeNarc stock rule name. A consumer composes
`rulesets/groovy/joke.groovy` with CodeNarc's own rulesets, and two rules of one name is a failure
whose message names neither.

There is no `category` resource. CodeNarc has no category concept, so the PMD module's three-file
split reduces to two here.

#### Scenario: Both resources are published
- **WHEN** the published jar is inspected
- **THEN** it contains `rulesets/groovy/joke.groovy` and `rulesets/groovy/joke-strict.groovy`

#### Scenario: Own rules are referenced by class
- **WHEN** `rulesets/groovy/joke.groovy` is inspected
- **THEN** each rule this artifact defines is referenced by its fully-qualified class
- **AND** no bare name is used for a rule this artifact defines

#### Scenario: The convenience ruleset selects every rule this artifact defines
- **WHEN** `rulesets/groovy/joke.groovy` is loaded
- **THEN** the resulting rule set contains every rule class this artifact ships

#### Scenario: No rule name collides with a stock rule
- **WHEN** the names this artifact defines are compared against CodeNarc's stock catalogue
- **THEN** no name appears in both

## REMOVED Requirements

### Requirement: The first release ships exactly one rule
**Reason**: The first release has happened. The requirement's standing half — that the module never
ships with no rules, because `failWhenNoMutations` is not relaxed for it — is now guaranteed by there
being four rules and no path back to zero. Its cadence half named a rule-per-change rhythm that the
PMD precedent it cited did not follow.

**Migration**: The cadence is restated as "Rules land in small thematic changes" above. The
`failWhenNoMutations` reasoning is retained there and in `spock-unroll-annotation-rule`'s mutation
scenario; no scenario is lost.
