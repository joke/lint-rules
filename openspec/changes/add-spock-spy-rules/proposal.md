## Why

`add-spock-fixture-rules` covered how collaborators are declared and `add-spock-interaction-rules`
covered what the blocks hold. Three conventions from the `spock-coding-conventions` skill were
deferred out of both, and this change closes them out. What remains afterwards is one convention that
is not statically checkable at all.

Two of the three concern the `Spy` — the shape used to test a method in isolation from its own
siblings, and the one whose strict-mocking bookkeeping is easiest to get wrong. The third is the
diagnostic quality of a constraint closure.

## What Changes

- **`RequireSpyEntryInteraction`** — reports a `then:` block that declares interactions on a `Spy`
  without also declaring `1 * spy._`. A spy's own methods count as interactions under strict mode, so
  the entry call made by `when:` must be accounted for or `0 * _` fails on the very call the feature
  is about.

  ```groovy
  OrderService service = Spy(constructorArgs: [repository])
  when:
  service.placeOrder(order)
  then:
  1 * service.validate(order)     // sibling, stubbed on the spy
  1 * repository.persist(order)
  1 * service._                   // ← required: accounts for the placeOrder() entry call
  0 * _
  ```

- **`AvoidSpyStaticInLabelledBlock`** — reports `SpyStatic(...)` under any Spock label. It enables
  static mocking for the whole feature method and belongs with the other unlabelled setup statements,
  not in `then:` where its resemblance to an interaction is misleading.

- **`UseVerifyAllForMultipleProperties`** — reports a constraint closure that asserts more than one
  property without `verifyAll`, including the chained-`&&` form. `verifyAll` evaluates every condition
  and reports all failures at once; a chain stops at the first, which is the diagnostic the reader
  needs least. A single property stays a plain closure — the rule does not push `verifyAll` onto one
  condition.

Each rule extends `AbstractSpockRule` and reads the feature-method model built by
`add-spock-interaction-rules`; none adds shared machinery.

## Capabilities

### New Capabilities

- `spock-spy-entry-rule`: the `RequireSpyEntryInteraction` rule — how a spy is recognised, when the
  entry interaction is required, why it must sit after the specific interactions, and what the rule
  cannot see.
- `spock-spy-static-rule`: the `AvoidSpyStaticInLabelledBlock` rule — where `SpyStatic` belongs and
  why the rule matches the call by name.
- `spock-verify-all-rule`: the `UseVerifyAllForMultipleProperties` rule — the two reported shapes,
  the single-property exemption, and why `expect:` blocks are out of scope.

### Modified Capabilities

- `codenarc-rule-distribution`: the convenience ruleset declares eleven rules rather than eight, and
  the artifact's coverage of the `spock-coding-conventions` checklist is stated — including the one
  convention that is deliberately not implemented.

## Impact

- **Depends on** `add-spock-fixture-rules` (for `AbstractSpockRule` and the specification gate) and
  `add-spock-interaction-rules` (for the block partition and the interaction classifier).
- **New**: three rule classes, three Spock specifications, three entries in
  `rulesets/groovy/joke.groovy`, three README sections.
- **`SpyStatic` is not in this repository's Spock.** `spock-core:2.4-groovy-5.0` contains no
  `SpyStatic` member — `SpecInternals` declares `MockImpl`, `SpyImpl`, `GroovyMockImpl` and no static
  variant, and the string appears nowhere in the jar. The convention is documented in the
  `spock-coding-conventions` skill, so the rule encodes house style rather than an API this build can
  exercise. The rule needs no resolution to work — it matches the call by name, exactly as
  `AvoidUnrollAnnotation` matches `@Unroll` — but it ships with no dogfood evidence and its fixtures
  are its only corpus. If the convention is wrong about the API, the rule is inert rather than
  incorrect.
- **`RulesetDistributionSpec`**: expected names go from eight to eleven, and the strict ruleset's rule
  count from 120 to 123.
- **This repository has no violations of any of the three.** No specification here declares a `Spy`,
  calls `SpyStatic`, or passes a constraint closure. All three land green, so each rule's evidence is
  its own fixtures.
- **Consumers**: `RequireSpyEntryInteraction` is the only one of the eleven that reports a *missing*
  statement rather than a present one, so its violations read as "add this line" rather than "remove
  this line". The README says so, because a rule that demands an addition is the one most likely to be
  excluded on first contact.
- **Not in scope, and not implementable**: "each method gets its own feature method, protected ones
  included, and is called directly." It needs the subject's API from another file with no compile
  classpath. This change records it as a review convention so that no later change starts building it.
- **Not in scope, deliberately**: `verifyAll` for several properties of a *returned value* in an
  `expect:` block. The skill calls it the right tool there without making it a rule, and "several
  assertions about one value" cannot be told from "several assertions about several values" without
  knowing what the expressions denote.
