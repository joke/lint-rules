# Spec Delta

## ADDED Requirements

### Requirement: Rule tests are unit and integration specifications
The `codenarc-rules` module SHALL test its rules in two layers: unit specifications that test each rule
class and each helper class in isolation, and integration specifications that run each rule through
CodeNarc against a fixture file. Every rule SHALL have both.

#### Scenario: Each rule has both layers
- **WHEN** the specifications of a CodeNarc rule are inspected
- **THEN** a `unit` specification tests the rule class with its AST collaborators replaced
- **AND** an `integration` specification runs the rule through CodeNarc against a violating fixture

#### Scenario: Unit specifications reach mutation analysis
- **WHEN** `./gradlew test` and `./gradlew pitest` run
- **THEN** both execute the `unit`-tagged specifications
- **AND** neither executes an `integration`-tagged specification

### Requirement: Integration samples are fixture files, never strings
The source an integration specification analyses SHALL be a `.groovy` file under
`codenarc-rules/src/test/resources`, one sample per file, outside every source set. No specification
SHALL embed Groovy source in a string. Unit specifications need no source text: they replace the AST
nodes a class reads with doubles.

#### Scenario: No specification embeds source text
- **WHEN** the specifications under `src/test/groovy` are inspected
- **THEN** none contains a Groovy class declaration inside a string literal

#### Scenario: Samples live in fixture files
- **WHEN** a rule's integration test data is inspected
- **THEN** every sample is a `.groovy` file under `src/test/resources`
- **AND** no file in a source set carries a deliberate violation

### Requirement: Ruleset resolution is exercised by an integration specification
The classpath resolution of the shipped rulesets SHALL be exercised by a specification tagged
`integration`, mirroring the PMD module's ruleset distribution tests: resolution from the `codenarc`
configuration is the single mechanism the distribution model depends on, so it SHALL be exercised on
every build rather than discovered by the first consumer.

#### Scenario: A missing or misnamed ruleset resource fails the build
- **WHEN** a shipped ruleset is absent from the jar or named differently
- **THEN** the `integration`-tagged specification fails

### Requirement: The ruleset specification initialises the rule registry
The specification that loads the shipped rulesets SHALL initialise CodeNarc's rule registry first.
`CodeNarcRunner` populates the registry on a real run and nothing populates it when `RuleSetUtil` is
driven directly, so without the initialisation the strict ruleset fails to load with `No such rule
named [...]` — a message that reads as a distribution fault rather than an uninitialised registry.

#### Scenario: The registry is initialised before a ruleset loads
- **WHEN** the ruleset resolution specification loads the strict ruleset
- **THEN** it loads without a `No such rule named` error

## REMOVED Requirements

### Requirement: Rule tests are Spock specifications
**Reason**: It embedded samples in specifications as Groovy strings and tagged every rule
specification `unit`, although those specifications ran CodeNarc over source.
**Migration**: Replaced by "Rule tests are unit and integration specifications", "Integration
samples are fixture files, never strings" and "Ruleset resolution is exercised by an integration
specification". The tagging obligation lives in `build-foundation`, unchanged.
