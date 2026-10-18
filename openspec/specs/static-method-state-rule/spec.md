# static-method-state-rule Specification

## Purpose

The `StaticMethodsModifyStaticState` rule: which `static` methods it reports, what counts as
writing private static state, the utility-class exception, the named-constructor exception, the
framework carve-outs that have no compliant rewrite, and its documentation, test and
mutation-coverage obligations.

## Requirements

### Requirement: StaticMethodsModifyStaticState reports static methods that do not write static state
The artifact SHALL provide a rule named `StaticMethodsModifyStaticState` that reports a `static`
method unless it writes a private static field, its declaring type is a utility class, or it is a
named constructor.

The rule SHALL be implemented in Java against the PMD 7 Java AST.

The rule SHALL NOT declare a `minimumLanguageVersion`: `static` exists in every Java version.

The rule's rationale SHALL be stated as an invariant about meaning rather than as a claim about
mockability. Mockito's inline mock maker and Spock's `SpyStatic` both mock static methods, so a
mockability argument is false and would discredit the rule. The invariant is that `static` on a
method means exactly one thing — the method writes class-level state — so that the modifier carries
information for a reader.

#### Scenario: A static helper is reported
- **WHEN** a class with instance methods declares `static String format(int n) { return "" + n; }`
- **THEN** the rule reports a violation on that method

#### Scenario: An instance method is not reported
- **WHEN** a class declares `String format(int n) { return "" + n; }`
- **THEN** the rule reports no violation

### Requirement: Named constructors are exempt
The rule SHALL NOT report a `static` method whose declared return type names its own declaring type,
or names an interface that the declaring type **directly declares** in its `implements` clause. Such
a method is a constructor with a name: a test double over it could only return what the constructor
it wraps already returns, so there is nothing to intercept and no seam is lost.

The comparison SHALL be made on the AST by **simple name**, without type resolution. This matches the
approach taken by `UseVisibleForTestingAnnotation` and for the same reason: resolution requires an
`auxclasspath` that consumers frequently do not configure, and a misconfigured one would make the
rule silently pass rather than fail visibly.

The consequence SHALL be documented: a factory returning a same-named type from a different package
is exempted too. A missed report that review can catch is preferred to silent under-reporting on
every misconfigured `auxclasspath`.

A return type that is a **superclass** of the declaring type, or an interface inherited transitively
rather than directly declared, SHALL NOT be exempt — that shape is a factory for something else,
which is a helper.

#### Scenario: A static factory returning the enclosing type is not reported
- **WHEN** a class with instance methods declares `static Foo of(int x) { return new Foo(x); }`
- **THEN** the rule reports no violation

#### Scenario: A static factory returning a directly implemented interface is not reported
- **WHEN** `class FooImpl implements Foo` declares `static Foo of(int x) { return new FooImpl(x); }`
- **THEN** the rule reports no violation

#### Scenario: A static factory returning an unrelated type is reported
- **WHEN** a class with instance methods declares `static Bar of(int x) { return new Bar(x); }`
- **THEN** the rule reports a violation

#### Scenario: A static factory returning a superclass is reported
- **WHEN** `class Foo extends Base` declares `static Base of(int x) { return new Foo(x); }`
- **THEN** the rule reports a violation, because only the declaring type and its directly declared
  interfaces are exempt

#### Scenario: A void static method is reported
- **WHEN** a class with instance methods declares `static void configure(int x) { }`
- **THEN** the rule reports a violation, because `void` names no type

### Requirement: Writing a private static field is an assignment, not a read
The rule SHALL treat a method as writing a private static field when its body assigns one, whether by
simple assignment, compound assignment, increment or decrement. The field SHALL be declared in the
same top-level type as the method.

The rule SHALL NOT treat any of the following as a write:

- reading or returning a private static field
- invoking a method on a private static field, such as `REGISTRY.put(k, v)`

A read is not a write because a static method that only returns a private static field is an
accessor whose encapsulation was already fake — the field should have been public. A mutating call
is excluded because it is undecidable: PMD cannot distinguish `put` from `size`.

A consequence SHALL be documented: a `private static final` field can never justify a static method,
because a final field can only be assigned in its initializer.

#### Scenario: A static method assigning a private static field is not reported
- **WHEN** a class declares `private static Foo instance;` and
  `static void reset() { instance = null; }`
- **THEN** the rule reports no violation

#### Scenario: A compound assignment counts as a write
- **WHEN** a class declares `private static int count;` and `static void bump() { count += 1; }`
- **THEN** the rule reports no violation

#### Scenario: An increment counts as a write
- **WHEN** a class declares `private static int count;` and `static void bump() { count++; }`
- **THEN** the rule reports no violation

#### Scenario: A static accessor over a private static field is reported
- **WHEN** a class declares `private static Foo instance;` and
  `static Foo get() { return instance; }`
- **THEN** the rule reports a violation

#### Scenario: A mutating call on a private static field is reported
- **WHEN** a class declares `private static final Map<K, V> REGISTRY = new HashMap<>();` and
  `static void register(K k, V v) { REGISTRY.put(k, v); }`
- **THEN** the rule reports a violation

#### Scenario: Writing a non-private static field is not an exception
- **WHEN** a class declares `static int count;` and `static void bump() { count++; }`
- **THEN** the rule reports a violation, because the field is not private

#### Scenario: Writing a local variable is not writing a field
- **WHEN** a static method assigns only local variables and parameters
- **THEN** the rule reports a violation

### Requirement: Utility classes are exempt
The rule SHALL NOT report any static method declared in a utility class. A type is a utility class
when it carries a **utility-class marker annotation**, or when it declares no instance methods and
declares no `public` or `protected` constructor.

The marker check SHALL be evaluated **first**, short-circuiting the structural test. Lombok's
`@UtilityClass` privatises the constructor and makes every member static during annotation
processing, so the source PMD reads declares instance-looking methods and no constructor at all —
which defeats both halves of the structural test on a type that is, once compiled, exactly the shape
the exemption describes.

Marker annotations SHALL be matched by simple name and held as a set, seeded with `UtilityClass`.
Matching by name SHALL NOT introduce a dependency on Lombok: nothing is imported, and a project
without Lombok never matches the name.

A class that declares no constructor at all SHALL be evaluated on its implicit constructor, which
takes the class's own access. A `public class` with no declared constructor is therefore not a
utility class, which agrees with the stock `UseUtilityClass` rule already enabled in `.pmd.xml`.

Instance *fields* SHALL NOT disqualify a utility class; only instance methods do.

Only methods declared directly on the type SHALL be considered. Inherited methods and methods of
nested types SHALL NOT be.

#### Scenario: A Lombok utility class is exempt
- **WHEN** a `public class` carries `@UtilityClass` and declares methods without the `static` keyword
- **THEN** the rule reports no violation, because the marker is checked before the structural test

#### Scenario: The marker is matched without Lombok present
- **WHEN** a type carries an annotation whose simple name is `UtilityClass` from any package
- **THEN** the rule reports no violation on its static methods

#### Scenario: A sealed utility class is exempt
- **WHEN** a class declares only static methods and a `private` constructor
- **THEN** the rule reports no violation

#### Scenario: A public class with an implicit constructor is not a utility class
- **WHEN** a `public class` declares only static methods and no constructor
- **THEN** the rule reports a violation on each static method

#### Scenario: A package-private class with an implicit constructor is a utility class
- **WHEN** a package-private class declares only static methods and no constructor
- **THEN** the rule reports no violation

#### Scenario: One instance method disqualifies the type
- **WHEN** a class with a `private` constructor declares one instance method alongside static methods
- **THEN** the rule reports a violation on each static method

#### Scenario: An interface with only static methods is exempt
- **WHEN** an interface declares only static methods
- **THEN** the rule reports no violation

#### Scenario: An interface with default methods is not exempt
- **WHEN** an interface declares a static method and a default method
- **THEN** the rule reports a violation on the static method

#### Scenario: Nested types are evaluated independently
- **WHEN** a static nested utility class sits inside a class with instance methods
- **THEN** the nested class's static methods are not reported

### Requirement: Methods with no compliant rewrite are carved out
The rule SHALL NOT report a method that the platform requires to be static, because a violation the
consumer cannot fix is worse than a missed one — the standard `var-local-variables-rule` sets.

The carve-outs SHALL be:

- `public static void main(String[])`
- a method annotated `@BeforeAll` or `@AfterAll`, which JUnit 5 requires to be static outside the
  `PER_CLASS` lifecycle

Annotations SHALL be matched by simple name.

A `@MethodSource` provider SHALL NOT be carved out, and this SHALL be documented. The annotation sits
on the test method and names the provider by string, so no structural check can distinguish a
provider from any other static method. Such methods are suppressed at the site.

#### Scenario: A main method is not reported
- **WHEN** a class declares `public static void main(String[] args)`
- **THEN** the rule reports no violation

#### Scenario: A method named main with a different signature is reported
- **WHEN** a class declares `public static void main()`
- **THEN** the rule reports a violation

#### Scenario: A JUnit lifecycle method is not reported
- **WHEN** a test class declares `@BeforeAll static void setUp()`
- **THEN** the rule reports no violation

#### Scenario: A MethodSource provider is reported
- **WHEN** a test class declares `static Stream<Arguments> cases()`
- **THEN** the rule reports a violation, and the compliant response is `@SuppressWarnings`

### Requirement: The rule is documented in the category file and the README
`category/java/joke.xml` SHALL declare the rule with its implementing class, a message, a
description, a priority and both a violating and a compliant example. `rulesets/java/joke.xml` SHALL
include it.

`README.md` SHALL document the rule, including that `@SuppressWarnings` is the expected response for
a framework-forced static rather than a sign of failure.

#### Scenario: The category entry is complete
- **WHEN** the `StaticMethodsModifyStaticState` entry in `category/java/joke.xml` is inspected
- **THEN** it declares a `class`, a `message`, a `<description>`, a `<priority>` and an `<example>`

#### Scenario: The convenience ruleset includes the rule
- **WHEN** `rulesets/java/joke.xml` is inspected
- **THEN** it references `StaticMethodsModifyStaticState`

### Requirement: The rule is covered by isolated unit tests and a smoke fixture
The rule SHALL be tested by a unit specification that isolates the rule class and covers every
reported and every non-reported case above, meeting the project's 100% mutation, coverage and test
strength thresholds on that specification alone. An integration specification SHALL also run the rule
through PMD against at least one violating fixture file.

#### Scenario: Every specified case has a unit case
- **WHEN** the rule's unit specification is inspected
- **THEN** it contains a case for each reported and each non-reported scenario in this spec

#### Scenario: Mutation thresholds are met by the unit specification
- **WHEN** `./gradlew pitest` runs
- **THEN** the rule class meets 100% mutation coverage, line coverage and test strength
- **AND** no specification tagged `integration` contributes to that result

#### Scenario: The rule fires through PMD
- **WHEN** the rule's integration specification runs
- **THEN** PMD reports a violation for the violating fixture

### Requirement: The rule class complies with this artifact's own rules
The rule class SHALL itself comply with `AvoidPrivateAndProtectedMethods` and `UseVisibleForTestingAnnotation` from its first commit.

#### Scenario: The rule class is analysed by the published rules
- **WHEN** `./gradlew check` runs
- **THEN** `pmdMain` reports no violation against the rule class
