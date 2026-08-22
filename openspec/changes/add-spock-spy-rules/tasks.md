## 0. Prerequisites

- [x] 0.1 `add-spock-fixture-rules` is implemented and archived — `AbstractSpockRule` and the
      specification gate exist
- [x] 0.2 `add-spock-interaction-rules` is implemented and archived — the block partition and the
      interaction classifier exist. All three rules here read that model and none extends it; if one
      needs to, stop and revisit the model rather than duplicating it
- [x] 0.3 Run `openspec validate add-spock-spy-rules` and confirm it resolves

## 1. RequireSpyEntryInteraction

The largest of the three, and the only rule in the family that reads declarations and interaction
ordering together.

- [x] 1.1 Collect spy variable names from declarations whose initialiser is a `Spy` call — locals in
      the feature method and fields on the specification, the same two positions
      `DeclareMockWithExplicitType` covers
- [x] 1.2 Recognise all three declaration shapes: `Spy(Type)`, `Spy(constructorArgs: [...])` and
      `Spy(realInstance)`. Confirm a typed declaration and an untyped one are both recognised, since
      the two rules are independently selectable
- [x] 1.3 For each `then:`/`and:` run, classify interactions by target: specific interactions on a
      spy, the entry interaction `1 * spy._`, and everything else
- [x] 1.4 Confirm `spy._` parses as a `PropertyExpression`, not a method call, and that the entry
      interaction is recognised through that node rather than through the method-call path
- [x] 1.5 Report a run holding one or more specific spy interactions and no entry interaction for
      that spy
- [x] 1.6 Confirm `1 * spy._` does not count as a specific interaction — otherwise the rule is
      satisfied by its own remedy in a block that never needed it
- [x] 1.7 Confirm a run with no spy interaction reports nothing, even with a spy in scope
- [x] 1.8 Implement the ordering check: report an entry interaction declared before any specific
      interaction on the same spy. Confirm an entry interaction after a *non-spy* interaction is
      accepted — position matters only relative to the same spy
- [x] 1.9 Confirm two spies are tracked independently, each requiring its own entry interaction
- [x] 1.10 Confirm a spy arriving from a helper method, a base class or a parameter reports nothing.
      Inferring would demand a line that breaks the specification
- [x] 1.11 Write the `unit`-tagged Spock specification covering every scenario in
      `specs/spock-spy-entry-rule/spec.md`
- [x] 1.12 Add a fixture asserting the required tail — specific interactions, `1 * spy._`, `0 * _` —
      passes both this rule and `RequireStrictMockingTerminator`

## 2. UseVerifyAllForMultipleProperties

- [x] 2.1 Implement the rule reporting a constraint closure whose body asserts more than one property
- [x] 2.2 Cover both shapes: a chained `&&` of two or more operands, and a body of two or more boolean
      statements. Report once per closure, not once per condition
- [x] 2.3 Confirm a single property is exempt, including a single condition using a relational
      operator, and that a disjunction is one condition rather than several
- [x] 2.4 Accept `with`, `verifyAll` and `verifyEach` as the wrapper, matching CodeNarc's own list of
      methods carrying implicit assertions. Confirm a wrapped closure is exempt however many
      conditions it holds
- [x] 2.5 Confirm the rule covers a constraint closure in a stubbed-return interaction, not only the
      cardinality form
- [x] 2.6 Add a fixture proving the rule cannot collide with `RequireValidatedInteractionArguments`:
      `{ true }` is reported by that rule and not by this one. Prove it by running both rather than by
      reasoning about the predicates
- [x] 2.7 Confirm `expect:` blocks are untouched, including a chained conjunction in one
- [x] 2.8 Write the `unit`-tagged Spock specification covering every scenario in
      `specs/spock-verify-all-rule/spec.md`

## 3. AvoidSpyStaticInLabelledBlock

Last, because it is the smallest and the least evidenced.

- [x] 3.1 Implement the rule reporting a `SpyStatic` call under any Spock statement label inside a
      feature method
- [x] 3.2 Match the name as written, with no attempt to resolve it — the same choice
      `AvoidUnrollAnnotation` makes for `@Unroll`
- [x] 3.3 Confirm `SpyStatic` in the unlabelled setup region reports nothing, and that a name merely
      ending in `SpyStatic` is not matched
- [x] 3.4 Confirm a `given:` block containing `SpyStatic` is reported by both this rule and
      `AvoidSetupAndGivenLabels`
- [x] 3.5 Give the rule the lowest priority of the three, since its fixtures are its only corpus
- [x] 3.6 Re-confirm the finding this change rests on: `spock-core:2.4-groovy-5.0` contains no
      `SpyStatic` member. If a newer Spock on the Groovy 5 line has added it, say so in the README
      instead of the absent-API caveat
- [x] 3.7 Write the `unit`-tagged Spock specification covering every scenario in
      `specs/spock-spy-static-rule/spec.md`

## 4. Ship the rules

- [x] 4.1 Add all three rules to `rulesets/groovy/joke.groovy`, referenced by class
- [x] 4.2 Confirm no name collides with a CodeNarc stock rule name
- [x] 4.3 Update `RulesetDistributionSpec`: expected names go from eight to eleven, and the strict
      ruleset's rule count from 120 to 123
- [x] 4.4 Confirm `rulesets/groovy/joke-strict.groovy` needs no edit

## 5. Dogfood

- [x] 5.1 Run `./gradlew codenarcTest` and confirm zero violations. All three are expected to land
      green: no specification here declares a `Spy`, calls `SpyStatic`, or passes a constraint closure
- [x] 5.2 If any violation appears, fix the specification rather than the ruleset
- [x] 5.3 Confirm `pmdMain` still passes over the three rule classes

## 6. Documentation

- [x] 6.1 Add the coverage table mapping each of the eleven rules to the convention it enforces
- [x] 6.2 Name "each method gets its own feature method, protected ones included" as deliberately not
      enforced, with the reason: it needs the subject's API from another file and CodeNarc analyses
      source without a compile classpath
- [x] 6.3 Lead the `RequireSpyEntryInteraction` section with the failure it prevents — `0 * _` failing
      on the very call the feature method exists to make — rather than with the convention. It is the
      only rule whose fix is to add a line, and the one most likely to be excluded on first contact
- [x] 6.4 Record the `SpyStatic` evidence gap: the API is real in the Spock this artifact builds
      against, but no specification here calls it, so the rule's fixtures are its only corpus
- [x] 6.5 Document the ordering requirement in `RequireSpyEntryInteraction` and why it is the only
      position check in the family: the wrong order is silently wrong

## 7. Verify

- [x] 7.1 Confirm mutation analysis reaches 100/100/100 for `codenarc-rules`
- [x] 7.2 Confirm the published jar contains `rulesets/groovy/joke.groovy` resolving eleven rules
- [x] 7.3 Confirm the published POM still declares no dependencies
- [x] 7.4 Run `./gradlew clean check` and confirm it is green. NEVER continue if there are violations
- [x] 7.5 Commit the completed change with `/commit-commands:commit`. The commit must touch
      `codenarc-rules/` or release-please routes it to no package and cuts no release
