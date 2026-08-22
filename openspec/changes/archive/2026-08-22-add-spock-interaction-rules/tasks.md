## 0. Prerequisite

- [x] 0.1 `add-spock-fixture-rules` is implemented and archived. This change extends
      `AbstractSpockRule` and assumes the specification gate exists
- [x] 0.2 Run `openspec validate add-spock-interaction-rules` and confirm it resolves

## 1. The feature-method model

Nothing ships from this group alone. Its own specification is the only thing that exercises it until
group 2.

- [x] 1.1 Implement the block partition: one walk over a feature method's top-level statements,
      grouping by `SpockUtil.SPOCK_LABELS`, with `and:` continuing the block it follows
- [x] 1.2 Take the last label where a statement carries several. Verify empirically that an empty
      block stacks labels — `when:` immediately followed by `then:` puts both on one statement — and
      cover it with a fixture
- [x] 1.3 Implement the interaction classifier for the cardinality form (`n * target`), the
      stubbed-return forms (`>>`, `>>>`), the combined form, and `interaction { }`
- [x] 1.4 Confirm `*` binds tighter than `>>`, so `1 * m.f() >> v` nests the cardinality inside the
      stub and never the reverse. A fixture asserts the nesting rather than assuming it
- [x] 1.5 Accept a constant integer, a range and `_` as cardinality; accept a method call, a property
      access and a bare variable as target
- [x] 1.6 Confirm `def total = 2 * price` and `service.warmUp()` are not classified as interactions
- [x] 1.7 Keep the model internal — no shipped ruleset entry, no consumer documentation. Confirm it
      passes `AvoidPrivateAndProtectedMethods`, `AvoidAnonymousClasses`, NullAway and `-Werror`
- [x] 1.8 Confirm the model reaches 100/100/100 under Pitest through the rules' specifications, not
      through a test written against it directly

## 2. InteractionsBelongInThenBlock

- [x] 2.1 Implement the rule reporting an interaction outside a `then:` block or its `and:`
      continuations
- [x] 2.2 Confirm an interaction in the implicit setup region, in `when:`, in `expect:` and in an
      `and:` following `when:` are each reported
- [x] 2.3 Confirm an interaction in `then:` and in an `and:` following `then:` are not reported
- [x] 2.4 Write the `unit`-tagged Spock specification covering every scenario in
      `specs/spock-interaction-placement-rule/spec.md`, including one fixture per interaction form
- [x] 2.5 Add scenarios for the class gate and for a `def setup()` fixture method containing an
      interaction

## 3. RequireValidatedInteractionArguments

- [x] 3.1 Implement the rule reporting `_`, `_ as Type` and `*_` in an interaction argument position
- [x] 3.2 Implement the always-true closure check on the body alone: one statement whose expression is
      a constant truthy under Groovy truth. Confirm `{ true }`, `{ _ -> true }` and `{ it -> true }`
      all reach the same check, and that the parameter list is never read
- [x] 3.3 Confirm a truthy non-boolean constant such as `{ 1 }` is reported
- [x] 3.4 Confirm `{ it }` is **not** reported, and record it in the README as a known gap rather than
      extending the rule into general truthiness analysis
- [x] 3.5 Confirm argument position only: `0 * _`, `1 * service._` and `1 * repo._(customer)` report
      nothing. These are mandated by sibling rules and a report here would be the family fighting
      itself
- [x] 3.6 Confirm the rule covers the cardinality form, both stub forms and the combined form, and
      does not inspect what produced the target
- [x] 3.7 Confirm the rule reports an interaction inside a `Mock() { … }` initialiser, and that
      `AvoidMockInitializerClosure` also reports the declaration. Add a fixture asserting both fire —
      each rule is individually selectable and must be correct alone
- [x] 3.8 Write the `unit`-tagged Spock specification covering every scenario in
      `specs/spock-interaction-argument-rule/spec.md`

## 4. ValueAssertionsBelongInExpectBlock and RequireStrictMockingTerminator

These two land together. Either alone leaves `AvoidUnrollAnnotationRuleSpec` half-migrated, with the
build red for a reason unrelated to the rule being added.

- [x] 4.1 Implement `ValueAssertionsBelongInExpectBlock` using `SpockUtil.isBooleanExpression` as the
      positive test
- [x] 4.2 Confirm no carve-out is needed for interactions or `thrown(...)`: verify empirically that
      `SpockUtil.isBooleanExpression` returns false for `1 * m.f()` (a `BinaryExpression`, for which
      `getVariableAndMethod` yields nothing) and for `thrown(X)` (a method call matching none of the
      boolean-name patterns). If either does match, add the carve-out and record why
- [x] 4.3 Confirm `def error = thrown(X)` is a declaration and never reaches the expression-statement
      test
- [x] 4.4 Confirm `receipt.valid` is not reported and `receipt.isValid()` is, and document the first
      as the known gap
- [x] 4.5 Implement `RequireStrictMockingTerminator` requiring `0 * _` as the last statement of each
      `then:`/`and:` run
- [x] 4.6 Confirm the requirement is unconditional — a feature method declaring no `Mock`, `Stub` or
      `Spy` is still reported — and that a `then:` block containing only `0 * _` is compliant
- [x] 4.7 Confirm one report per `then:`/`and:` run, not one per `and:`, and that a terminator placed
      before a trailing `and:` is reported
- [x] 4.8 Confirm `1 * _` is not accepted as a terminator, and that `when:`, `expect:`, `where:` and
      `cleanup:` are not required to carry one
- [x] 4.9 Confirm a feature method with only a standalone `expect:` block reports nothing
- [x] 4.10 Write both `unit`-tagged Spock specifications, covering every scenario in
      `specs/spock-value-assertion-rule/spec.md` and `specs/spock-strict-mocking-rule/spec.md`

## 5. Dogfood

- [x] 5.1 Run `./gradlew codenarcTest` and record every violation the four new rules report
- [x] 5.2 Rewrite `AvoidUnrollAnnotationRuleSpec`'s `the name and priority are settable` feature: move
      both value assertions to an `expect:` block and leave `then:` carrying only `0 * _`
- [x] 5.3 Fix every other violation in the specification rather than the ruleset. An exclusion here
      would be this repository declining a rule it wrote and still publishes
- [x] 5.4 Confirm the rewritten specification still asserts what it asserted before — that a ruleset
      can set both properties — and that the `spock-unroll-annotation-rule` scenarios still pass
- [x] 5.5 Confirm `pmdMain` still passes over the rule classes and the shared model

## 6. Ship the rules

- [x] 6.1 Add all four rules to `rulesets/groovy/joke.groovy`, referenced by class
- [x] 6.2 Confirm no name collides with a CodeNarc stock rule name
- [x] 6.3 Update `RulesetDistributionSpec`: expected names go from four to eight, and the strict
      ruleset's rule count from 116 to 120
- [x] 6.4 Confirm `rulesets/groovy/joke-strict.groovy` needs no edit

## 7. Documentation

- [x] 7.1 State the adoption cost ahead of the individual rule descriptions: these four report in
      volume on a codebase that has not adopted strict mocking, and each is individually excludable
- [x] 7.2 Add a README section for each of the four rules
- [x] 7.3 Document the unconditional terminator with the mock-free worked example, so a reader meeting
      a `then:` block containing only `0 * _` understands it as the intended shape
- [x] 7.4 Record both known gaps — the bare truthiness check and the `{ it }` closure — with the
      reason each is not closed
- [x] 7.5 Document that the four rules overlap deliberately where they overlap, and why two reports on
      one line is preferred to one rule deferring to another

## 8. Verify

- [x] 8.1 Confirm mutation analysis reaches 100/100/100 for `codenarc-rules`
- [x] 8.2 Confirm the published jar contains `rulesets/groovy/joke.groovy` resolving eight rules
- [x] 8.3 Confirm the published POM still declares no dependencies
- [x] 8.4 Run `./gradlew clean check` and confirm it is green. NEVER continue if there are violations
- [x] 8.5 Commit the completed change with `/commit-commands:commit`. The commit must touch
      `codenarc-rules/` or release-please routes it to no package and cuts no release
