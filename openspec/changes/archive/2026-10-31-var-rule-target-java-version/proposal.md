# Proposal

## Why

`UseVarForLocalVariables` is gated by `minimumLanguageVersion="10"`, but PMD only applies that gate
to the language version it analyses with. The Gradle PMD plugin offers no way to set that version for
PMD 7 (`targetJdk` is a no-op there), so PMD analyses every file at its latest Java version. A Java 8
project therefore receives `var` violations it cannot act on. The rule needs to know the consumer's
target Java version from its own configuration.

## What Changes

- `UseVarForLocalVariables` gains a rule property stating the consumer's target Java version, set
  from the ruleset XML the consumer already passes to Gradle.
- When that property is set below 10, the rule reports nothing. When it is unset, the rule behaves
  as today: only PMD's `minimumLanguageVersion` gate applies.
- The integration spec gains a Java 8 case that the existing spec does not cover.
- The rule's category documentation and the pmd-rules README describe the property and show how to set it.

No breaking changes: existing rulesets keep their current behaviour.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `var-local-variables-rule`: adds a target-version property that suppresses the rule below Java 10,
  and makes the Java 8 skip scenario testable through the integration spec.

## Impact

- `pmd-rules/src/main/java/io/github/joke/lint/pmd/rules/java/UseVarForLocalVariables.java`: property
  definition and the version check.
- `pmd-rules/src/main/resources/category/java/joke.xml`: property documentation in the rule entry.
- `pmd-rules/src/test/groovy/.../UseVarForLocalVariablesSpec.groovy` and
  `...IntegrationSpec.groovy`, plus fixtures under `src/test/resources`: unit and integration cases.
- `README.md`: example ruleset configuration.
- No dependency or minimum-version changes; PMD stays at 7.0.0 compile floor.
