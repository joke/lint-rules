# Tasks

## 1. Rule property

- [ ] 1.1 Add an integer `targetJavaVersion` property to `UseVarForLocalVariables` (default 0, meaning unset) using `PropertyFactory.intProperty`
- [ ] 1.2 Add a `@VisibleForTesting` predicate that is true when the property is set below 10, and check it at the start of `reportIfRewritableAsVar`
- [ ] 1.3 Add unit cases to `UseVarForLocalVariablesSpec` for the suppressed, kept-at-10, and unset branches of the new predicate

## 2. Fixtures and integration

- [ ] 2.1 Add an integration case for a Java 8 language version expecting no violations, using an existing or new fixture under `src/test/resources`
- [ ] 2.2 Add a case that sets the property to a non-numeric value and expects a configuration error

## 3. Documentation

- [ ] 3.1 Describe `targetJavaVersion` in the `UseVarForLocalVariables` description in `category/java/joke.xml`
- [ ] 3.2 Add a Java 8 ruleset example that sets `targetJavaVersion` to `8` to the pmd-rules README

## 4. Verification

- [ ] 4.1 Check that the unit spec still meets the 100% mutation, coverage and test-strength thresholds for the rule class
- [ ] 4.2 Run a Gradle build against a small Java 8 project that uses the ruleset with `targetJavaVersion` set to `8` and confirm no `var` violations are reported
- [ ] 4.3 Run `./gradlew check` and do not continue if there are violations
- [ ] 4.4 Commit the completed change with `/commit-commands:commit`

## Workflow follow-up

- Archive the change once implemented and verified.
