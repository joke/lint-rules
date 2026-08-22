## Why

`add-spock-fixture-rules` covered how collaborators are declared. This change covers what the
specification then does with them, which is where the conventions carry their weight and where they
are least self-enforcing.

The thread through all four rules: **a feature method's blocks each hold one kind of thing.**
`when:` acts, `then:` verifies *who was called*, `expect:` asserts *what came back*. When the two
mix, the interaction contract is buried in a wall of `==` and stops being read.

```groovy
then:
1 * repository.save(customer) >> customer   // who was called
receipt.total == 49.99G                     // ← what came back; belongs in expect:
error.message == 'order must not be null'   // ← and this
```

The second half is strictness. An interaction that does not pin down its argument, and a `then:`
that does not close with `0 * _`, both let a wrong call pass silently — which is the exact class of
regression the mocking discipline exists to catch.

## What Changes

- **`InteractionsBelongInThenBlock`** — reports an interaction outside a `then:` block. An
  interaction in the setup region reads as configuration rather than verification, and sits away from
  the `when:` that causes it.
- **`RequireStrictMockingTerminator`** — reports a `then:` block whose last statement is not `0 * _`.
  **Unconditional**: a `then:` with no mock in scope still needs it, because "no interaction happened
  on anything" is an assertion worth making, and a rule that fires only when it can see a mock stops
  firing exactly when a collaborator is introduced by a refactor.
- **`ValueAssertionsBelongInExpectBlock`** — reports a boolean expression in a `then:` block.
  `thrown(...)` and interactions stay; everything else moves to `expect:`.
- **`RequireValidatedInteractionArguments`** — reports an interaction argument that matches
  anything. Covers `_`, `_ as Type`, `*_`, and a closure whose body is a single truthy constant —
  `{ true }`, `{ _ -> true }`, `{ it -> true }` are one check, because the parameter list is
  irrelevant and only the body is examined. Applies to every interaction form on every kind of
  double: mock, stub and spy alike.
- **The interaction classifier and the block partition** — the shared machinery all four sit on.
  `SpockUtil` supplies the specification gate, the feature-method test, the label vocabulary and the
  boolean-expression heuristic, but nothing about `*`, `>>` or `>>>`. That model is this change's to
  build.

## Capabilities

### New Capabilities

- `spock-interaction-placement-rule`: the `InteractionsBelongInThenBlock` rule — what counts as an
  interaction, why `and:` continues a `then:` rather than starting a block, and which blocks are
  reported.
- `spock-strict-mocking-rule`: the `RequireStrictMockingTerminator` rule — the terminator's position,
  why it is required unconditionally, and how a `then:`/`and:` run is terminated once.
- `spock-value-assertion-rule`: the `ValueAssertionsBelongInExpectBlock` rule — the positive
  boolean-expression test, the `thrown(...)` carve-out, and the bare-truthiness gap the positive test
  leaves open.
- `spock-interaction-argument-rule`: the `RequireValidatedInteractionArguments` rule — the four
  rejected forms, why the check is argument-position only, and what it cannot see.

### Modified Capabilities

- `codenarc-rule-distribution`: the convenience ruleset declares eight rules rather than four, and
  the shared interaction model is stated as internal machinery rather than published API.

## Impact

- **Depends on** `add-spock-fixture-rules` for `AbstractSpockRule` and the specification gate.
- **New**: four rule classes, one shared feature-method model, four Spock specifications, four
  entries in `rulesets/groovy/joke.groovy`, four README sections.
- **This repository has violations**, and this is the change that proves the loop rather than
  asserting it. `AvoidUnrollAnnotationRuleSpec.groovy:19` carries two value assertions in a `then:`
  block and no terminator:

  ```groovy
  then:                          →   then:
  rule.name == 'Renamed'             0 * _
  rule.priority == 3
                                     expect:
                                     rule.name == 'Renamed'
                                     rule.priority == 3
  ```

  The rewritten `then:` contains nothing but `0 * _`, which is what the unconditional terminator
  means in a specification with no collaborators. It is the worked example of that decision and the
  README says so.
- **`RulesetDistributionSpec`**: expected names go from four to eight, and the strict ruleset's rule
  count from 116 to 120.
- **Consumers**: these four are considerably more opinionated than the fixture rules and will fire on
  an existing Spock codebase in volume — `RequireStrictMockingTerminator` alone reports once per
  `then:` block that lacks a terminator, which on a codebase that never adopted strict mocking is
  every one of them. The README states this before the rule descriptions, so a first run reads as an
  adoption cost rather than a defect count.
- **Known limitations, recorded rather than fixed**: `ValueAssertionsBelongInExpectBlock` rests on
  CodeNarc's method-name heuristic and does not report a bare truthiness check such as
  `receipt.valid`; `RequireValidatedInteractionArguments` does not report a closure that is truthy
  without being constant, such as `{ it }`. Both under-report rather than guess.
- **Not in scope**: `SpyStatic` placement, `verifyAll` for multiple properties, and the spy entry
  interaction `1 * subject._`. The last is the natural follow-up, since it is the one convention that
  needs the interaction model this change builds.
