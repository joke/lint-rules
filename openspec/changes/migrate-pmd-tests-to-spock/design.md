# Design

## Context

See proposal.md for motivation. Current state:

- `pmd-rules` tests are JUnit 5. Eight `*Test extends SimpleAggregatorTst` classes drive 181 cases
  from XML descriptors; `ImportRulePredicatesTest` and `VisitDelegationTest` use Mockito; two ITs call
  PMD's API directly (deliberately without `pmd-test`).
- `codenarc-rules` already tests in Spock under the same `conventions` plugin: `@Tag('unit')` /
  `@Tag('integration')`, `test` / `integrationTest` split, Pitest at 100/100/100 through the JUnit
  Platform.
- Applying `groovy` to a module makes `conventions` run CodeNarc on it with `joke-strict`, which
  comes from `codenarc-rules`. `pmd-rules` does not have that on its `codenarc` configuration yet.
- `integrationTest` resolves PMD at the 7.0.0 floor, so any helper must stick to PMD's stable API.
- Six XML cases pin a Java language version (`java 1.4`, `1.5`, `1.7`, `8`, `1.8`, `10`).
- `spock-deepmock` 5.0.0 is on Maven Central. It is built against `spock-core:2.4-groovy-4.0` and
  tested on Groovy 4.0; this repository is on Groovy 5.0.8 and `spock-core:2.4-groovy-5.0`.
- The platform declares `junit-bom:6.1.2`, while the current `build-foundation` spec says the BOM
  must stay at 5 because of `pmd-test`. The spec has drifted from the build; dropping `pmd-test`
  removes the stated reason either way.

## Goals / Non-Goals

**Goals:**
- Every test in `pmd-rules` is a Spock specification; no JUnit Jupiter test, `SimpleAggregatorTst`
  or XML descriptor remains.
- Rule cases are readable: one Java file per sample, one `where:` row per case.
- Pitest stays at 100/100/100 for both modules.
- `spock-deepmock` is available, and used wherever a test builds a chain of nested mocks by hand.

**Non-Goals:**
- No change to any rule, ruleset or published artifact.
- No move to a different PMD floor or JUnit line, and no change to the Groovy 5 line.
- No attempt to keep `pmd-test` as a fallback harness.

## Decisions

### D1. Fixtures are `.java` files under `src/test/resources`, not in a source set

Alternatives: (a) inline strings in the specification, (b) `.java` files in `src/test/java`,
(c) `.java` files in a separate source set, (d) resources.

(b) makes the build compile the fixtures with `-Werror`/Error Prone/NullAway and flag them with its
own PMD run; some fixtures cannot compile (unresolved imports for `UseTypeImports`). (c) still gets
compiled and linted unless excluded, and an exclusion is exactly what the old spec warned about.
(a) is how `codenarc-rules` does it, but 181 cases with multi-line classes read badly as strings.
(d) keeps real Java files, which an IDE highlights, and puts them on no analysis path.

Layout: `src/test/resources/<package>/fixtures/<RuleName>/<Case>.java`. Each is a single top-level
class so PMD's file-name handling is satisfied. Line numbers stay in the specification's `where:`
table, next to the case name. Short cases may stay inline as strings where a file would be noise;
the choice is per case and not specified.

### D2. A small helper runs a rule through PMD's stable API

Replace `RuleTst` with a helper (in `src/test/groovy`) that, given a rule reference, a fixture path
and an optional Java version, returns the violations. It uses `PMDConfiguration`,
`PmdAnalysis` and `RuleSetLoader`, as the existing ITs already do, and loads the rule from
`category/java/joke.xml` by classpath reference, as `SimpleAggregatorTst.addRule` does today.

Alternative: subclass `RuleTst` from Groovy. Rejected: it keeps `pmd-test`, and `RuleTst` is itself
versioned, the reason the ITs avoid it. The cost is that the helper replaces a few things `RuleTst`
did for free (report collection, version selection); the six pinned-version cases show which of
those matter.

Specification shape:

```groovy
@Tag('unit')
class AvoidAnonymousClassesSpec extends Specification {
    def 'violations are reported at the anonymous class'() {
        expect:
        violationLines('AvoidAnonymousClasses', fixture) == lines

        where:
        fixture              | lines
        'WithMethodBody'     | [3]
        'FieldOnly'          | [3]
    }
}
```

### D3. Mocking uses Spock `Mock`/`Spy` on top of `byte-buddy` and `mockito-core`

Spock 2.4 mocks final classes and spies concrete classes through those two libraries (inline mock
maker). The PMD AST nodes are mocked today by Mockito, so the same classes mock under Spock. The
package-private `RuleContext#getRule()` is the one open case: the Mockito test stubs it with an
`Answer` keyed by method name because it cannot be named from the test package. A Groovy spec
ignores visibility when naming the call, but whether Spock's proxy answers it is unproven; see
Risks.

### D4. `spock-deepmock` joins the platform and both test classpaths

Added to `dependencies/build.gradle` (version pinned like every other coordinate) and as
`testImplementation` in both modules. Existing `Mock()` use in `codenarc-rules` is flat, so DeepMock
only appears where a chain exists. Applying it is part of this change, not a follow-up: the
conversion of `VisitDelegationTest` and `ImportRulePredicatesTest` is the first place to look for
chains (`qualifier.firstChild(...)`-style calls).

### D5. `pmd-rules` gets CodeNarc dogfooding

Add `codenarc project(':codenarc-rules')`. The two modules then depend on each other's jar through
different configurations (`pmd` in one, `codenarc` in the other), which are distinct tasks
(`:codenarc-rules:pmdMain` needs the `pmd-rules` jar; `:pmd-rules:codenarcTest` needs the
`codenarc-rules` jar), so no task cycle is expected. A cycle would only appear if building one jar
needed the other. This is checked on the first build rather than assumed.

### D6. The old XML cases are the checklist

The 181 `test-code` cases are the migration inventory. Each becomes a fixture and a row, and the
count per rule is recorded in `tasks.md`, so a dropped case is visible and Pitest has the same
evidence to work with. The XML descriptors and the Java tests are deleted only after their
specification passes and Pitest is green for that rule.

## Risks / Trade-offs

- **[Mutation score drops below 100]** A converted case that asserts less than its XML original
  (for instance only the count and not the lines) leaves mutants alive → assert the same things the
  XML asserted (`expected-problems`, `expected-linenumbers`), run Pitest per rule while converting,
  and keep the old test until its replacement kills the same mutants.
- **[`spock-deepmock` against Groovy 5]** Built for the Groovy 4 variant of Spock; the Groovy 5
  variant may differ in the internals DeepMock uses → spike first: a trivial `DeepMock()` spec in
  `codenarc-rules`. If it fails, the fallback is hand-written nested mocks and filing the issue
  upstream; the DeepMock requirement then moves to a follow-up and this change ships without it.
- **[Spock cannot mock the final classes or `getRule()`]** → spike `Mock(ASTTypeExpression)` and
  the `RuleContext` stub before converting the Mockito tests. Fallback for `getRule()`: a
  hand-written `RuleContext` stub class in the spec.
- **[Helper diverges from `RuleTst`]** Behaviour such as which language versions are valid for a
  fixture may differ → the six version-pinned cases are converted first as the check.
- **[CodeNarc flags the new specs]** The strict ruleset applies to `pmd-rules` for the first time →
  fix the specifications, not the ruleset; the same bar `codenarc-rules` meets.
- **[Spec drift]** `build-foundation` says the JUnit BOM is 5; the build says 6.1.2 → this change
  removes the reason either way and does not decide the BOM version.

## Migration Plan

1. Spike (D3, D4, D5) on a throwaway spec; stop and report if any assumption fails.
2. Build wiring: platform entries, `groovy` plugin, Spock/byte-buddy/mockito/DeepMock/CodeNarc
   dependencies in `pmd-rules`; DeepMock in `codenarc-rules`.
3. Helper, then convert one rule end to end (`AvoidAnonymousClasses`, smallest) and run Pitest.
4. Convert the remaining seven rules, one at a time, each with Pitest green.
5. Convert the Mockito tests and the two ITs.
6. Remove `pmd-test`, Jupiter, direct Mockito and unused AssertJ; delete XML descriptors.
7. Apply DeepMock where chains exist; run `./gradlew check`.

Rollback is a revert of the commits; no published artifact changes.

## Open Questions

- Whether short fixtures stay inline as strings or always become files. Deferrable: the specs allow
  either.
- Whether AssertJ is still used after conversion. Decided by what remains once the ITs are Spock.
