## Why

The family covers *how* a double is declared (`DeclareMockWithExplicitType`) and *what* a spy's
`then:` block must hold (`RequireSpyEntryInteraction`), but three conventions about the double itself
are still enforced by review only:

- A `Mock`, `Stub` or `Spy` must be created per feature method. A `@Shared` or `static` double lives
  outside the scope Spock verifies interactions in, so strict mocking cannot hold for it.
- The collaborator's type is declared once, on the left. `TypeMirror x = Stub(TypeMirror)` states it
  twice; `Spock` already infers it from the variable's declared type.
- A spy interaction states its response. `1 * spy.foo()` on a spy silently calls the *real* method,
  which is rarely what the line reads as.

## What Changes

- **`AvoidSharedOrStaticMock`** (new) — reports a field that is `@Shared` or `static` and is
  initialised from `Mock`, `Stub` or `Spy`.

  ```groovy
  @Shared CustomerRepository repository = Mock()      // reported
  static CustomerRepository repository = Stub()       // reported
  CustomerRepository repository = Mock()              // compliant
  ```

- **`DeclareMockWithExplicitType`** (extended) — additionally reports a *typed* declaration whose
  factory call repeats the declared type as its type argument. Only the same type is reported; a
  differing type is information the left side cannot carry and is left alone.

  ```groovy
  TypeMirror mirror = Stub(TypeMirror)                // reported
  def mirror = Mock(TypeMirror)                       // reported (already)
  TypeMirror mirror = Stub()                          // compliant
  Collection<String> items = Mock(List)               // compliant — types differ
  ```

- **`RequireSpyInteractionResponse`** (new) — reports a counted interaction on a spy that carries no
  `>>` / `>>>` response. `1 * service._` is deliberately exempt: it is the documented way to allow the
  spy's one self-call from `when:`, and `RequireSpyEntryInteraction` requires it.

  ```groovy
  1 * service.validate(order)                         // reported — calls the real method implicitly
  1 * service.validate(order) >> true                 // compliant
  1 * service.validate(order) >> { callRealMethod() } // compliant — call-through said out loud
  1 * service._                                       // compliant — the entry interaction
  1 * service._(order)                                // reported — a filter, not the entry call
  ```

## Capabilities

### New Capabilities

- `spock-shared-mock-rule`: the `AvoidSharedOrStaticMock` rule — which fields are reported, why all of
  `Mock`/`Stub`/`Spy` are, and the `setupSpec` assignment gap that is documented rather than closed.
- `spock-spy-response-rule`: the `RequireSpyInteractionResponse` rule — what counts as an interaction
  on a spy, the exact `spy._` exemption, and the cardinality-zero exemption.

### Modified Capabilities

- `spock-mock-declaration-rule`: a typed declaration repeating its own type in the factory call is
  reported.
- `codenarc-rule-distribution`: the convenience ruleset declares thirteen rules rather than eleven,
  and the coverage statement counts the two new conventions.

## Impact

- **Depends on** the existing `AbstractSpockRule`, `AbstractSpockBlockVisitor` and `SpockInteraction`.
  `RequireSpyEntryInteraction` and the new spy rule both need "which variables are spies", so that
  recognition is extracted from the entry rule into one shared helper; the entry rule's behaviour does
  not change.
- **New**: two rule classes, two Spock specifications, two entries in
  `rulesets/groovy/joke.groovy`, two README sections.
- **Changed**: `DeclareMockWithExplicitTypeRule` and its specification; `SpockInteraction` gains a
  read of whether an interaction carries a response; `RulesetDistributionSpec` counts go from eleven
  to thirteen and the strict ruleset from 123 to 125.
- **Consumers**: `DeclareMockWithExplicitType` begins reporting declarations it was silent on, so an
  upgrade can surface new violations from a rule the consumer already selects. The README says so.
- **Dogfood**: this repository's own specifications are the corpus; any violation they show is fixed
  in the specification, never by loosening the rule.
- **Not in scope**: detecting a double assigned to a `@Shared` field in `setupSpec`; a spy obtained
  from a helper method, base class or parameter; interactions inside an `interaction { }` closure.
