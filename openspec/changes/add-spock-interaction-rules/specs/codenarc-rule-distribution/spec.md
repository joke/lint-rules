## ADDED Requirements

### Requirement: The feature-method model is internal machinery
The rules SHALL share one model of a Spock feature method — its partition into labelled blocks and
the classification of each top-level statement as an interaction, an exception capture or a value
assertion — rather than each rule computing its own.

Four rules read the same structure, and a disagreement between two of them about what an interaction
is would surface as one rule reporting a line the other exempts. Sharing the model makes that
impossible rather than unlikely.

The model SHALL be internal: not referenced from any shipped ruleset, not documented for consumers,
and carrying no compatibility promise. The artifact promises its rules and its rulesets.

`and:` SHALL continue the block it follows rather than starting one, matching CodeNarc's own label
vocabulary, which excludes `and:` because it carries no semantic weight. Where a statement carries
several labels — which happens when a labelled block is empty — the last SHALL be taken.

Classification SHALL be by syntactic shape alone. No rule SHALL attempt to determine whether a target
is a mock, a stub, a spy or a real object, because CodeNarc analyses source without a compile
classpath.

#### Scenario: One model serves every rule
- **WHEN** the rule classes are inspected
- **THEN** each reads the shared feature-method model rather than partitioning statements itself

#### Scenario: The model is not shipped as a ruleset entry
- **WHEN** `rulesets/groovy/joke.groovy` is inspected
- **THEN** it references rule classes only

#### Scenario: An and block continues its predecessor
- **WHEN** a feature method contains `then:` followed by `and:`
- **THEN** the model reports both runs as one `then:` block

### Requirement: The adoption cost of the interaction rules is documented
The README SHALL state, ahead of the individual rule descriptions, that the interaction rules report
in volume on a codebase that has not adopted strict mocking — `RequireStrictMockingTerminator` reports
once per `then:` block without a terminator, which on such a codebase is every one of them.

A first run that produces hundreds of violations reads as a broken ruleset unless the reader was told
to expect it. Each rule is individually excludable, and the README SHALL say so in the same place.

The README SHALL also record the two known gaps: `ValueAssertionsBelongInExpectBlock` does not report
a bare truthiness check, and `RequireValidatedInteractionArguments` does not report a closure that is
truthy without being constant.

#### Scenario: The adoption cost is stated before the rules
- **WHEN** the README's CodeNarc rules section is read
- **THEN** the volume of first-run violations is stated before the individual rule descriptions
- **AND** each rule is documented as individually excludable

#### Scenario: The known gaps are recorded
- **WHEN** the README's rule descriptions are read
- **THEN** both under-reporting gaps are stated with the reason each is not closed

## MODIFIED Requirements

### Requirement: The Spock specifications are the corpus the rules analyse
The `codenarc-rules` module SHALL analyse its own Spock specifications with the ruleset it publishes,
so that no rule is released without having been run on real code by the build that produces it.

A rule that fires on this repository's own specifications SHALL be answered by fixing the
specification, never by excluding the rule from `rulesets/groovy/joke-strict.groovy`. An exclusion
there would be this repository declining a rule it wrote and still publishes to consumers.

`add-spock-interaction-rules` is the first change in which the dogfood corpus has real violations
rather than none. `AvoidUnrollAnnotationRuleSpec` carries two value assertions in a `then:` block and
no terminator, and is rewritten to comply:

```groovy
then:                          →   then:
rule.name == 'Renamed'             0 * _
rule.priority == 3
                                   expect:
                                   rule.name == 'Renamed'
                                   rule.priority == 3
```

The rewritten `then:` block contains nothing but the terminator. That is what the unconditional
terminator means in a specification with no collaborators, and it SHALL be documented as the worked
example rather than presented as an artefact of the migration.

#### Scenario: The module analyses itself with its own artifact
- **WHEN** `./gradlew codenarcTest` runs
- **THEN** it analyses `codenarc-rules/src/test/groovy` with `rulesets/groovy/joke-strict.groovy`
  resolved from the `codenarc` configuration

#### Scenario: A violation in the specifications fails the build
- **WHEN** a specification violates a rule this artifact publishes
- **THEN** `check` fails

#### Scenario: Both analyses run
- **WHEN** `./gradlew check` runs
- **THEN** `pmdMain` analyses the module's Java with `:pmd-rules`
- **AND** `codenarcTest` analyses the module's Groovy with `:codenarc-rules`

#### Scenario: The dogfood violations are fixed in the specification
- **WHEN** the interaction rules are added
- **THEN** `AvoidUnrollAnnotationRuleSpec` is rewritten to comply
- **AND** no rule is excluded from `rulesets/groovy/joke-strict.groovy`
