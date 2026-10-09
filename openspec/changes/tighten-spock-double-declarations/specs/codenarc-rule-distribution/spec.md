## MODIFIED Requirements

### Requirement: The artifact's coverage of the Spock conventions is stated
The README SHALL carry a table mapping each convention in the `spock-coding-conventions` checklist to
the rule that enforces it, and SHALL name the one convention that is deliberately not enforced.

Thirteen rules against a checklist of fourteen items reads as an oversight unless the fourteenth is
accounted for. Stating the mapping costs a table and closes the question for every later reader.

The two conventions added by `tighten-spock-double-declarations` — no `@Shared` or `static` double,
and no unstated call-through on a spy — SHALL each appear in the table against their rule.

#### Scenario: Every rule maps to a convention
- **WHEN** the README's coverage table is read
- **THEN** each of the thirteen rules names the convention it enforces

#### Scenario: The unenforced convention is named
- **WHEN** the coverage table is read
- **THEN** it names "each method gets its own feature method, protected ones included" as not
  enforced, with the reason

#### Scenario: The redundant-type convention is attributed to the existing rule
- **WHEN** the coverage table is read
- **THEN** "the type is written once, on the left" maps to `DeclareMockWithExplicitType`
