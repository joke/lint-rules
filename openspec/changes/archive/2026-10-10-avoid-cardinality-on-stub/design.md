## Context

One new rule over machinery that already exists. `RequireSpyInteractionResponse` already answers "is
this interaction on a recognised spy variable"; this rule asks the same of a stub and then a different
question of the interaction.

```
                      spy rules                          new rule
                 ┌──────────────────────┐         ┌──────────────────────────┐
 declaration ──▶ │ SpyScope (Spy only)  │         │ same recognition,        │
                 │ receiverOf / spyNamed │   ──▶   │ factory kind = Stub      │
                 └──────────────────────┘         └──────────────────────────┘
 interaction ──▶  isCounted && !isNeverCalled        requires a cardinality
                  && !isEntryCall                    that is not `_`, read
                  (no response)                      UNDER a >> / >>>
```

Spock's own rule, read from `MockObject.checkRequiredInteractionAllowed` and confirmed by a spike on
Spock 2.4: an unverified double — `Stub()` by default — throws `InvalidSpecException` when an
interaction targeting it `isRequired()`, and `isRequired()` is `minCount != 0 || maxCount !=
unbounded`. The rule restates that decision statically.

## Goals / Non-Goals

**Goals:**

- Report, on a recognised stub, exactly the interactions Spock would reject.
- Report `1 * stub.foo() >> value`, the form that is actually written.
- Stay silent for an interaction that names no stub, so `0 * _` is never reported.
- Leave the two spy rules' behaviour unchanged.

**Non-Goals:**

- `GroovyStub`. It has the same restriction, but `MockFactories` does not know it, and teaching the
  factory set about it changes `DeclareMockWithExplicitType` and `AvoidSharedOrStaticMock` in the same
  stroke.
- A stub from a helper method, base class or parameter, or a stub reassigned after declaration.
- Interactions inside an `interaction { }` closure — the boundary the spy rules document.
- `Mock(verified: false)`, which Spock also rejects a cardinality on. It is a mock that was made
  unverified on purpose and is not a stub.

## Decisions

### 1. Report every cardinality except `_ *`

A spike over twelve forms showed `0 *`, `(0..2) *` and `(1..3) *` all throw, and only `_ *` passes.
The rule therefore reports a counted interaction on a stub unless its cardinality is the wildcard
`_`. `0 * stub.foo()` is reported although it reads as harmless, because it does not run.

Alternative: report only positive counts. Rejected — it would leave the specification failing at
runtime for `0 *` while the lint passes, which is the gap the rule exists to close.

### 2. The cardinality is read under the response

`SpockInteraction` reads a stubbed interaction "as written", so `1 * stub.foo() >> value` exposes no
cardinality. That is deliberate for the spy rule, where "counted" has to mean "states no response".
The new rule needs the opposite: a response does not excuse the count.

`SpockInteraction` gains `hasCardinality` / `isWildcardCardinality`-style reads that look under a
`>>` or `>>>`. The existing `isCounted`, `isNeverCalled` and `isEntryCall` are untouched, so the spy
rules cannot change behaviour.

Alternative: change `isCounted` to see through the response. Rejected — it would silently alter
`RequireSpyInteractionResponse`, which is built on the current reading.

### 3. Scope recognition is generalised, not duplicated

`SpyScope` is "the variables in scope that were initialised from a call to one factory". Making the
factory a parameter lets the spy rules pass `Spy` and the new rule pass `Stub`. A second class that
copies the field, local and receiver logic would drift from the first.

`MockCall` gains `isStub()` beside `isSpy()`. The class is renamed to say what it now is; the spy
rules change only their reference to it, and their specifications are the regression test.

### 4. A `verified` named argument removes a stub from scope

`Stub(verified: true)` is allowed a required interaction. The recognition therefore skips a `Stub`
call that carries a `verified` named argument, whatever its value. A non-literal value cannot be
decided without resolution, and under-reporting is the family's posture.

`MockCall` already separates named arguments into one leading map, so this is a lookup in a structure
it already reads, not new parsing.

Alternative: skip any stub declared with named arguments. Rejected — `Stub(name: 'users')` is an
ordinary stub and the rule should report it.

### 5. Which interactions are in scope

The same as the spy rule: top-level statements in any block whose `SpockInteraction` is present and
whose receiver is a recognised stub, as a method call, a property access or the bare variable. A bare
`0 * _` has the variable `_`, which is no declared stub, and falls out without a special case.

## Risks / Trade-offs

- [`isRequired()` arithmetic is read from bytecode and one spike] → the spike covers the forms the
  specification lists; a Spock upgrade that changes `isRequired` is caught by the specification the
  next time the corpus is analysed.
- [A stub from a helper, base class or parameter is invisible] → the family's standing posture; the
  rule stays silent rather than demand a change it cannot verify.
- [`verified:` skipped whatever its value, so `Stub(verified: false)` is missed] → `verified: false`
  is the default and nobody writes it; accepted for the sake of not resolving a value.
- [Renaming `SpyScope` touches both spy rules] → their specifications run unchanged and are the check.
- [Reading a cardinality under `>>` adds a branch to `SpockInteraction`] → it is additive and
  reachable only from the new rule, so mutation analysis shows it is exercised.

## Open Questions

- None blocking. `GroovyStub` is the natural follow-up, together with a decision on whether the whole
  family should recognise the `Groovy*` factories.
