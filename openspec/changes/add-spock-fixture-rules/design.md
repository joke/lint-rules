## Context

`codenarc-rules` ships one rule. Seven more are planned, drawn from the `spock-coding-conventions`
skill. This change lands the three that need no model of a feature method's block structure, and
builds the shared rule base that all eight sit on.

The enabling fact for every rule in this family: **CodeNarc parses at `Phases.CONVERSION`**
(`org/codenarc/source/SourceCode.groovy:31`), and Spock's global AST transform runs at
`SEMANTIC_ANALYSIS`. CodeNarc therefore never sees the rewritten specification — it sees what was
typed, with statement labels, cardinalities and `>>` intact:

```
def 'charges the total'() {              MethodNode
    PaymentGateway gateway = Mock()      ├─ ExpressionStatement  labels=[]        implicit setup
    def service = new Checkout(gateway)  ├─ ExpressionStatement  labels=[]
    when:                                ├─ ExpressionStatement  labels=['when']  ← the label rides
    def receipt = service.checkout(o)    │                                          the FIRST statement
    then:                                ├─ ExpressionStatement  labels=['then']
    1 * gateway.charge(49.99G) >> 'txn'  │
    0 * _                                ├─ ExpressionStatement
    expect:                              │
    receipt.transactionId == 'txn'       └─ ExpressionStatement  labels=['expect']
}
```

Nothing in this family needs type resolution, which is what makes it viable at all: CodeNarc
analyses source without a compile classpath.

## Goals / Non-Goals

**Goals**

- Three rules covering the fixture region of a specification.
- One place for the eight accessors CodeNarc's contract and the specification gate demand.
- Adopt CodeNarc's `SpockUtil` deliberately, with the compatibility trade written down.

**Non-Goals**

- Any rule needing the block partition or an interaction classifier. That is
  `add-spock-interaction-rules`, which follows.
- Deciding *when* a `Spy` is warranted, or that a spec tests one method per feature. Both need the
  subject's API, which is in another file and off the classpath.
- Making the rules configurable beyond the two properties CodeNarc's own Spock rules expose.

## Decisions

### 1. `SpockUtil` is adopted, and the trade is stated

CodeNarc 4.0.0 ships `org.codenarc.rule.junit.SpockUtil`, whose Javadoc reads *"This class is not
intended for general use."* It carries exactly what this family needs:

| Need | Source |
|---|---|
| "is this a specification class" | `SpockUtil.isSpockSpecification` |
| "is this a feature method" | `SpockUtil.isSpockFeatureMethod` — "has ≥1 statement label", matching Spock's own `SpecParser` |
| the label vocabulary, with `and:` correctly excluded | `SpockUtil.SPOCK_LABELS` |
| "is this a boolean expression" | `SpockUtil.isBooleanExpression` (used by the next change) |
| **"is this statement an interaction"** | **nothing — ours to write** |

The alternative was reimplementing roughly sixty lines and being immune to CodeNarc's internals.
Rejected: behaviour that diverges from the stock Spock rules is worse than behaviour that breaks
loudly, and the divergence would be silent.

The exposure is real and asymmetric. Rules compiled against the 4.0.0 floor are promised to run on
newer CodeNarc; if `SpockUtil` is renamed, that promise fails inside a *consumer's* analysis, not in
our build. What the mitigation actually buys: Dependabot bumps `org.codenarc:CodeNarc` in
`dependencies/build.gradle`, our own build breaks first, and the artifact is re-released. It does
not help a consumer already running a newer CodeNarc than our latest release. This is the first
place the artifact depends on anything of CodeNarc's beyond the documented rule base classes, and it
is a deliberate exception rather than a new default.

### 2. One abstract base for the CodeNarc contract

`AbstractRule` declares `name` and `priority` as abstract read-write properties because a ruleset
configures a rule by setting them. Gating on "is this a Spock specification" adds
`specificationSuperclassNames` and `specificationClassNames`, the two properties every stock Spock
rule exposes. That is eight accessors per rule class:

```
without a base                          with AbstractSpockRule
─────────────────────────────           ─────────────────────────────
8 rules × 8 accessors = 64              1 base  × 8 accessors = 8
8 × @SuppressWarnings("PMD.DataClass")  1 × @SuppressWarnings("PMD.DataClass")
64 accessors to mutation-test at 100%   8, exercised through any one rule's spec
```

The existing `spock-unroll-annotation-rule` spec predicts the duplication — *"every rule class this
artifact ever ships will carry the same four"* — as an observation rather than a commitment. Going
from one rule to four is the moment to notice, and it is cheaper to move now than at eight.

`AbstractSpockRule` is public because CodeNarc instantiates rule classes reflectively and Java
visibility leaves no alternative. It is not supported API: the artifact promises its rules and its
rulesets, nothing else. Concrete rules keep a public no-arg constructor, since CodeNarc's
`RuleSetBuilder` calls `newInstance()`.

### 3. The declaration rule fires on the declaration, not on `Mock(Type)`

The obvious predicate — "a `Mock` call with a class-literal argument" — is wrong, and the
`spock-coding-conventions` skill demonstrates why in its own exception example:

```groovy
def service = new CheckoutService(Mock(PaymentGateway))   // legal: not a declaration
def service = Spy(OrderService, constructorArgs: [repo])  // violation
OrderService service = Spy(constructorArgs: [repo])       // legal: type on the left
```

The predicate is therefore *a dynamically-typed declaration whose initialiser is a `Mock`, `Stub` or
`Spy` call*. Passing a mock inline to a constructor is untouched — there is no variable to type.

Both locals and fields are covered, and they are different AST nodes. A local is a
`DeclarationExpression`; a field is a `FieldNode` with an initial expression. Spock specifications
commonly declare collaborators as fields — this repository's own `AvoidUnrollAnnotationRuleSpec`
does — so a rule visiting only `visitDeclarationExpression` would miss the more common form.

### 4. Rules are gated on the class, not on the file name

`SpockUtil.isSpockSpecification` matches the superclass name against `specificationSuperclassNames`
(default `'*Specification'`) or the class name against `specificationClassNames` (default null),
mirroring `SpockMissingAssert` and `SpockUseVerifyEach`.

This matters for `DeclareMockWithExplicitType` above all: `Mock`, `Stub` and `Spy` are ordinary
identifiers, and a production Groovy class with a method called `Mock` would otherwise be reported.
The label and initialiser rules are self-gating in practice — nothing outside a specification has
Spock labels — but they carry the same gate so that the whole family answers the question one way.

### 5. Rule names carry no `Spock` prefix

The existing rule is `AvoidUnrollAnnotation`, and every class in this artifact lives in
`io.github.joke.lint.codenarc.rules.spock`. CodeNarc's stock rules prefix (`SpockMissingAssert`)
because they sit in a general catalogue where the qualifier distinguishes; here it would be on every
name and distinguish nothing.

The names are checked against CodeNarc's stock catalogue for collision, because a consumer composes
this ruleset with CodeNarc's own and two rules of one name is a confusing failure.

## Risks / Trade-offs

- **CodeNarc internal API** → Decision 1. Accepted deliberately; the exposure and the limit of the
  mitigation are both stated rather than assumed away.

- **`AbstractSpockRule` retrofits a shipped rule** → `AvoidUnrollAnnotationRule` is already released
  and its spec pins its behaviour. The retrofit moves accessors, not behaviour, and the spec's
  scenarios for the defaults and for ruleset-driven override are unchanged and must still pass.

- **The base class is in the published jar** → a consumer can extend it, and will then be coupled to
  something the artifact makes no promise about. Documented as unsupported; no technical measure
  prevents it, and none is worth taking.

- **`DeclareMockWithExplicitType` on a non-specification** → mitigated by the class gate (Decision 4).
  A specification that extends something other than `*Specification` is not analysed by any rule in
  this family; the property exists so a consumer with a different base class can say so.

- **Three rules, no dogfood evidence** → all three land green on this repository, so their only
  evidence is their own specifications. This is the same position `UseTypeImports` was in and is
  acceptable for the same reason: the samples in the specification are the corpus, and the next
  change supplies real violations.

## Migration Plan

1. Sync `split-into-lint-rules-modules`' specs into `openspec/specs/` so the `MODIFIED` deltas here
   resolve.
2. Add `AbstractSpockRule` and retrofit `AvoidUnrollAnnotationRule`. `./gradlew check` green — this
   step changes no behaviour and its own tests prove it.
3. Add the three rules, each with its specification, one at a time.
4. Register all three in `rulesets/groovy/joke.groovy`, update `RulesetDistributionSpec`, document.

Rollback: every step is an ordinary source change, revertible before release. Nothing here is
published irreversibly.

## Open Questions

- Should the four rules of `add-spock-interaction-rules` also share a partition helper, or does each
  walk the feature method itself? Deferred to that change, where the second consumer of the helper
  exists and its shape can be judged rather than guessed.
