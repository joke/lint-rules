## Why

`split-into-lint-rules-modules` shipped one rule to prove the loop. The conventions the module
exists to enforce are written down in the `spock-coding-conventions` skill and enforced by nothing —
CodeNarc's stock catalogue carries three Spock rules and none of them covers house style.

Three of those conventions concern the **fixture region** of a specification: the statements before
the first labelled block, and how collaborators are declared there.

```groovy
// what the conventions ask for            // what fires today
def 'charges the order total'() {          nothing
    given:                                 ← the label Spock does not need
    def gateway = Mock(PaymentGateway)     ← type on the wrong side
    def repo = Mock(Repo) {                ← contract buried in construction
        findById(_) >> customer
    }
```

Going from one rule class to four also turns a shrug into a cost. CodeNarc's `AbstractRule` declares
`name` and `priority` as abstract read-write properties, and gating on "is this a Spock
specification" adds `specificationSuperclassNames` and `specificationClassNames` — eight accessors
per rule class, each needing a mutation-killing test at 100/100/100. At one rule that is 40 lines of
boilerplate. At eight it is 320, and every one of them is the same.

## What Changes

- **`AvoidSetupAndGivenLabels`** — reports a `setup:` or `given:` statement label in a feature
  method. Spock treats unlabelled statements at the top of a feature method as the setup block, so
  the label states what the position already says.
- **`DeclareMockWithExplicitType`** — reports a dynamically-typed declaration whose initialiser is
  `Mock`, `Stub` or `Spy`. `Type name = Mock()` puts the collaborator's type where a reader looks
  for it; `def name = Mock(Type)` hides it in the initialiser and leaves the variable untyped.
- **`AvoidMockInitializerClosure`** — reports `Mock`/`Stub`/`Spy` called with a trailing closure.
  Inline stubbing buries the collaboration contract in construction, away from the `when:` it
  answers, and in practice leans on `_` to get there.
- **`AbstractSpockRule`** — an abstract base carrying the four properties CodeNarc's contract and the
  specification gate require, so a rule class declares its defaults and its visitor and nothing else.
  `AvoidUnrollAnnotationRule` is retrofitted onto it.
- **`SpockUtil` is adopted.** CodeNarc 4.0.0 ships `org.codenarc.rule.junit.SpockUtil` with
  `isSpockSpecification`, `isSpockFeatureMethod`, `SPOCK_LABELS` and `isBooleanExpression`. Its
  Javadoc says it is not intended for general use; we use it anyway, and the trade is recorded.
- `rulesets/groovy/joke.groovy` gains three rules and the distribution specification's expectations
  follow.

Every rule is gated on the class being a Spock specification, through the same
`specificationSuperclassNames = '*Specification'` property CodeNarc's own Spock rules expose.

## Capabilities

### New Capabilities

- `spock-block-label-rule`: the `AvoidSetupAndGivenLabels` rule — which labels it reports, why
  `and:`, `cleanup:` and `where:` are untouched, and why a `setup()` fixture *method* is not a
  feature method.
- `spock-mock-declaration-rule`: the `DeclareMockWithExplicitType` rule — the declaration-shape
  predicate, why it fires on fields as well as locals, and why an inline `Mock(Type)` passed as an
  argument is legal.
- `spock-mock-initializer-rule`: the `AvoidMockInitializerClosure` rule — the trailing-closure
  predicate, and why `Spy(constructorArgs: [...])` is not one.

### Modified Capabilities

- `codenarc-rule-distribution`: rule classes share an abstract base carrying the CodeNarc contract
  properties and the specification gate; the convenience ruleset declares four rules rather than
  one; "the remaining Spock rules land one rule at a time" becomes "in small thematic changes",
  which is what the PMD precedent it cites actually did; the artifact's dependence on CodeNarc
  internal API is stated.
- `spock-unroll-annotation-rule`: the requirement that the rule class carry `name`, `priority` and
  the `@SuppressWarnings("PMD.DataClass")` suppression moves to the shared base. Rule behaviour is
  unchanged.

## Impact

- **Prerequisite**: `split-into-lint-rules-modules` must have its specs synced into `openspec/specs/`
  before this change's `MODIFIED` deltas resolve — `codenarc-rule-distribution` and
  `spock-unroll-annotation-rule` are still `ADDED` deltas in that change. Its two remaining open
  tasks are release-gated (the Central Portal relocation rehearsal), not implementation-gated, so a
  spec sync unblocks this without waiting for a release.
- **New**: three rule classes, one abstract base, four Spock specifications, three entries in
  `rulesets/groovy/joke.groovy`, three README sections.
- **This repository has no violations of these three.** `AvoidUnrollAnnotationRuleSpec` uses no
  `setup:`/`given:` label and declares no mocks; `RulesetDistributionSpec` uses `def setupSpec()`,
  which is a fixture method rather than a labelled block. The dogfood corpus stays green — unlike
  `add-spock-interaction-rules`, which follows and does not.
- **`RulesetDistributionSpec`**: the convenience ruleset's expected names go from one to four, and
  the strict ruleset's rule count from 113 to 116.
- **Consumers**: three new rules in `rulesets/groovy/joke.groovy`, which is referenced whole by
  `joke-strict.groovy`. A consumer on either ruleset picks them up on upgrade. Each is individually
  excludable.
- **CodeNarc internal API**: adopting `SpockUtil` means a CodeNarc release that renames or removes it
  breaks rule classes compiled against 4.0.0 — in a consumer's analysis, not in our build. The
  mitigation is that Dependabot bumps `org.codenarc:CodeNarc` in `dependencies/build.gradle`, so our
  own build breaks first and the artifact can be re-released. It does not protect a consumer already
  running a newer CodeNarc than our latest release.
- **Not in scope**: `SpyStatic` placement, `verifyAll` for multiple properties, and the spy entry
  interaction `1 * subject._` — all deferred. "Each method gets its own feature method, protected
  ones included" is **not implementable**: it needs the subject's API from another file with no
  compile classpath, and stays a review convention.
