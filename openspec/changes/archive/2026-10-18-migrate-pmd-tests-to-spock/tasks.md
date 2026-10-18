# Tasks

## 1. Spike the risky assumptions

- [x] 1.1 In a throwaway spec in `codenarc-rules`, add `io.github.joke:spock-deepmock:5.0.0` and write a `DeepMock()` spec with a two-link chain; verify it passes on Groovy 5.0.8 and `spock-core:2.4-groovy-5.0`. If it fails, stop and report before going on.
- [x] 1.2 In a throwaway spec in `pmd-rules` (with `groovy` applied and `byte-buddy`, `mockito-core` on the test classpath), mock a final PMD AST class with `Mock()` and stub `RuleContext#getRule()`; verify both work, or record the fallback in design.md.
- [x] 1.3 Add `codenarc project(':codenarc-rules')` to `pmd-rules` and run `./gradlew :pmd-rules:codenarcTest :codenarc-rules:pmdMain`; verify no circular task dependency is reported.
- [x] 1.4 Delete the throwaway specs and verify `git status` shows only intended build changes.

## 2. Build wiring

- [x] 2.1 Add `io.github.joke:spock-deepmock`, `net.bytebuddy:byte-buddy` and `org.mockito:mockito-core` (already present) constraints to `dependencies/build.gradle`; verify `./gradlew :dependencies:build` passes.
- [x] 2.2 Apply the `groovy` plugin in `pmd-rules/build.gradle` and add Groovy, Spock, DeepMock, `byte-buddy`, `mockito-core` and `junit-platform-launcher` on test configurations only; verify `./gradlew :pmd-rules:generatePomFileForMavenPublication` yields a POM with no dependencies.
- [x] 2.3 Not needed: Add DeepMock and `byte-buddy` to `codenarc-rules`' test classpath; verify `./gradlew :codenarc-rules:test` is still green. Skipped because `codenarc-rules` has no nested mocks (task 5.4), and DeepMock's global `ASTTransformation` breaks 35 CodeNarc rule specs (`AvoidCardinalityOnStubRuleSpec`, `RequireSpyInteractionResponseRuleSpec`, `RequireSpyEntryInteractionRuleSpec`) when added there.

## 3. Helper and first rule

- [x] 3.1 Write the PMD-running helper in `pmd-rules/src/test/groovy` (rule reference, fixture path, optional Java version, returns violations) using only API stable at PMD 7.0.0; verify it with a specification that runs one rule against one fixture and asserts its violation line.
- [x] 3.2 Convert `AvoidAnonymousClasses` (12 cases): one `.java` fixture per case under `src/test/resources/.../fixtures/AvoidAnonymousClasses/`, one `where:` row each, same expected counts and lines as the XML; verify `./gradlew :pmd-rules:test` passes and `:pmd-rules:pitest` reports 100/100/100 for the rule.
- [x] 3.3 Delete `AvoidAnonymousClassesTest.java` and its XML descriptor; verify `./gradlew :pmd-rules:check` does not reference them.

## 4. Remaining rule specifications

- [x] 4.1 Convert `AvoidLambdaBlockBodies` (17 cases, including the `java 1.7` and `java 8` pinned cases); verify tests and Pitest 100/100/100 for the rule.
- [x] 4.2 Convert `UseVarForLocalVariables` (21 cases, including `java 1.8` and `java 10`); verify tests and Pitest 100/100/100 for the rule.
- [x] 4.3 Convert `UseStaticImports` (23 cases, including `java 1.4` and `java 1.5`); verify tests and Pitest 100/100/100 for the rule.
- [x] 4.4 Convert `UseTypeImports` (20 cases); verify tests and Pitest 100/100/100 for the rule.
- [x] 4.5 Convert `AvoidPrivateAndProtectedMethods` (23 cases); verify tests and Pitest 100/100/100 for the rule.
- [x] 4.6 Convert `UseVisibleForTestingAnnotation` (19 cases); verify tests and Pitest 100/100/100 for the rule.
- [x] 4.7 Convert `StaticMethodsModifyStaticState` (46 cases); verify tests and Pitest 100/100/100 for the rule.
- [x] 4.8 Verify the 181 cases are all present: `grep -c "<test-code"` on the deleted descriptors (from git) summed equals the number of `where:` rows across the eight specifications.

## 5. Mockito tests and integration tests

- [x] 5.1 Convert `ImportRulePredicatesTest` to a Spock specification, using `DeepMock()` for any chain of nested AST mocks; verify the specification passes and Pitest stays 100/100/100.
- [x] 5.2 Convert `VisitDelegationTest` to a Spock specification with `Spy` on the rule and `Mock` for the AST nodes and `RuleContext`, using `DeepMock()` where a chain appears; verify the specification passes and Pitest stays 100/100/100.
- [x] 5.3 Convert `RulesetDistributionIT` and `StrictRulesetDistributionIT` to separate `@Tag('integration')` specifications that still avoid `pmd-test`; verify `./gradlew :pmd-rules:integrationTest` passes at the PMD 7.0.0 floor.
- [x] 5.4 Search `codenarc-rules/src/test` for hand-built nested mocks and, if any exist, replace them with `DeepMock()`; verify the affected specifications pass. (An earlier read found none outside fixture strings.)

## 6. Clean-up

- [x] 6.1 Remove `pmd-test`, JUnit Jupiter, direct Mockito and, if unused, AssertJ from `pmd-rules/build.gradle`; verify `git grep -n "pmd-test\|org.junit.jupiter\|org.assertj\|org.mockito" -- pmd-rules` shows only intended hits.
- [x] 6.2 Delete the `xml` fixture directory and any remaining Java test class; verify `find pmd-rules/src/test -name '*.xml' -o -name '*Test.java'` is empty.
- [x] 6.3 Update the `Purpose` paragraph of `openspec/specs/pmd-rule-distribution/spec.md` ("the JUnit and XML-fixture test model") and any README or CONTRIBUTING text describing the test model; verify with `grep -rn "XML fixture\|JUnit" README.md openspec/specs`.
- [x] 6.4 Update `context:` in `openspec/config.yaml` ("pmd-rules uses JUnit 5 with pmd-test, AssertJ, Mockito and XML fixtures"); verify the text matches the new test stack.

## 7. Integration checks

- [x] 7.1 Run `./gradlew check` from the repository root; verify it passes with no CodeNarc, PMD, Error Prone, NullAway, Spotless or Pitest violations. NEVER continue if there are violations.
- [x] 7.2 Commit the completed change with /commit-commands:commit.

## Workflow follow-up

- Archive the change after review, and verify the archived main specs no longer say `pmd-rules` is JUnit-only.
