## 0. Prerequisite

- [x] 0.1 Sync `split-into-lint-rules-modules`' specs into `openspec/specs/` with
      `/opsx:sync`, so `codenarc-rule-distribution` and `spock-unroll-annotation-rule` exist as main
      specs and this change's `MODIFIED` deltas resolve. Its two open tasks are release-gated, not
      implementation-gated, so this does not wait on a release
- [x] 0.2 Run `openspec validate add-spock-fixture-rules` and confirm it resolves

## 1. The shared rule base

No behaviour change in this group. `./gradlew check` must be green at the end of it, and
`AvoidUnrollAnnotationRuleSpec` must pass unchanged except for the new gate.

- [x] 1.1 Add `AbstractSpockRule` extending `AbstractAstVisitorRule`, carrying read-write `name`,
      `priority`, `specificationSuperclassNames` (default `*Specification`) and
      `specificationClassNames` (default unset)
- [x] 1.2 Move `@SuppressWarnings("PMD.DataClass")` onto the base with the comment recording that
      CodeNarc's contract mandates the shape. Confirm `rulesets/java/joke-strict.xml` still does not
      exclude `DataClass`
- [x] 1.3 Give the base a constructor taking the rule's default name and priority. Confirm
      `AvoidPrivateAndProtectedMethods` accepts its visibility — constructors are not method
      declarations, and the named-constructor exemption already widened for this shape
- [x] 1.4 Add the specification gate to the base, delegating to `SpockUtil.isSpockSpecification`.
      Implemented by overriding `AbstractAstVisitorRule.shouldApplyThisRuleTo(ClassNode)` rather than
      as a method each visitor calls: that is the hook CodeNarc already consults per class, so the
      gate is stated once and no visitor can forget it. `super` is called first, so `applyToClassNames`
      and `doNotApplyToClassNames` keep working
- [x] 1.5 Retrofit `AvoidUnrollAnnotationRule`: extend the base, delete the four accessors and the
      suppression, keep `getAstVisitorClass()` and a public no-argument constructor
- [x] 1.6 Add a specification scenario for a non-specification class carrying `@Unroll`. The visitor
      needs no gate of its own (see 1.4); the existing samples gained `extends Specification`, which
      the gate now requires
- [x] 1.7 Confirm every accessor on the base is exercised by an existing specification. At 100/100/100
      an unmutated setter fails the build, so the coverage must come from a rule's own spec rather
      than from a test written against the base directly
- [x] 1.8 Run `./gradlew check` — green before continuing

## 2. AvoidSetupAndGivenLabels

- [x] 2.1 Implement the rule reporting a `setup:` or `given:` statement label inside a Spock feature
      method, gated on the class
- [x] 2.2 Use `SpockUtil.isSpockFeatureMethod` for the feature-method test, so a `def setup()` or
      `def setupSpec()` fixture method carrying no labels is not a feature method and is not reported
- [x] 2.3 Handle stacked labels: an empty `given:` block puts both labels on the following statement,
      so `statementLabels` is a list. Report each reportable label once and no statement twice for
      one label
- [x] 2.4 Confirm `when:`, `then:`, `expect:`, `where:`, `cleanup:`, `and:`, `filter:` and `combined:`
      are not reported, and that a non-Spock label such as a loop label is not reported
- [x] 2.5 Write the `unit`-tagged Spock specification covering every scenario in
      `specs/spock-block-label-rule/spec.md`, with violating and compliant samples embedded as Groovy
      strings
- [x] 2.6 Add a scenario asserting the rule is silent on a class that does not extend
      `*Specification`, and one asserting `specificationClassNames` widens the gate

## 3. DeclareMockWithExplicitType

- [x] 3.1 Implement the rule reporting a dynamically-typed declaration whose initialiser is a call to
      `Mock`, `Stub` or `Spy`
- [x] 3.2 Cover local variables through `DeclarationExpression` and fields through the field node
      with an initial expression. Verify empirically that a Spock mock field reaches the visitor —
      a field is not a `DeclarationExpression` and would otherwise be missed
- [x] 3.3 Confirm the predicate is the *declaration*, not the call: `new CheckoutService(Mock(Gateway))`
      and `service.register(Mock(Listener))` report nothing. This form appears in the conventions the
      rule enforces, so a fixture must cover it
- [x] 3.4 Confirm `Type name = Mock()`, `Type name = Spy(constructorArgs: [x])` and
      `def service = new CheckoutService(gateway)` report nothing
- [x] 3.5 Confirm a `Mock()` call reaches the visitor as an implicit-this `MethodCallExpression`, and
      that the rule does not depend on the receiver being absent
- [x] 3.6 Write the `unit`-tagged Spock specification covering every scenario in
      `specs/spock-mock-declaration-rule/spec.md`
- [x] 3.7 Add a scenario asserting the rule is silent on a class that does not extend
      `*Specification`, since `Mock` is an ordinary identifier and this rule is the family's realistic
      collision

## 4. AvoidMockInitializerClosure

- [x] 4.1 Implement the rule reporting a `Mock`, `Stub` or `Spy` call whose last argument is a
      closure, using `SpockUtil.getClosureArgument`
- [x] 4.2 Confirm `Mock()`, `Mock(Type)` and `Spy(constructorArgs: [x])` report nothing — the last is
      a map argument and is the documented way to construct a spy
- [x] 4.3 Confirm a closure argument to an unrelated call, such as `list.each { }`, reports nothing
- [x] 4.4 Confirm the overlap with `DeclareMockWithExplicitType` reports twice for
      `def repository = Mock(Repo) { … }`, once from each rule, and add a fixture asserting it. Each
      rule is independently selectable, so each must be correct alone
- [x] 4.5 Write the `unit`-tagged Spock specification covering every scenario in
      `specs/spock-mock-initializer-rule/spec.md`

## 5. Ship the rules

- [x] 5.1 Add all three rules to `rulesets/groovy/joke.groovy`, referenced by class, keeping the
      grouping style of the existing entry
- [x] 5.2 Confirm no name collides with a CodeNarc stock rule name, by loading the stock catalogue and
      intersecting it with the names this artifact defines
- [x] 5.3 Update `RulesetDistributionSpec`: the convenience ruleset's expected names go from one to
      four, and the strict ruleset's rule count from 113 to 116
- [x] 5.4 Confirm `rulesets/groovy/joke-strict.groovy` needs no edit, since it references
      `rulesets/groovy/joke.groovy` whole

## 6. Dogfood

- [x] 6.1 Run `./gradlew codenarcTest` and confirm zero violations. All three rules are expected to
      land green on this repository: no specification here uses a `setup:` or `given:` label, and none
      declares a mock
- [x] 6.2 If any violation appears, fix the specification rather than the ruleset. An exclusion here
      would be an exclusion of a rule this repository wrote three days earlier. No violation appeared:
      `codenarcTest` reports TotalFiles=5 FilesWithViolations=0
- [x] 6.3 Confirm `pmdMain` still passes over the rule classes, including the new base

## 7. Documentation

- [x] 7.1 Add a README section for each of the three rules under the CodeNarc rules heading, matching
      the format the PMD sections use
- [x] 7.2 Lead the `DeclareMockWithExplicitType` section with the inline-`Mock(Type)` exception, since
      a reader's first guess about the rule is the wrong one
- [x] 7.3 Document that every rule is gated on `specificationSuperclassNames` / `specificationClassNames`,
      and what a consumer with a different specification base class sets
- [x] 7.4 Extend the CodeNarc support window section to state that the artifact uses one CodeNarc class
      marked internal, what breaks if it changes, and that the mitigation is early failure in this
      build rather than protection for a consumer

## 8. Verify

- [x] 8.1 Confirm mutation analysis reaches 100/100/100 for `codenarc-rules`, and that the base's
      accessors are covered through the rules' own specifications
- [x] 8.2 Confirm the published jar contains `rulesets/groovy/joke.groovy` resolving four rules
- [x] 8.3 Confirm the published POM still declares no dependencies
- [x] 8.4 Run `./gradlew clean check` and confirm it is green. NEVER continue if there are violations
- [x] 8.5 Commit the completed change with `/commit-commands:commit`. The commit must touch
      `codenarc-rules/` or release-please routes it to no package and cuts no release
