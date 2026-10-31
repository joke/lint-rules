# Design

## Context

PMD 7 gates a rule on `minimumLanguageVersion` against the language version of the file being
analysed (`RuleApplicator`, PMD 7.0.0 and 7.26.0). That version is not the consumer's `javac` target,
and the Gradle PMD plugin exposes no working setting for it: its `targetJdk` property is documented
by Gradle itself as having no effect for PMD 5 and later. So the consumer's target has to reach the
rule through its own ruleset configuration. See proposal.md - Why.

## Goals / Non-Goals

**Goals:**
- A consumer can state its target Java level in the ruleset XML and have the rule stay silent below 10.
- Rulesets that set nothing behave exactly as they do today.

**Non-Goals:**
- Detecting the target from build files or from the compiler.
- Changing PMD's own language-version handling or the Gradle plugin.
- Per-version copies of the rule.

## Decisions

**Property type: integer, default 0 meaning unset.** `PropertyFactory.intProperty` (available in
PMD 7.0.0) makes PMD reject non-numeric values such as `eight` or `1.8` at configuration time, which
satisfies the "rejected" scenario without extra code. Alternative considered: a string property parsed
by the rule. Rejected because it moves validation into the rule and needs its own error path.

**Check placement: `reportIfRewritableAsVar`, before `isRewritableAsVar`.** The guard reads the
property and stops reporting when the target is below 10. Alternative considered: folding it into
`isRewritableAsVar`. Rejected because that method is the pure "would var compile" decision and should
stay independent of configuration. The existing unit spec stubs siblings on a spy, so a new
`@VisibleForTesting` predicate for the guard keeps each feature testing one decision.

**Keep `minimumLanguageVersion="10"`.** It stays as documentation of the language floor and still
applies when PMD does analyse at an older version. The property covers the case PMD cannot see.

**Alternative rejected: Gradle `pmd { targetJdk }`.** Gradle's own deprecation message says it has no
effect for PMD 5 and later, so it cannot be the mechanism.

## Risks / Trade-offs

- [A consumer forgets the property on a Java 8 codebase] → The rule behaves as today and reports
  `var` candidates. The README example and the category description show the setting.
- [Property name is a public contract] → Name it `targetJavaVersion` now; renaming later would break
  consumer rulesets, so it is part of the change review.
- [Runtime behaviour under Gradle is inferred, not observed] → The integration spec covers the PMD
  path with a `'8'` language version; a Gradle run against a Java 8 fixture project is a manual check
  in tasks.md.

## Migration Plan

None required. Existing rulesets get the unchanged default. Rollback is removing the property from the
rule and the README, which restores today's behaviour.

## Open Questions

None that block the specs or tasks.
