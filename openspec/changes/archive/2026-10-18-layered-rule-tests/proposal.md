# Proposal

## Why

The test suite does not match its labels. 22 of the 24 specifications are tagged `unit` yet run real
PMD or real CodeNarc over real source: `AvoidAnonymousClassesSpec` never calls `AvoidAnonymousClasses`
directly, and `ValueAssertionsBelongInExpectBlockRuleSpec` feeds Groovy source held in strings to a
real parser. The tag follows Pitest rather than the test, because `includedGroups = ['unit']` is the
only way a test reaches mutation analysis. The result is 181 PMD fixtures that mostly check PMD
itself (suppression, language level, "two violations in one file"), 3,900 lines of CodeNarc specs
whose Groovy-in-Groovy snippets cannot be highlighted, compiled or read as files, and a
`VisitDelegationSpec` that exists to kill mutants rather than to specify behaviour.

Pitest at 100/100/100 is the project's proof of completeness. That job belongs to isolated unit
tests that exercise one class at a time. Integration tests have a different job: showing that a rule
is wired up and fires against the real tool.

## What Changes

- Two layers with distinct purposes:
  - **Unit** specs, tagged `unit`, test one production class with its collaborators isolated by
    `Mock`, `Spy`, `DeepMock` and `SpyStatic`. They are exhaustive: every branch and every decision.
    Pitest selects only these and keeps its 100/100/100 thresholds.
  - **Integration** specs, tagged `integration`, run a rule through the real tool against a fixture
    file and assert that it reports. They are a smoke check, not exhaustive, and are not under Pitest.
- Every rule in both modules gets a unit spec of its own, named for the class (`FooSpec`), and an
  integration spec (`FooIntegrationSpec`) over a small number of fixture files.
- PMD: the 181 per-case fixtures shrink to a few per rule. Cases that exercise rule logic move into
  unit specs. Cases that test PMD itself are dropped.
- CodeNarc: Groovy snippets leave the specifications. Each rule's integration fixture is a `.groovy`
  file under `src/test/resources`, outside every source set, and the unit specs need no source text.
- The package-private `@VisibleForTesting` helpers behind each rule are tested directly, spying the
  rule and stubbing its siblings, with `SpyStatic` for statics of the frameworks we depend on
  (`SpockUtil`, PMD helpers) but not for pure JDK functions.
- Migration is rule by rule. Each step adds the new unit spec, renames and trims the old spec into
  the integration spec and shrinks its fixtures, so the build stays green and Pitest stays at 100%
  throughout.
- **Behaviour change in `codenarc-rules`**: `DeepMock` is recognised as a mock factory alongside
  `Mock`, `Stub` and `Spy`, so `DeepMock()` is held to the same declaration, sharing and initialiser
  rules as `Mock()`. `AvoidMockInitializerClosureRule`'s private copy of the factory list is replaced
  by the shared one. `SpyStatic(Type)` is stated, and tested, to be no double. The `Groovy*` factories
  (`GroovyMock`, `GroovyStub`, `GroovySpy`, `GroovyDeepMock`) stay out of scope.
- **BREAKING** (to the repository's own requirements, not to consumers): "every rule specification is
  tagged `unit`", "each rule is run through PMD against fixture files", "every specified case has a
  fixture" and "samples are embedded in the specification" are reversed.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `build-foundation`: "Both modules test with Spock" now defines the unit and integration layers and
  stops requiring rule specifications to run through the tool; "Spock mocks isolate the unit under
  test" adds `SpyStatic`, the static-stubbing boundary and `DeepMock` for `pmd-rules` only; "Spock
  mocking dependencies are managed by the platform" gives `codenarc-rules` `byte-buddy` and
  `mockito-core` but not `spock-deepmock`; "Mutation testing thresholds" states that Pitest selects
  `unit` only and that integration specs are outside it.
- `pmd-rule-distribution`: "Rule test fixtures are Java files outside the Java source sets" becomes a
  smoke-fixture requirement; "The PMD rules are tested with Spock" names the two layers; "Rule
  fixtures run at a chosen Java language version" applies to integration specs only.
- `codenarc-rule-distribution`: "Rule tests are Spock specifications" replaces embedded snippets with
  fixture files and the unit/integration split.
- `anonymous-class-rule`, `lambda-block-body-rule`, `method-visibility-rule`, `static-import-rule`,
  `static-method-state-rule`, `type-import-rule`, `var-local-variables-rule`,
  `visible-for-testing-rule`: "The rule is covered by unit tests and mutation testing" requires an
  isolated unit case for every specified case, and a smoke fixture, instead of a fixture per case.
- `spock-mock-declaration-rule`, `spock-shared-mock-rule`, `spock-mock-initializer-rule`: `DeepMock`
  is a mock factory, and `SpyStatic` is not.

## Impact

- `pmd-rules/src/test`: all specs rewritten; fixtures under `src/test/resources` reduced; a
  per-module helper that runs one rule against one fixture stays.
- `codenarc-rules/src/test`: all 14 rule specs and the ruleset spec rewritten; fixture files added
  under `src/test/resources`; unit specs added for `MockFactories`, `MockCall`, `DoubleScope`,
  `SpockInteraction`, `AbstractSpockRule` and `AbstractSpockBlockVisitor`.
- `codenarc-rules/src/main`: `MockFactories` gains `DeepMock`; `AvoidMockInitializerClosureRule`
  stops duplicating the factory list. This alters what the published module reports, so it is its own
  `feat` commit and cuts a `codenarc-rules` release. Everything else is `test` and releases nothing.
- Build: `codenarc-rules` gains `byte-buddy` and `mockito-core` on its test classpath, and does not
  gain `spock-deepmock`: its transformation rewrites interaction targets in every Groovy source
  compiled with it, which would distort what the integration layer analyses. The uncommitted
  `testImplementation 'io.github.joke:spock-deepmock'` in `pmd-rules/build.gradle` is part of this
  change. No change to the `test` / `integrationTest` split or to Pitest's `includedGroups`; both
  already match the new meaning of the tags.
- Dogfooding: the new specs are analysed by the strict CodeNarc ruleset, so they obey it. This
  includes the new `DeepMock` handling once it ships.
- Risk accepted: unit specs mock the tools' AST models, so a wrong assumption about PMD or CodeNarc
  is caught only by an integration fixture, and only if a fixture happens to exercise it.
