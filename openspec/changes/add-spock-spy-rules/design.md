## Context

Three rules, all consumers of machinery that already exists. `AbstractSpockRule` supplies the
CodeNarc contract properties and the specification gate; the feature-method model supplies the block
partition and the interaction classifier. Nothing here needs a new abstraction, which is why these
three were deferrable in the first place.

With them the artifact covers the `spock-coding-conventions` checklist as far as static analysis
reaches:

```
convention                                          rule                              change
──────────────────────────────────────────────────  ────────────────────────────────  ──────
no @Unroll                                          AvoidUnrollAnnotation             split
no setup:/given: label                              AvoidSetupAndGivenLabels          fixture
Type name = Mock(), not def name = Mock(Type)       DeclareMockWithExplicitType       fixture
no interactions in the Mock() initialiser           AvoidMockInitializerClosure       fixture
every interaction in then:                          InteractionsBelongInThenBlock     interaction
every then: ends with 0 * _                         RequireStrictMockingTerminator    interaction
value assertions in expect:, not then:              ValueAssertionsBelongInExpectBlock interaction
no bare _ as an argument                            RequireValidatedInteractionArgs   interaction
spy siblings end with 1 * subject._                 RequireSpyEntryInteraction        THIS
SpyStatic is a setup statement                      AvoidSpyStaticInLabelledBlock     THIS
verifyAll for multiple properties                   UseVerifyAllForMultipleProperties THIS
──────────────────────────────────────────────────  ────────────────────────────────  ──────
one feature method per method under test            NOT IMPLEMENTABLE — needs the subject's API
```

That last row is the reason this change states the boundary rather than leaving it implied. It needs
to know what methods the subject declares, from another file, and CodeNarc analyses source without a
compile classpath.

## Goals / Non-Goals

**Goals:**

- The three deferred conventions, each as an independently selectable rule.
- A written boundary for the artifact: what the checklist asks for, what is enforced, and what is
  deliberately left to review.
- No new shared machinery. If one of these needs it, the model from the previous change was wrong.

**Non-Goals:**

- Deciding whether a `Spy` was the right choice for a subject. The rule takes the spy as given and
  checks only the bookkeeping it implies.
- `verifyAll` in `expect:` blocks.
- Resolving `SpyStatic` to a type, or verifying that the API exists.

## Decisions

### 1. A spy is recognised from its declaration, and nowhere else

`RequireSpyEntryInteraction` collects spy variable names from declarations initialised with `Spy(...)`
— locals in the feature method and fields on the specification, the same two positions
`DeclareMockWithExplicitType` covers. A spy arriving from anywhere else is invisible and the rule
stays silent.

This is the family's standing posture: under-report rather than guess. A spy passed in from a helper
method or a base class cannot be recognised without resolution, and a rule that guessed would demand
`1 * x._` for variables that are not spies at all — a violation whose only fix is to add a line that
breaks the specification.

All three declaration shapes are recognised: `Spy(Type)`, `Spy(constructorArgs: [...])` and
`Spy(realInstance)`.

### 2. The entry interaction is required only when a specific interaction on the spy exists

```groovy
then:
1 * service.validate(order)   ← a specific interaction on the spy
1 * service._                 ← therefore required
0 * _
```

A `then:` block that declares no interaction on the spy needs no entry interaction: nothing in it
constrains the spy, so `0 * _` is not about to fail on the entry call. Requiring `1 * spy._`
unconditionally would put the line into feature methods where it asserts nothing.

`1 * service._` is itself an interaction on the spy, so it must not count as the specific interaction
that triggers the requirement — otherwise the rule would be satisfied by its own remedy in a block
that never needed it.

### 3. The entry interaction must follow the specific ones

Spock matches a declared interaction against a call in declaration order, and `1 * spy._` matches
every method on the spy. Declared first, it absorbs the sibling calls the specific interactions were
meant to verify, and those then fail their own counts.

The rule therefore checks position, not just presence. Combined with
`RequireStrictMockingTerminator`, the required tail of a spy `then:` block is fixed:

```
specific interactions
1 * spy._
0 * _
```

This is the one rule in the family that reports on ordering. It earns the exception because the wrong
order is silently wrong — the specification still runs, and fails somewhere other than where the
mistake is.

### 4. `SpyStatic` is matched by name, and the API is not verified

`spock-core:2.4-groovy-5.0` contains no `SpyStatic`. `SpecInternals` declares `MockImpl`, `SpyImpl`
and `GroovyMockImpl` with no static variant, and the string appears nowhere in the jar.

The rule ships anyway, matching the call by name exactly as `AvoidUnrollAnnotation` matches `@Unroll`
by name — CodeNarc analyses source without a compile classpath, so name-matching is the only option
available to either rule. The consequence is favourable: where the API does not exist the rule is
inert, and where a consumer's Spock provides it the placement is enforced.

What it does mean is that this rule has no dogfood evidence and its fixtures are its only corpus. That
is recorded rather than smoothed over, and it is the reason the rule carries the lowest priority of
the three.

### 5. `UseVerifyAllForMultipleProperties` reports two shapes and exempts one

```groovy
1 * repo.save({ it.name == 'Ada' && it.region == 'eu' })   // reported — chained &&
1 * repo.save({ it.name == 'Ada'; it.region == 'eu' })     // reported — two conditions
1 * repo.save({ verifyAll(it) { name == 'Ada'; region == 'eu' } })  // compliant
1 * auditLog.record({ it.action == 'CHECKOUT' })           // compliant — one property
```

The single-property exemption is explicit in the convention: *"For a single property, the inline
boolean closure is enough; don't reach for `verifyAll`."* A rule that pushed `verifyAll` onto one
condition would make the common case wordier for no diagnostic gain.

`with`, `verifyAll` and `verifyEach` are all accepted as the wrapper, matching CodeNarc's own list of
methods carrying implicit assertions. A closure already delegating to one of them is not reported
regardless of how many conditions it holds.

The `expect:` half of the convention is out of scope. Telling "several properties of one returned
value" from "assertions about several different values" needs to know what the expressions denote,
and a rule that reported every multi-assertion `expect:` block would report almost every
specification in the artifact's own corpus.

### 6. The unimplementable convention is written down

"Each method gets its own feature method, protected ones included, and is called directly" is recorded
in the distribution capability as explicitly not implemented, with the reason.

Left unstated it is an invitation: the checklist has eleven items, ten are rules, and the eleventh
looks like an oversight to whoever reads the list next. Stating it costs a paragraph and closes the
question permanently.

## Risks / Trade-offs

- **`AvoidSpyStaticInLabelledBlock` targets an API this build cannot exercise** → Decision 4. The
  rule is inert where the API is absent, so the failure mode is "reports nothing" rather than "reports
  wrongly". Accepted, recorded, and priority-ordered accordingly.

- **`RequireSpyEntryInteraction` is the family's only rule demanding an addition** → every other rule
  reports something present that should be removed or moved. A rule whose fix is "write another line"
  meets more resistance on first adoption, and a consumer who does not understand *why* will exclude
  it. Mitigated by the README leading that rule's section with the failure it prevents — `0 * _`
  failing on the very call the feature method is about.

- **Position checking is new to the family** → Decision 3. It is confined to one rule and one
  ordering, and the ordering is not stylistic: the wrong order changes what the specification
  asserts. No other rule gains a position check on the strength of this one.

- **`UseVerifyAllForMultipleProperties` overlaps `RequireValidatedInteractionArguments`** → both
  examine constraint closures. They cannot both fire on one closure: a body of a single truthy
  constant is one statement, so the multiple-properties rule is silent on exactly what the argument
  rule reports. Verified by fixture rather than by argument.

- **Three rules, no dogfood evidence** → no specification in this repository declares a `Spy`, calls
  `SpyStatic`, or passes a constraint closure. The same position `add-spock-fixture-rules` was in, and
  acceptable for the same reason: the samples in each specification are the corpus. Unlike that change,
  there is no follow-up queued to supply real violations, so this is the state these three stay in
  until the repository's own specifications grow a spy.

## Migration Plan

1. `RequireSpyEntryInteraction` — the largest of the three, and the only one reading spy declarations
   and interaction ordering together.
2. `UseVerifyAllForMultipleProperties`, including the fixture proving it cannot collide with
   `RequireValidatedInteractionArguments`.
3. `AvoidSpyStaticInLabelledBlock` — last, because it is the smallest and the least evidenced.
4. Register all three, update the distribution capability with the coverage statement and the
   unimplementable convention, document.

Rollback: ordinary source changes, revertible before release.

## Open Questions

- Should `RequireSpyEntryInteraction` also report `1 * spy._` in a block with no specific spy
  interaction — a line that asserts nothing and suggests the author misunderstood the convention? It
  is the inverse of the rule as specified and would need its own requirement; deferred until it is
  seen in real code rather than imagined.
