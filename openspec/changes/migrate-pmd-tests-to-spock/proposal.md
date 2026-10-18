# Proposal

## Why

`pmd-rules` is the only module tested in JUnit, and its 181 rule cases live in `pmd-test` XML
descriptors that are hard to read, write and debug. `codenarc-rules` already tests everything with
Spock. Moving `pmd-rules` to Spock makes the repository uniform and lets the tests use Spock's
data tables, interaction mocking and, via `spock-deepmock`, nested mocks instead of hand-built
chains.

The current specs forbid this on purpose: `pmd-rule-distribution` says the PMD module "SHALL NOT
apply the `groovy` plugin" and keeps fixtures in XML so PMD does not flag its own test data. This
change revisits those decisions rather than working around them.

## What Changes

- `pmd-rules` applies the `groovy` plugin and tests with Spock specifications under
  `src/test/groovy`, tagged `unit` or `integration` as in `codenarc-rules`.
- The eight `*Test extends SimpleAggregatorTst` classes and their 181 XML cases become Spock
  specifications with `where:` tables. Each deliberate violation or compliant sample becomes a real
  `.java` file under `src/test/resources`, outside every Java source set, so it is neither compiled
  with `-Werror`/Error Prone/NullAway nor analysed by PMD.
- A small helper runs PMD through the stable API (`PMDConfiguration`, `PmdAnalysis`) against a
  fixture at a chosen Java language version, replacing `pmd-test`'s `RuleTst`.
- `ImportRulePredicatesTest`, `VisitDelegationTest` and the two distribution ITs are rewritten as
  Spock specifications. Mockito `mock`/`spy` become Spock `Mock`/`Spy`, with `byte-buddy` and
  `mockito-core` on the test classpath so Spock can mock the final PMD AST classes.
- `io.github.joke:spock-deepmock` joins the `dependencies` platform and the `pmd-rules` and
  `codenarc-rules` test classpaths. Nested mocks are written with `DeepMock()` where a test would
  otherwise build a chain by hand.
- `pmd-test`, JUnit Jupiter and direct Mockito use are removed from `pmd-rules`' test
  dependencies. AssertJ is removed if nothing still uses it.
- `pmd-rules` gains `codenarc project(':codenarc-rules')` so the convention plugin's CodeNarc
  dogfooding applies to its specifications.
- Pitest stays at 100/100/100 for both modules.
- **BREAKING** (to the repository's own rules, not to consumers): the requirements "Rule test
  fixtures stay in XML" and "The PMD rules are tested with JUnit, not Spock" are reversed.
  Published artifacts are unchanged.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `build-foundation`: "Test stack" no longer bans Spock/Groovy in `pmd-rules`, no longer requires
  `pmd-test` XML fixtures or Mockito there, and adds `spock-deepmock`, `byte-buddy` and
  `mockito-core` (for Spock's final-class mocking) to the platform.
- `pmd-rule-distribution`: "Rule test fixtures stay in XML" becomes a fixture-location requirement
  for `.java` files outside the Java source sets; "tested with JUnit, not Spock" becomes "tested with
  Spock"; the integration tests are Spock specifications that still avoid `pmd-test`; the
  "dependencies do not reach the POM" scenario no longer names `pmd-test`.
- `codenarc-rule-distribution`: "Rule tests are Spock specifications" drops its contrast with the PMD
  module's XML requirement.
- `anonymous-class-rule`, `lambda-block-body-rule`, `method-visibility-rule`,
  `static-import-rule`, `static-method-state-rule`, `type-import-rule`, `var-local-variables-rule`,
  `visible-for-testing-rule`: "The rule is covered by unit tests and mutation testing" says
  Spock specification with fixtures instead of JUnit 5 with `pmd-test`.

## Impact

- `pmd-rules/build.gradle`, `dependencies/build.gradle`, `codenarc-rules/build.gradle`.
- `pmd-rules/src/test`: all of it. Java tests and XML descriptors are replaced by Groovy
  specifications, a helper and fixture files.
- Mutation score is the main risk. Each of the 181 cases must be kept, and the specifications must
  still reach 100/100/100.
- Risks to verify early: Spock mocking `RuleContext#getRule` (package-private), Spock mocking the
  final AST classes, `spock-deepmock` on Groovy 5 (its README claims Groovy 4.0), and no task cycle
  from `pmd-rules` depending on `codenarc-rules` while `codenarc-rules` depends on `pmd-rules`.
- No change to the published artifacts, so a `ci:`/`test:` commit cuts no release.
