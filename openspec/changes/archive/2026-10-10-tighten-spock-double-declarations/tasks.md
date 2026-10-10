## 1. Shared spy recognition

- [x] 1.1 Extract "spies in scope" (class fields plus the feature method's locals initialised from a
      `Spy(...)` call) out of `RequireSpyEntryInteractionRule`'s visitor into one helper both spy rules
      use
- [x] 1.2 Add to `SpockInteraction` a read of whether an interaction carries a `>>`/`>>>` response, and
      expose whether its cardinality is the literal `0`
- [x] 1.3 Run `RequireSpyEntryInteractionRuleSpec` unchanged and confirm it still passes: it is the
      regression test for the extraction

## 2. DeclareMockWithExplicitType — redundant type

- [x] 2.1 For a typed local and a typed field, compare the declared type's name (generics ignored)
      with the factory call's first positional argument
- [x] 2.2 Confirm against a fixture what CodeNarc hands the rule for `Mock(TypeMirror)` — a
      `ClassExpression` or an unresolved `VariableExpression` — and that name comparison works for
      both
- [x] 2.3 Report only on equal types; confirm `Collection<String> c = Mock(List)`, `Spy(realInstance)`,
      `Spy(constructorArgs: [...])` and `Type x = Mock()` are silent
- [x] 2.4 Confirm `def x = Mock(Type)` is still reported once, not once per condition
- [x] 2.5 Extend `DeclareMockWithExplicitTypeRuleSpec` for every scenario in
      `specs/spock-mock-declaration-rule/spec.md`

## 3. AvoidSharedOrStaticMock

- [x] 3.1 Implement the rule over `FieldNode`: `@Shared` (`Shared` / `spock.lang.Shared`) or `static`,
      with a `Mock`/`Stub`/`Spy` initialiser, reusing the factory-name set
- [x] 3.2 Guard against reporting one non-private declaration twice (property and field)
- [x] 3.3 Confirm an instance field, a non-double shared field, and a `setupSpec` assignment are silent
- [x] 3.4 Write the `unit`-tagged Spock specification covering every scenario in
      `specs/spock-shared-mock-rule/spec.md`

## 4. RequireSpyInteractionResponse

- [x] 4.1 Implement the rule over top-level statements in every block: a counted interaction whose
      receiver is a recognised spy, with no response
- [x] 4.2 Exempt a literal `0` cardinality and the bare property `spy._` at any cardinality
- [x] 4.3 Confirm `spy._(arg)`, a pattern-matched method name, `_ *` and `(1..3) *` are reported
- [x] 4.4 Confirm a `Mock`/`Stub` receiver and a spy from a helper or parameter are silent
- [x] 4.5 Add a fixture asserting the required tail — `>>` responses, `1 * spy._`, `0 * _` — passes this
      rule, `RequireSpyEntryInteraction` and `RequireStrictMockingTerminator`
- [x] 4.6 Write the `unit`-tagged Spock specification covering every scenario in
      `specs/spock-spy-response-rule/spec.md`

## 5. Ship the rules

- [x] 5.1 Add both rules to `rulesets/groovy/joke.groovy`, referenced by class
- [x] 5.2 Update `RulesetDistributionSpec`: expected names go from eleven to thirteen, strict ruleset
      from 123 to 125
- [x] 5.3 Confirm no name collides with a CodeNarc stock rule

## 6. Dogfood

- [x] 6.1 Run `./gradlew codenarcTest` and fix any violation in the specification, never by loosening
      the ruleset — `DeclareMockWithExplicitType` now reports more than before
- [x] 6.2 Confirm `pmdMain` still passes over the new and changed rule classes

## 7. Documentation

- [x] 7.1 Add a README section for each new rule, and extend `DeclareMockWithExplicitType`'s with the
      redundant-type case and the equal-types-only boundary
- [x] 7.2 Document the gaps: `setupSpec` assignment to a shared field, and a spy from a helper
- [x] 7.3 Document that `1 * spy._` is the one spy interaction left to call through, and why
- [x] 7.4 State in the README that `DeclareMockWithExplicitType` reports more on upgrade
- [x] 7.5 Update the coverage table: thirteen rules, fourteen conventions, one not enforced

## 8. Verify

- [x] 8.1 Confirm mutation analysis reaches 100/100/100 for `codenarc-rules`
- [x] 8.2 Run `./gradlew check` and confirm it is green. NEVER continue if there are violations
- [ ] 8.3 Commit the completed change with `/commit-commands:commit`. The commit must touch
      `codenarc-rules/` or release-please routes it to no package and cuts no release
