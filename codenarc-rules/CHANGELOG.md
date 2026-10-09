# Changelog

## [1.0.0](https://github.com/joke/lint-rules/compare/codenarc-rules-v0.0.0...codenarc-rules-v1.0.0) (2026-10-09)


### ⚠ BREAKING CHANGES

* coordinates move from io.github.joke.pmd:rules to io.github.joke.lint:pmd-rules, and rule classes from io.github.joke.pmd.rules.java to io.github.joke.lint.pmd.rules.java. Ruleset resource paths are unchanged, so consumers edit only the dependency coordinate. The retired coordinate receives one final POM-only publication carrying a relocation to the new one.

### Features

* **codenarc-rules:** add four Spock interaction rules on a shared model ([e65fe3c](https://github.com/joke/lint-rules/commit/e65fe3ce9e08ae455722fa530e70a05680b75e6d))
* **codenarc-rules:** add three Spock fixture rules on a shared base ([79fab75](https://github.com/joke/lint-rules/commit/79fab75ddad5050a98927934f83e5eaa09f12f7d))
* **codenarc-rules:** add three Spock spy and verifyAll rules ([5ed1801](https://github.com/joke/lint-rules/commit/5ed1801a191d2c63f076352dabed6dba97d7992b))
* publish PMD and CodeNarc rules as separate artifacts ([1a16999](https://github.com/joke/lint-rules/commit/1a16999228d7a5eb16f0853caa6dd283a1b0aa27))
