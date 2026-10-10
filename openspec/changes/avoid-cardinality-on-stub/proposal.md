## Why

A Spock `Stub` is not verified, so Spock refuses a required interaction on one. A specification that
writes `1 * stub.foo()` is not wrong in a way the reader can see; it fails when it runs, with
`Stub 'stub' matches the following required interaction … Remove the cardinality, or turn the stub
into a mock`. The convention is that a stub is only ever stubbed — `stub.foo() >> value` — and that a
double whose calls are counted is a `Mock`.

The failure is already guaranteed at runtime, so the rule moves it to analysis time and to the line
that causes it. A spike against Spock 2.4 established exactly which forms throw:

| written, against a default `Stub()` | result |
|---|---|
| `1 * stub.foo()`, `(1..3) * stub.foo()`, `(0..2) * stub.foo()` | throws |
| `0 * stub.foo()` | throws |
| `1 * stub.foo() >> x` | throws |
| `_ * stub.foo()` | passes |
| `stub.foo() >> x` | passes |
| `0 * _` beside a stub | passes |
| `Stub(verified: true)`, `1 * stub.foo()` | passes |

`Spock` decides this with `isRequired()`, which is `minCount != 0 || maxCount != unbounded`, so the
only cardinality a stub accepts is `_ *`. Even `0 *` is a required interaction.

## What Changes

- **`AvoidCardinalityOnStub`** (new) — reports an interaction on a recognised stub that carries a
  cardinality other than `_ *`, whether or not it also carries a `>>` / `>>>` response.

  ```groovy
  1 * stub.foo()                       // reported
  1 * stub.foo() >> value              // reported — the response does not remove the cardinality
  0 * stub.foo()                       // reported — Spock treats it as required too
  (1..3) * stub.foo()                  // reported
  stub.foo() >> value                  // compliant
  _ * stub.foo() >> value              // compliant — not a required interaction
  0 * _                                // compliant — names no stub
  ```

- **Stub recognition** is the existing spy recognition generalised: variables, as fields or as locals
  in the feature method, initialised from a call to one factory. `SpyScope` becomes a scope over
  doubles of a named factory kind, read by the two spy rules for `Spy` and by the new rule for `Stub`.
  The spy rules' behaviour does not change.

- **A stub declared with a `verified` named argument is not a stub for this rule.** Spock lets
  `Stub(verified: true)` take required interactions, so reporting it would be a false positive.

- **`SpockInteraction`** gains a read of the cardinality *under* a `>>` / `>>>`. Today a stubbed
  interaction is read as written and exposes no cardinality, which is right for the spy rule and wrong
  for this one: `1 * stub.foo() >> value` is the form people write.

## Capabilities

### New Capabilities

- `spock-stub-cardinality-rule`: the `AvoidCardinalityOnStub` rule — what is a stub, which
  cardinalities are reported, the `verified:` opt-out, and the exemption of an interaction that names
  no stub.

### Modified Capabilities

- `codenarc-rule-distribution`: the convenience ruleset declares fourteen rules rather than thirteen,
  and the coverage statement counts the new convention.

## Impact

- **Depends on** the existing `AbstractSpockRule`, `AbstractSpockBlockVisitor`, `SpockInteraction`,
  `MockFactories`/`MockCall` and `SpyScope`. The scope helper is renamed and parameterised; both spy
  rules are its regression test.
- **New**: one rule class, one Spock specification, one entry in `rulesets/groovy/joke.groovy`, one
  README section.
- **Changed**: `SpyScope` (generalised), `MockCall` (stub and `verified` reads), `SpockInteraction`
  (cardinality under a response), the two spy rules (use the renamed helper),
  `RulesetDistributionSpec` (thirteen to fourteen rules, strict ruleset 125 to 126), the README
  coverage table (fourteen rules, fifteen conventions, one not enforced).
- **Consumers**: a specification that already fails at runtime on a stub cardinality now also fails
  analysis. No passing specification gains a violation, except one that relies on `Stub(verified:
  true)`, which is exempt.
- **Dogfood**: this repository's own specifications are the corpus; any violation they show is fixed
  in the specification, never by loosening the rule.
- **Not in scope**: `GroovyStub`, which has the same restriction but is not in `MockFactories` and
  would widen two unrelated rules; a stub from a helper method, base class or parameter; a stub
  reassigned after declaration; interactions inside an `interaction { }` closure.
