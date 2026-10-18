# Tasks

Every rule step below keeps the build green and Pitest at 100/100/100: the old specification stays
tagged `unit` until the new unit specification alone carries the rule. Each step ends with its own
commit. A step is not done until `./gradlew :<module>:check` for the rule's module passes with no
violation from PMD, CodeNarc, Error Prone or Spotless.

## 1. Spike the risky assumptions (throwaway, nothing committed)

- [x] 1.1 In `pmd-rules`, write a throwaway unit spec that (a) mocks a final PMD AST class with `Mock()`, (b) stubs the end of a two-link chain that includes a `NodeStream`-returning call with `DeepMock()`, and (c) stubs a PMD static with `SpyStatic`; verify it passes under `:pmd-rules:test` and under `:pmd-rules:pitest`. Stop and report if any of the three fails.
- [x] 1.2 In `codenarc-rules`, temporarily add `byte-buddy` and `mockito-core` to the test classpath, then write a throwaway unit spec that (a) builds a real `TupleExpression` holding a `MapExpression`, (b) mocks `ExpressionStatement` and `ClassNode` with `Mock()`, and (c) stubs `SpockUtil.isBooleanExpression` with `SpyStatic` for a call made from Java; verify it passes under `:codenarc-rules:test` and `:codenarc-rules:pitest`. Stop and report if any of the three fails.
- [x] 1.3 Run the CodeNarc interaction, spy and cardinality rules over a snippet with a chained `DeepMock` target (`1 * person.address.city >> 'x'`), once as plain source and once parsed with `spock-deepmock` on the classpath (the form the library rewrites to); record what each rule reports in `design.md`. Stop and report if a rule misreports plain source.
- [x] 1.4 Delete every throwaway spec and revert the temporary dependency edit; verify `git status` shows no leftover test file.

## 2. Build groundwork

- [x] 2.1 Add `testImplementation 'net.bytebuddy:byte-buddy'` and `testImplementation 'org.mockito:mockito-core'` to `codenarc-rules/build.gradle`, with no version of their own; verify `./gradlew :codenarc-rules:test` is green and `./gradlew :codenarc-rules:generatePomFileForMavenPublication` yields a POM with no dependencies.
- [x] 2.2 Verify `./gradlew :codenarc-rules:dependencies --configuration testRuntimeClasspath` does not list `spock-deepmock`, and that `pmd-rules`' `testRuntimeClasspath` does.
- [x] 2.3 Verify the Pitest and test-split configuration needs no edit: `includedGroups` is `['unit']`, `test` includes `unit`, `integrationTest` includes `integration`, and `check` depends on both.

## 3. PMD pilot: AvoidAnonymousClasses

- [x] 3.1 Rename `AvoidAnonymousClassesSpec` to `AvoidAnonymousClassesIntegrationSpec` (still tagged `unit` for the moment) and write a new `AvoidAnonymousClassesSpec` tagged `unit`, covering `visit`, `reportIfBodyHoldsLogic`, `hasExtractableBody` and `isEnumConstantBody` in isolation with a spy on the rule and a mocked `ASTAnonymousClassDeclaration`; also move this rule's cases out of `VisitDelegationSpec`. Verify `./gradlew :pmd-rules:test` passes.
- [x] 3.2 Retag the integration spec `integration`, then triage the 12 fixtures per design decision 3: keep one violating fixture and the enum-constant fixture (PMD's model of a constant body), delete the rest; verify `./gradlew :pmd-rules:integrationTest` passes and `./gradlew :pmd-rules:pitest` reports 100/100/100 for the rule with the integration spec excluded.
- [x] 3.3 Verify the new specs are clean under `./gradlew :pmd-rules:codenarcTest`; fix the specifications, never the ruleset. Commit as `test(pmd-rules): ...`.

## 4. PMD pilot review checkpoint

- [x] 4.1 Stop and ask the user to review the pilot: spec layout, naming, how a `visit` is isolated, how `SpyStatic` and `DeepMock` read, and the fixture triage. Agree whether the three-case `visit` pattern (reports, reports nothing, returns its data) becomes a shared helper before the other seven rules. Record the agreed pattern in `design.md`.

## 5. PMD rules, one per step

Each step is the same as 3.1 to 3.3, with the rule's own `VisitDelegationSpec` and `ImportRulePredicatesSpec` cases moved into its new `Spec`. Verify each by `./gradlew :pmd-rules:check` and a commit.

- [x] 5.1 `AvoidLambdaBlockBodies` (17 fixtures): verify Pitest 100/100/100 for the rule on its unit spec alone.
- [x] 5.2 `UseVarForLocalVariables` (21 fixtures): keep the language-level fixture pinning a Java version, since only real PMD shows that the ruleset's minimum version is honoured; verify Pitest 100/100/100 for the rule on its unit spec alone.
- [x] 5.3 `UseStaticImports` (23 fixtures): move its `ImportRulePredicatesSpec` cases into `UseStaticImportsSpec`; verify Pitest 100/100/100 for the rule on its unit spec alone.
- [x] 5.4 `UseTypeImports` (20 fixtures): move its `ImportRulePredicatesSpec` cases into `UseTypeImportsSpec`; verify Pitest 100/100/100 for the rule on its unit spec alone.
- [x] 5.5 `AvoidPrivateAndProtectedMethods` (23 fixtures): verify Pitest 100/100/100 for the rule on its unit spec alone.
- [x] 5.6 `UseVisibleForTestingAnnotation` (19 fixtures): verify Pitest 100/100/100 for the rule on its unit spec alone.
- [x] 5.7 `StaticMethodsModifyStaticState` (46 fixtures): verify Pitest 100/100/100 for the rule on its unit spec alone.
- [x] 5.8 Delete `VisitDelegationSpec` and `ImportRulePredicatesSpec` once empty; confirm `RulesetDistributionSpec` and `StrictRulesetDistributionSpec` are tagged `integration`; verify `find pmd-rules/src/test/resources -name '*.java' | wc -l` is far below 181 and `./gradlew :pmd-rules:check` passes.

## 6. CodeNarc helper classes

Each helper gets a unit spec named for it, with real AST nodes where a node is data and `Mock`/`Stub` where it is not. Verify each by `./gradlew :codenarc-rules:check` and a commit.

- [x] 6.1 `MockFactories` and `MockCall`: cover every factory name, a dynamically-named call (null method name), named-argument maps, `repeats` and the qualified/unqualified type comparison, and that `SpyStatic` is not a factory; verify Pitest 100/100/100 for both classes on the unit spec alone.
- [x] 6.2 `DoubleScope`: cover fields, locals, scope reset between feature methods, and every receiver shape; verify Pitest 100/100/100 for the class on the unit spec alone.
- [x] 6.3 `SpockInteraction`: cover every interaction form it recognises and every one it rejects; verify Pitest 100/100/100 for the class on the unit spec alone.
- [x] 6.4 `AbstractSpockBlockVisitor`: cover the label partition, `and:` continuation, stacked labels, a loop label, and a fixture method; stub `SpockUtil` with `SpyStatic`; verify Pitest 100/100/100 for the class on the unit spec alone.
- [x] 6.5 `AbstractSpockRule`: cover the property accessors and the specification gate, including that `super` is consulted first; stub `SpockUtil.isSpockSpecification` with `SpyStatic`; verify Pitest 100/100/100 for the class on the unit spec alone.
- [x] 6.6 Add a CodeNarc counterpart to `PmdRuleRunner` in `codenarc-rules/src/test/groovy` that runs one rule over one `.groovy` fixture from `src/test/resources` and returns violation lines; verify it with the first integration spec in group 7 (7.1).

## 7. CodeNarc rules, one per step

Each step: rename the old spec to `…IntegrationSpec` and retag it `integration` once its replacement carries Pitest; write `…Spec` for the rule class and `…AstVisitorSpec` for its nested visitor, without a source string; move the old snippets that exercise rule logic into unit cases and the rest into at most a few `.groovy` fixture files; verify by `./gradlew :codenarc-rules:check` (Pitest 100/100/100 for the rule on its unit specs alone, `codenarcTest` clean) and a commit.

- [x] 7.1 `AvoidUnrollAnnotationRule`
- [x] 7.2 `AvoidSetupAndGivenLabelsRule`
- [x] 7.3 `ValueAssertionsBelongInExpectBlockRule`
- [x] 7.4 `InteractionsBelongInThenBlockRule`
- [x] 7.5 `RequireStrictMockingTerminatorRule`
- [x] 7.6 `RequireValidatedInteractionArgumentsRule`
- [x] 7.7 `UseVerifyAllForMultiplePropertiesRule`
- [x] 7.8 `AvoidSharedOrStaticMockRule`
- [x] 7.9 `AvoidMockInitializerClosureRule`
- [x] 7.10 `DeclareMockWithExplicitTypeRule`
- [x] 7.11 `AvoidCardinalityOnStubRule`
- [x] 7.12 `AvoidSpyStaticInLabelledBlockRule`
- [x] 7.13 `RequireSpyEntryInteractionRule`
- [x] 7.14 `RequireSpyInteractionResponseRule`
- [x] 7.15 Confirm `RulesetDistributionSpec` is tagged `integration` and initialises the rule registry; verify `grep -rn "'''" codenarc-rules/src/test/groovy` finds no embedded Groovy source.

## 8. DeepMock support

- [x] 8.1 Write failing unit cases first: `MockFactories` recognises `DeepMock`, and rejects `SpyStatic` and `MockRepository`; verify they fail for the right reason.
- [x] 8.2 Add `DeepMock` to `MockFactories`' name set and make `AvoidMockInitializerClosureRule` use `MockFactories` instead of its private `Set.of("Mock", "Stub", "Spy")`; verify the unit cases pass and Pitest stays at 100/100/100 for both classes.
- [x] 8.3 Add `DeepMock` and `SpyStatic` unit cases to the visitor specs of `DeclareMockWithExplicitTypeRule`, `AvoidSharedOrStaticMockRule` and `AvoidMockInitializerClosureRule`, matching the scenarios in the three rule spec deltas; verify `./gradlew :codenarc-rules:pitest` stays at 100/100/100.
- [x] 8.4 Add one `DeepMock` violating line to an existing integration fixture of each of the three rules; verify `./gradlew :codenarc-rules:integrationTest` passes.
- [x] 8.5 Commit as `feat(codenarc-rules): ...`, separate from every `test` commit.

## 9. Final verification

- [x] 9.1 Run `./gradlew check` from the repository root; verify there are no violations from PMD, CodeNarc, Error Prone, Spotless or Pitest, and do not continue if there are.
- [x] 9.2 Verify every `@Tag('unit')` spec starts no PMD or CodeNarc analysis and every spec that does is `@Tag('integration')`: `grep -rn "PmdAnalysis\|SourceString\|CodeNarcRunner" */src/test/groovy` lists only integration specs and the two runners.
- [x] 9.3 Run `openspec validate layered-rule-tests --strict`; verify it passes.
- [x] 9.4 Commit the completed change with /commit-commands:commit.

## Workflow follow-up

- Archive the change after the review is complete, which syncs the deltas into `openspec/specs`.
