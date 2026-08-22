## ADDED Requirements

### Requirement: The artifact's coverage of the Spock conventions is stated
The README SHALL carry a table mapping each convention in the `spock-coding-conventions` checklist to
the rule that enforces it, and SHALL name the one convention that is deliberately not enforced.

Eleven rules against a checklist of twelve items reads as an oversight unless the twelfth is
accounted for. Stating the mapping costs a table and closes the question for every later reader.

#### Scenario: Every rule maps to a convention
- **WHEN** the README's coverage table is read
- **THEN** each of the eleven rules names the convention it enforces

#### Scenario: The unenforced convention is named
- **WHEN** the coverage table is read
- **THEN** it names "each method gets its own feature method, protected ones included" as not
  enforced, with the reason

### Requirement: One convention is deliberately not implemented
No rule SHALL be added for "each method gets its own feature method, protected ones included, and is
called directly". It SHALL remain a review convention.

Deciding it needs the list of methods the subject declares. That list is in another file, and CodeNarc
analyses source without a compile classpath, so the rule would have nothing to compare the
specification against. Approximating it — by matching feature-method names against method names, say —
would report a specification whose feature names read as sentences, which is every specification this
artifact ships.

This requirement exists so that a later change does not start building it, and so that a reader
comparing the checklist against the ruleset finds the gap explained rather than open.

#### Scenario: No rule attempts the check
- **WHEN** the rule classes are inspected
- **THEN** none reads a subject's declared methods or compares them against feature methods

#### Scenario: The boundary is documented
- **WHEN** the README's coverage table is read
- **THEN** it states that this convention needs the subject's API and is left to review

### Requirement: One rule reports a missing statement rather than a present one
The README section for `RequireSpyEntryInteraction` SHALL lead with the failure it prevents rather
than with the convention it enforces, because it is the only rule this artifact ships whose fix is to
add a line.

Every other rule reports something present that should be removed or moved, and a reader can act on
the report without understanding the reasoning. A rule demanding an addition cannot be acted on that
way: a consumer who does not know that `0 * _` is about to fail on the spy's entry call reads the
report as noise and excludes the rule.

#### Scenario: The section leads with the failure
- **WHEN** the README section for `RequireSpyEntryInteraction` is read
- **THEN** its first paragraph states that without the entry interaction, `0 * _` fails on the call
  the feature method exists to make
