## Context

Four rules, all reading the same thing: a feature method partitioned into labelled blocks, with each
top-level statement classified as an interaction, an exception capture, or a value assertion.

CodeNarc parses at `Phases.CONVERSION`, before Spock's global AST transform runs at
`SEMANTIC_ANALYSIS`, so all of it is visible as written. `add-spock-fixture-rules` records that fact
and the `SpockUtil` adoption it rests on; this change consumes both.

What `SpockUtil` does **not** carry is any notion of a Spock interaction. Nothing in CodeNarc models
`*`, `>>` or `>>>`. That model is the substance of this change, and three of the four rules are thin
once it exists.

## Goals / Non-Goals

**Goals**

- One model of a feature method — blocks, and the classification of the statements in them — shared
  by four rules.
- Rules that under-report rather than guess, with every gap written down.
- Each rule independently correct, because each is independently selectable from the ruleset.

**Non-Goals**

- Knowing whether a variable is a mock, a stub, a spy or a real object. No rule here needs it: an
  interaction is recognised by its shape, not by its target's provenance.
- Deciding whether a `Spy` was the right choice, or whether a spec tests one method per feature.
  Both need the subject's API from another file, off the classpath.
- Reporting a value assertion that CodeNarc's heuristic cannot recognise as boolean.

## Decisions

### 1. The interaction model

An **interaction** is a top-level statement in a feature method whose expression is one of:

```
1 * mock.foo(x)          BinaryExpression('*',  cardinality, target)
mock.foo(x) >> value     BinaryExpression('>>', target, value)
mock.foo(x) >>> [a, b]   BinaryExpression('>>>', target, values)
1 * mock.foo(x) >> value BinaryExpression('>>', BinaryExpression('*', …), value)
interaction { … }        MethodCallExpression named 'interaction' with a closure
```

`*` binds tighter than `>>`, so the combined form nests the cardinality inside the stub, never the
other way round. **Cardinality** is a constant integer, a range (`(1..3) * …`), or `_`
(`_ * mock.foo()`). **Target** is a `MethodCallExpression` (`mock.foo()`), a `PropertyExpression`
(`mock._`), or a `VariableExpression` (`_`).

`>>` is also Groovy's right shift and its closure composition operator. Inside a feature method, at
statement level, in a `then:` block, the ambiguity is not worth modelling — a specification that
right-shifts an integer as a bare statement has a larger problem.

### 2. The block partition, and why `and:` is not a block

Groovy attaches a statement label to the *first* statement following it, so a labelled block is a run
of statements starting at a labelled one and ending before the next. Partitioning is a single walk
over the feature method's top-level statements.

`SpockUtil.SPOCK_LABELS` deliberately omits `and:` — *"it doesn't have any semantic impact"*. That
omission is exactly right here: an `and:` after `then:` continues the same verification block, so
the current label stays `then` across it. `RequireStrictMockingTerminator` consequently requires one
terminator at the end of the whole `then:`/`and:` run, not one per `and:`.

A statement can carry several labels when a block is empty (`when:` immediately followed by `then:`).
Verified against the AST rather than assumed: Groovy stacks them **innermost first**, so `given: when:
then: stmt` arrives as `[then, when, given]` and the label nearest the statement in the source is the
*first* of the list. That first label is the one taken, so an empty `when:` does not make the
following statements part of it.

CodeNarc's own `SpockMissingAssertRule` takes `labels.intersect(SPOCK_LABELS).last()`, which on that
same list yields `when` — the outermost. This change does not copy it: the intent stated here and the
mechanism CodeNarc uses disagree once a block is empty, and the intent is what a reader of a
specification means.

### 3. These are block-structure rules, not expression rules

CodeNarc's stock Spock rules are written as expression visitors carrying a `currentLabel` field
updated as statements go by. That style works for a rule that judges statements one at a time.

Three of these four judge a *block*: "does this `then:` end with a terminator", "does this `then:`
contain a value assertion", "is this interaction in the right block". They are implemented as one
pass over the feature method's top-level statements in the method visit, not as per-expression
callbacks with a mutable label field. The partition is computed once and the rules read it.

The model is internal. It is not exported, not documented for consumers, and carries no
compatibility promise — the artifact promises rules and rulesets.

### 4. The terminator is required unconditionally

A `then:` block with no mock in scope still requires `0 * _`.

The alternative — require it only when the feature method declares a `Mock`, `Stub` or `Spy` — was
rejected on the failing case rather than the common one. A specification with no collaborators today
acquires one when the subject grows a dependency, and a conditional rule goes quiet at precisely that
moment: the `then:` block that was fine yesterday is now unterminated and nothing says so. The
assertion "no interaction happened on anything" is also not vacuous when there are no mocks — it is
cheap and it stays true by construction, which is what makes it safe to leave in place.

The visible cost is a `then:` containing nothing else:

```groovy
when:
rule.name = 'Renamed'

then:
0 * _

expect:
rule.name == 'Renamed'
```

This repository's own `AvoidUnrollAnnotationRuleSpec` becomes exactly that, and it is documented as
the worked example rather than smoothed over.

### 5. The value-assertion rule tests positively

Two definitions were available for "a value assertion in a `then:` block":

| | positive: `SpockUtil.isBooleanExpression` | negative: not an interaction and not `thrown` |
|---|---|---|
| `result == 5` | reports | reports |
| `service.helper()` | silent | **reports** — a false positive |
| `receipt.valid` | silent — a miss | reports |
| rests on | a method-name heuristic | a complete interaction model |

The positive test is chosen. It shares CodeNarc's own judgement about what a boolean expression is,
so this rule and `SpockMissingAssert` agree by construction rather than by coincidence — and a
consumer who runs both never sees them disagree about the same line.

The two forms verified as *not* matching the heuristic, so neither needs a carve-out in code:
`1 * mock.foo()` is a `BinaryExpression` and `SpockUtil.getVariableAndMethod` returns nothing for it;
`thrown(IllegalArgumentException)` is a method call whose name matches none of the boolean patterns.
`def error = thrown(X)` is a declaration and is not an expression statement at all.

The gap is real: `receipt.valid` in a `then:` block is not reported. Recorded as a limitation, not
worked around. Widening later is a change of definition with its own evidence.

### 6. Argument position only

`RequireValidatedInteractionArguments` examines the *arguments* of an interaction's target call and
nothing else. `_` in target position is not merely tolerated — it is mandated by
`RequireStrictMockingTerminator` (`0 * _`) and by the spy convention (`1 * subject._`) that follows
this change. A rule reading `_` wherever it appears would fight its own family.

The four rejected forms:

```
1 * repo.save(_)                    bare wildcard
1 * repo.save(_ as Customer)        typed wildcard — narrower, still matches every Customer
1 * repo.save(*_)                   spread wildcard — any number of any arguments
1 * repo.save({ true })             constraint closure that constrains nothing
1 * repo.save({ _ -> true })          ↑ same body
1 * repo.save({ it -> true })         ↑ same body
```

The last three are one check. A closure's parameter list is irrelevant to whether its body
discriminates, so the rule examines the body alone: a single statement whose expression is a constant
truthy under Groovy truth — `true`, a non-zero number, a non-empty string.

`_ as Type` is included after consideration. It carries real information, which is why it is the
closest call in the set, but it still accepts every instance of that type, and the rule's subject is
whether the *argument* was the right one. A consumer who disagrees excludes one rule.

`{ it }` — a closure returning its own parameter, truthy for anything non-null and non-empty —
is **not** covered. It is not a constant, and detecting it would begin a general truthiness analysis
this rule declines to start. Recorded as a gap.

### 7. Rules report independently, including where they overlap

`Mock() { findById(_) >> row }` violates `AvoidMockInitializerClosure` from the previous change and
`RequireValidatedInteractionArguments` from this one. Both fire.

Suppressing the second because the first already condemned the line was considered and rejected:
every rule is individually selectable from `rulesets/groovy/joke.groovy`, so a consumer may have
adopted only one. Deferring would make that consumer's ruleset silently miss a violation it selected
a rule to catch. Two reports on one line is the lesser cost, and the fix removes both at once.

## Risks / Trade-offs

- **These rules fire in volume on an existing codebase** → `RequireStrictMockingTerminator` reports
  once per unterminated `then:`, which on a codebase that never adopted strict mocking is all of them.
  Unlike the fixture rules, adoption here is a project decision rather than a tidy-up. Mitigated by
  saying so in the README ahead of the rule descriptions, and by every rule being individually
  excludable.

- **`ValueAssertionsBelongInExpectBlock` is the highest-false-positive rule in the artifact** →
  reduced, not eliminated, by the positive test (Decision 5). A helper call in a `then:` block is not
  reported; a boolean-returning helper named `isValid()` is. That is the heuristic's error, shared
  with `SpockMissingAssert`, and a suppression is the escape.

- **The interaction model is guesswork about Spock's grammar** → mitigated by the corpus: every form
  in Decision 1 gets a fixture, and the model is exercised by four rules rather than one, so a gap
  shows up as a rule that under-reports rather than as silence.

- **The unconditional terminator will read as noise to a reader who meets it first in a mock-free
  spec** → accepted, with the worked example documented (Decision 4). The alternative fails silently
  at the moment a collaborator appears, which is worse and less visible.

- **Overlapping reports** → Decision 7. Accepted deliberately; independence beats tidiness when the
  rules are selectable one at a time.

## Migration Plan

1. Build the feature-method model — partition and classifier — with its own specification. Nothing
   ships from it alone.
2. `InteractionsBelongInThenBlock`, then `RequireValidatedInteractionArguments`. Both are silent on
   this repository, so their evidence is their fixtures.
3. `ValueAssertionsBelongInExpectBlock` and `RequireStrictMockingTerminator`, then rewrite
   `AvoidUnrollAnnotationRuleSpec` to comply. Doing the two together avoids a state where the spec is
   half-migrated and the build is red for a reason unrelated to the rule being added.
4. Register all four, update the distribution specification, document the adoption cost.

Rollback: ordinary source changes, revertible before release.

## Open Questions

- Should the spy entry interaction (`1 * subject._` where the subject is a `Spy`) land as a follow-up
  using this change's model? It is the last convention in the skill that the model makes reachable,
  and the only reason it is not here is that four rules is already the batch size the PMD precedent
  set.
