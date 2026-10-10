## 1. Generalise scope recognition

- [x] 1.1 Parameterise the spy scope helper by factory kind and rename it to say what it now is; the
      two spy rules pass `Spy`
- [x] 1.2 Add `isStub()` to `MockCall` beside `isSpy()`, and a read of whether the call carries a
      `verified` named argument
- [x] 1.3 Run `RequireSpyEntryInteractionRuleSpec` and `RequireSpyInteractionResponseRuleSpec`
      unchanged and confirm both pass: they are the regression test for the rename

## 2. Read the cardinality under a response

- [x] 2.1 Add to `SpockInteraction` a read of whether an interaction carries a cardinality other than
      `_`, looking under a `>>` or `>>>`
- [x] 2.2 Confirm `isCounted`, `isNeverCalled` and `isEntryCall` are unchanged for stubbed
      interactions, so the spy rules cannot move

## 3. AvoidCardinalityOnStub

- [x] 3.1 Implement the rule over top-level statements in every block: an interaction whose receiver
      is a recognised stub and whose cardinality is present and not `_`
- [x] 3.2 Confirm `1 * stub.foo() >> value`, `0 * stub.foo()`, `(1..3) *` and `(0..2) *` are reported
- [x] 3.3 Confirm `stub.foo() >> value`, `_ * stub.foo()` and `0 * _` are silent
- [x] 3.4 Confirm a `Mock`, a `Spy`, a `Stub(verified: ...)` and a stub from a helper or parameter are
      silent
- [x] 3.5 Confirm a stub in one feature method is not a stub in the next
- [x] 3.6 Write the `unit`-tagged Spock specification covering every scenario in
      `specs/spock-stub-cardinality-rule/spec.md`

## 4. Ship the rule

- [x] 4.1 Add the rule to `rulesets/groovy/joke.groovy`, referenced by class
- [x] 4.2 Update `RulesetDistributionSpec`: expected names go from thirteen to fourteen, strict ruleset
      from 125 to 126
- [x] 4.3 Confirm the name does not collide with a CodeNarc stock rule

## 5. Dogfood

- [x] 5.1 Run `./gradlew codenarcTest` and fix any violation in the specification, never by loosening
      the ruleset
- [x] 5.2 Confirm `pmdMain` still passes over the new and changed rule classes

## 6. Documentation

- [x] 6.1 Add a README section for the rule, with the spike's table of what throws and what passes
- [x] 6.2 Document the `verified:` opt-out, and that `GroovyStub` is not recognised
- [x] 6.3 Update the coverage table: fourteen rules, fifteen conventions, one not enforced
- [x] 6.4 Update the README's count of rules that carry priority 2

## 7. Verify

- [x] 7.1 Confirm mutation analysis reaches 100/100/100 for `codenarc-rules`
- [x] 7.2 Run `./gradlew check` and confirm it is green. NEVER continue if there are violations
- [x] 7.3 Commit the completed change with `/commit-commands:commit`. The commit must touch
      `codenarc-rules/` or release-please routes it to no package and cuts no release
