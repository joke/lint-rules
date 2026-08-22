# lint-rules

House static-analysis rules, published as two artifacts:

| artifact | tool | analyses |
|---|---|---|
| `io.github.joke.lint:pmd-rules` | [PMD 7](https://pmd.github.io/) | Java |
| `io.github.joke.lint:codenarc-rules` | [CodeNarc 4](https://codenarc.org/) | Groovy, aimed at Spock specifications |

Both are implemented as Java rule classes and shipped as rulesets you reference from your build. They
version independently and are consumed independently — there is no reason to take both.

> **Moving from `io.github.joke.pmd:rules`?** It is now `io.github.joke.lint:pmd-rules`. The ruleset
> resource paths are unchanged, so only the dependency coordinate moves. The old coordinate carries a
> relocation POM, so Maven and Gradle will tell you the same thing.

## Use the PMD rules

```groovy
dependencies {
    pmd 'io.github.joke.lint:pmd-rules:<version>'
}

pmd {
    ruleSets = ['rulesets/java/joke-strict.xml']
}
```

That is the whole analysis this project runs on itself: PMD's `bestpractices`, `codestyle`, `design`,
`errorprone`, `multithreading` and `performance` categories, composed with the exclusions and
property overrides that make them workable, plus every rule this artifact defines. It needs **PMD
7.26.0 or later** — see [PMD versions](#pmd-versions).

To keep your own composition and take only the rules from here, reference the convenience ruleset and
add PMD's categories yourself:

```groovy
pmd {
    ruleSets = [
            'category/java/bestpractices.xml',   // PMD's own categories, if you want them
            'rulesets/java/joke.xml',            // every rule this artifact defines
    ]
}
```

`rulesets/java/joke.xml` is a convenience selection of everything here. To pick rules individually,
reference them out of the catalogue instead:

```groovy
pmd {
    ruleSets = ['category/java/joke.xml/UseVarForLocalVariables']
}
```

If you enable PMD's `codestyle` category alongside these rules, exclude `TooManyStaticImports`. It is
in direct opposition to `UseStaticImports`, which this artifact defines, and no configuration
satisfies both. `rulesets/java/joke-strict.xml` already excludes it.

**`ruleSets` cannot subtract.** Wanting `joke-strict.xml` minus one rule means writing your own
ruleset file that references it and excludes from it, and pointing `ruleSetFiles` at that:

```xml
<rule ref="rulesets/java/joke-strict.xml">
    <exclude name="SomeRuleYouDisagreeWith"/>
</rule>
```

## Use the CodeNarc rules

```groovy
dependencies {
    codenarc 'io.github.joke.lint:codenarc-rules:<version>'
}

codenarc {
    config = resources.text.fromString("ruleset { ruleset('rulesets/groovy/joke-strict.groovy') }")
}
```

That is the whole analysis this project runs on its own Groovy: the curated selection of CodeNarc
stock rules, plus every rule this artifact defines. It needs **CodeNarc 4.0.0 or later** — see
[CodeNarc versions](#codenarc-versions).

**Why the stub?** Gradle cannot take a classpath ruleset. Its `CodeNarcInvoker` passes the ruleset to
CodeNarc's Ant task as `"file:" + config`, and `CodeNarcActionParameters` exposes no classpath
option — so unlike PMD's `ruleSets`, you cannot simply name `rulesets/groovy/joke-strict.groovy`.
CodeNarc's *own* loader does resolve nested `ruleset(...)` references from the classpath, so the
one-line stub above is the whole indirection. The stub is a pointer, not a policy: the composition
lives in the artifact, and you get its updates by bumping the dependency.

The outer `ruleset { }` is required. CodeNarc's `RuleSetBuilder` exposes only `ruleset(Closure)` at
the top level; `ruleset(String)` is a method on the closure's delegate, so a bare
`ruleset('rulesets/...')` fails at analysis time.

To take only the rules from here and keep your own composition, reference the convenience ruleset
instead:

```groovy
codenarc {
    config = resources.text.fromString("ruleset { ruleset('rulesets/groovy/joke.groovy') }")
}
```

`rulesets/groovy/joke.groovy` contains every rule this artifact defines and names no CodeNarc stock
rule, so it resolves identically under every supported CodeNarc.

## CodeNarc versions

The rules are compiled against **CodeNarc 4.0.0** and run on any later 4.x.

CodeNarc's compatibility surface is two-dimensional in a way PMD's is not: the artifact coordinate
itself encodes the Groovy line.

| line | coordinate | supported |
|---|---|---|
| Groovy 3 | `org.codenarc:CodeNarc:3.x` | no |
| Groovy 4 | `org.codenarc:CodeNarc:3.x-groovy-4.0` | no |
| Groovy 5 | `org.codenarc:CodeNarc:4.0.0` and later | **yes** |

The rule classes touch only `AbstractAstVisitorRule`, `AbstractAstVisitor`, `Violation`,
`SourceCode`, `org.codehaus.groovy.ast.*` and one CodeNarc class marked internal, so they would
plausibly run on the older lines too — but nothing tests that, and a plausible claim is not a
supported one. The Groovy 4 line additionally cannot carry the shipped composition
at its own oldest release: `joke-strict.groovy` names `SpockMissingAssert`, which CodeNarc only added
in 3.3.0.

**One internal CodeNarc class is used deliberately.** `org.codenarc.rule.junit.SpockUtil`, whose
Javadoc says it is not intended for general use, supplies the specification gate, the feature-method
predicate and the Spock label vocabulary. The alternative was reimplementing them, which would make
this artifact's judgement drift silently from the stock Spock rules you compose it with; depending on
`SpockUtil` makes the same disagreement a loud failure instead.

The exposure is stated rather than mitigated away. A CodeNarc release that renames or removes
`SpockUtil` breaks these rules **inside your analysis**, not in this project's build. What the
dependency flow buys is that this repository fails first — Dependabot raises `org.codenarc:CodeNarc`
here, the build breaks, and the artifact is re-released. It does **not** protect you if you are
already running a CodeNarc newer than this artifact's latest release.

The published POM declares no dependencies. You supply CodeNarc yourself, on Gradle's `codenarc`
configuration, at a version you choose.

## PMD versions

The rules are compiled against **PMD 7.0.0** and run on any later PMD 7.x. Compiling against the
floor rather than the newest release is deliberate: rules built against an older API run on newer
PMD, whereas rules built against a newer API fail with `NoSuchMethodError` inside your analysis, at a
point where the stack trace blames PMD rather than this artifact. Every release runs its integration
tests against both the floor and the newest supported version.

The shipped resources do not all carry the same floor:

| resource | PMD floor | why |
|---|---|---|
| the rule classes | 7.0.0 | compiled against the floor API |
| `category/java/joke.xml` | 7.0.0 | names only rules this artifact defines |
| `rulesets/java/joke.xml` | 7.0.0 | references only `category/java/joke.xml` |
| `rulesets/java/joke-strict.xml` | **7.26.0** | names PMD stock rules this artifact does not own |

The first three reference none of PMD's stock categories and exclude nothing from them, so they
resolve identically under every PMD 7 version. Composing them with PMD's own categories is yours to
do, which is what keeps your PMD version yours to choose.

`rulesets/java/joke-strict.xml` is the exception, and it is why the composition can ship at all. A
file naming stock rules is pinned to the PMD versions that still spell those rules the same way: a
renamed or removed rule is a hard ruleset-load failure, not a warning. Its floor is 7.26.0 rather
than 7.0.0 for a concrete reason — it excludes `ImplicitFunctionalInterface`, which PMD added after
7.0.0. That floor is the range this project supports and analyses itself with; no build check
asserts it.

**On a PMD older than 7.26.0, use `rulesets/java/joke.xml`** and compose the stock categories
yourself.

**The published POM declares no dependencies.** PMD comes from your `pmd` configuration at whatever
version you picked, and this artifact never overrides it. Every dependency the module declares sits
on a configuration that cannot reach the POM — `compileOnly`, `annotationProcessor` or a test
configuration — which is what keeps it empty.

The jar itself is Java 11 bytecode, so the JVM running PMD must be Java 11 or later. This says
nothing about the source you analyse: analysing Java 8 code is a property of your PMD language
version, not of this artifact.

## PMD rules

### UseVarForLocalVariables

Reports local variable declarations written with an explicit type where `var` would compile and
preserve the declared type. Repeating a type the compiler already knows adds no information and makes
the declaration harder to change.

Reported — single-variable local declarations, including basic and enhanced `for` loop variables and
try-with-resources:

```java
String name = "joke";                       // reported
for (int i = 0; i < 3; i++) { }             // reported
for (String s : names) { }                  // reported
try (InputStream in = open()) { }           // reported
```

Not reported, because `var` cannot express them:

```java
int uninitialized;                          // var requires an initializer
String nothing = null;                      // null has no type to infer
int[] shorthand = {1, 2};                   // var forbids the array-initializer form
Runnable task = () -> { };                  // a lambda needs a target type
Supplier<String> make = String::new;        // so does a method reference
int a = 1, b = 2;                           // var forbids multiple declarators
```

Fields and method, constructor, catch and lambda parameters are out of scope entirely.

The rule declares `minimumLanguageVersion="10"`, so PMD skips it for source analysed at an earlier
language version rather than reporting violations you cannot act on.

### StaticMethodsModifyStaticState

Reports a `static` method that neither writes private static state, belongs to a utility class, nor
is a named constructor.

This is about what the modifier tells a reader, not about mocking — Mockito's inline mock maker and
Spock's `SpyStatic` both mock statics, so a mockability argument would simply be false. Left
unconstrained, `static` means any of five things: helper, factory, constant accessor, entry point,
class-state mutator. It therefore means nothing. Under this rule it means exactly one thing, and
seeing it tells you to go find the field.

```java
static String format(int n) { return "" + n; }   // reported: a helper, not state
static int get() { return count; }               // reported: a read is not a write
static void register(String k, String v) {       // reported: nothing can tell put from size
    REGISTRY.put(k, v);
}

static void bump() { count++; }                  // not reported: writes private static state
static void reset() { count = 0; }               // not reported: so does this
static Example of(int x) { return new Example(); }  // not reported: a named constructor
public static void main(String[] args) { }       // not reported: the JVM requires static
```

A write is an assignment, a compound assignment, an increment or a decrement to a **private static**
field. It follows that a `private static final` field can never justify a static method, because a
final field is only ever assigned in its initializer — a registry built on one must either become a
utility class or move onto an injected instance.

A utility class is exempt: one declaring no instance methods and no public or protected constructor.
A class declaring no constructor is judged on its implicit one, which takes the class's own access,
so a `public class` with no declared constructor is *not* a utility class — which agrees with PMD's
stock `UseUtilityClass`. An interface has no constructor at all, and an enum's implicit constructor
is always private.

A type carrying an annotation named `UtilityClass` is exempt without the structural test. Lombok's
`@UtilityClass` privatises the constructor and makes every member static during annotation
processing, so the source PMD reads declares instance-looking methods and no constructor at all,
which fails both halves of the structural test on a type that is a utility class once compiled. The
annotation is matched by **simple name**, so this adds no dependency on Lombok — a project without it
simply never matches the name.

A **named constructor** is exempt: a `static` method whose declared return type is its own declaring
type, or an interface that type *directly declares* it implements. A test double over such a factory
could only return what the constructor it wraps already returns, so there is nothing to intercept and
no seam is lost — it is not a helper hiding behind `static`, it is the constructor, named.

```java
public class Cost implements Comparable<Cost> {
    static Cost finite(int amount) { … }         // not reported: returns its own type
    static Comparable<Cost> comparable(int a) { … }  // not reported: a directly declared interface

    static Number total(int a) { … }             // reported: a factory for something else
    static Cost[] all() { … }                    // reported: an array is not the constructor
    static void configure(int a) { … }           // reported: void names no type
}
```

A superclass return type, or an interface inherited transitively rather than declared here, is still
reported. The comparison is on **simple names**, with no type resolution, for the same reason
`UseVisibleForTestingAnnotation` matches names: resolution needs an `auxclasspath` consumers
frequently do not configure, and a misconfigured one would make the rule silently pass. The cost is
that a factory returning a same-named type from another package is exempted too — a missed report
review can catch, which beats silent under-reporting nobody can see.

`main(String[])` and `@BeforeAll`/`@AfterAll` methods are exempt because the platform forces them to
be static and an unfixable violation is worse than a missed one. **A `@MethodSource` provider and a
Spring `static @Bean` are not exempt** — the annotation names the provider in a string, so nothing
can pick it out from any other static method. `@SuppressWarnings("PMD.StaticMethodsModifyStaticState")`
is the expected response there, not a sign that something has gone wrong.

### AvoidPrivateAndProtectedMethods

Reports a method declared `private`, or declared `protected` without a marker stating why. The legal
visibilities are `public`, package-private, and a `protected` whose intent is declared.

A `private` method cannot be reached from a test, so it is only ever exercised through whatever
public method calls it, and it cannot be stubbed when that caller is the thing you meant to test.
No annotation changes that, so **no marker excuses a `private` method**.

`protected` is reachable but is usually the wrong seam: it widens the API to every subclass in every
consumer's codebase, where package-private widens it only to the package the test lives in. So
package-private is the default internal form and `protected` is a stated exception, permitted when
the method carries one of two markers:

| Marker | Meaning |
|---|---|
| `@ApiStatus.OverrideOnly` | an extension point implementors override |
| `@VisibleForTesting` | a visibility widened to create a test seam |

```java
private boolean check() { return true; }      // reported
protected boolean verify() { return true; }   // reported: unmarked

boolean inspect() { return true; }            // not reported: package-private
public boolean isValid() { return true; }     // not reported: public API
private Example() { }                         // not reported: constructors are out of scope

@Override
protected void hook() { }                     // not reported: visibility is not chosen here

@ApiStatus.OverrideOnly
protected void extend() { }                   // not reported: a declared extension point

@VisibleForTesting
protected boolean seam() { return true; }     // not reported: a declared test seam

@VisibleForTesting
private boolean hidden() { return true; }     // reported: no marker makes private reachable
```

The markers are matched by **simple name**, so both `@OverrideOnly` imported directly and
`@ApiStatus.OverrideOnly` qualified through its outer type are recognised. The set is hardcoded
rather than exposed as a rule property: letting each project choose which annotations legitimise
`protected` reintroduces exactly the per-project drift the rule exists to prevent.

**This does not soften the rule.** The count of *undeclared* legal forms is still zero — an unmarked
`protected` is reported exactly as before. What a marker hands back is a choice between two
*documented* intents, not a choice about whether to declare one, and the rule's purpose of removing
discretion survives intact.

The markers are necessary because package-private is not always a compliant rewrite. A `protected`
member on a published abstract base whose subclasses live in other packages and other modules is
unreachable if narrowed, so the rule without them demanded a rewrite that does not compile.

Constructors are out of scope — you do not spy a constructor, and a private one is required by
`StaticMethodsModifyStaticState`'s utility-class exception. `@Override` methods are exempt, because
Java forbids narrowing an inherited visibility.

For anything the markers do not cover, suppress it:

```java
@SuppressWarnings("PMD.AvoidPrivateAndProtectedMethods")
protected void extensionPoint() { }
```

Reporting `protected` only when no subclass actually uses it would be better, and is not possible:
PMD analyses one compilation unit at a time, rules are copied per thread, and every violation must
be attached to a live parsed node. "Does any subclass exist" is a whole-module question. The markers
exist so the question never has to be asked — a declaration is readable from a single file, which
makes the check correct even across a module boundary and into a consumer's subclass. The one case
provable in a single file — `protected` in a `final` class — is already covered by PMD's stock
`AvoidProtectedMethodInFinalClassNotExtending`, which stays correct where it fires: nothing can
override in a `final` class, and no out-of-package subclass can exist there for a seam to reach.

### UseVisibleForTestingAnnotation

Reports a package-private method that does not carry `@VisibleForTesting`.

`AvoidPrivateAndProtectedMethods` makes package-private the canonical form for an internal method.
This rule makes the widened visibility read as a deliberate test seam rather than a forgotten
modifier — which is the only reason the wider visibility was acceptable in the first place.

```java
boolean check() { return true; }              // reported: an unmarked seam

@VisibleForTesting
boolean verify() { return true; }             // not reported

public boolean isValid() { return true; }     // not reported: not package-private

@Test
void reportsTheViolation() { }                // not reported: a JUnit test method
```

**The annotation is matched by simple name**, so any declaration works — JetBrains, Guava, AndroidX
and Elastic all ship a `VisibleForTesting` and all of them are markers. You do not need the one this
project happens to use, and there is no fully-qualified-name list to keep in sync. Matching the name
also avoids PMD's type resolution, which needs an `auxclasspath` that consumers frequently do not
configure and which would make the rule silently pass when misconfigured.

Only methods are in scope; fields, constructors and nested classes are not. `@Override` methods are
exempt, and so are JUnit 5 test and lifecycle methods, which are conventionally package-private and
for which the annotation would be nonsense.

Note that a package-private method is stubbable only from a test in the **same package and the same
classloader**. That holds for a standard Gradle layout and fails under JPMS with a sealed module.

### AvoidLambdaBlockBodies

Reports a lambda whose body is a block. Logic belongs in something with a name — a lambda block body
is anonymous by construction, so no test can call it, no caller can stub it, and its branches are
reachable only through the pipeline that encloses it.

**A method reference is not required.** This is the part to read twice, because the intuitive reading
would make the rule close to unusable: a lambda that closes over a local variable cannot become a
method reference at all. Any non-block body satisfies the rule, so such a lambda stays a lambda and
simply delegates:

```java
items.forEach(item -> {                     // reported
    validate(item, context);
    save(item, context);
});

items.forEach(item -> process(item, context));   // fine — still a lambda, just not a block
items.forEach(this::save);                       // fine — no body at all

map(x -> { return x + 1; });                // reported: converts to an expression
map(x -> { save(x); });                     // reported: so does this
map(x -> x + 1);                            // fine
Runnable task = () -> { };                  // fine: nothing to extract
```

An empty block is exempt — `() -> { }` has nothing to extract, and a violation nobody can act on is
worse than one that is missed. A block containing only a comment is exempt for the same reason: a
comment is not a statement.

Logic inside an *expression* body is deliberately not reported:

```java
map(x -> x > 0 ? positive(x) : negative(x));                  // not reported — two branches
map(x -> x.getA().getB().stream().filter(…).count());         // not reported — a long chain
```

Block-versus-expression is a syntactic proxy for "logic hiding in an anonymous place". It is a good
proxy and a cheap one to determine, and it is the start rather than the whole answer.

The rule declares `minimumLanguageVersion="8"`, since lambdas do not exist before Java 8.

#### The one conflict: a block lambda in a static field initializer

Extracting it produces a method that must be `static` — a static initializer calls it — and
`StaticMethodsModifyStaticState` then reports that method. No form satisfies both rules, so
`@SuppressWarnings` is available:

```java
@SuppressWarnings("PMD.StaticMethodsModifyStaticState")
static String makeGreeting() { … }
```

**Usually there is a better fix: drop `static` from the field.** The shape that produces these in
bulk is the static dispatch table, where every handler is forced static and every one is a violation:

```java
// one suppression per handler, and another on every handler added
private static final Map<String, Handler> HANDLERS = Map.of(
        "create", Example::handleCreate,
        "delete", Example::handleDelete);

// no suppressions at all — the handlers are instance methods now
private final Map<String, Handler> handlers = Map.of(
        "create", this::handleCreate,
        "delete", this::handleDelete);
```

### AvoidAnonymousClasses

Reports an anonymous class whose body is not empty. Its logic has no name: nothing can instantiate
it, nothing can stub it. It is the less testable of the two anonymous forms — unlike a lambda it can
declare several methods and carry its own fields.

```java
return new Runnable() {                     // reported
    @Override
    public void run() { doTheWork(); }
};

return new Worker();                        // fine — a named class
```

**The two rules ship together because each is the other's bypass.** A ban on lambda block bodies
alone is escaped in one edit by rewriting the lambda as an anonymous class, and every other rule here
waves that through: it is not static, its method is `public` and `@Override`, and it is not a lambda.
The escape would land you on the *less* testable construct.

Two exemptions, both because the alternative is a violation nobody can act on:

```java
new TypeToken<List<String>>() { }           // not reported: the empty body IS the mechanism

enum Op {
    PLUS { int apply(int a, int b) { return a + b; } },   // not reported: an enum constant body
    MINUS { int apply(int a, int b) { return a - b; } };
}
```

PMD models an enum constant body as an anonymous class, so without that exemption the rule would
report every strategy enum — which has no anonymous-free rewrite that keeps the enum. An anonymous
class declared inside a method *of* an enum is still reported; only the constant's own body is
exempt.

### UseStaticImports

Reports a static member — method or field — reached through its declaring type. The import carries
the owner; the code should not repeat it on every use.

> **If you enable `category/java/codestyle.xml`, you must exclude `TooManyStaticImports`.** It caps
> static imports at four by default, and this rule only ever adds them. No configuration satisfies
> both. This is the one rule here that fights a stock rule rather than composing with it.
>
> ```xml
> <rule ref="category/java/codestyle.xml">
>     <exclude name="TooManyStaticImports"/>
> </rule>
> ```

> **Error Prone's `BadImport` needs nothing from you.** It rejects static imports of a handful of
> names this rule would otherwise demand, `copyOf` among them, which would leave you with one report
> asking for the import and one refusing it. `copyOf` is excluded here for that reason — and because
> it is uninformative anyway — so the two tools agree without a suppression on either side.

**The threshold is a floor, not a ceiling.** The rule only ever says "import this". It never reports
an import as unnecessary and never stops you importing a shorter name by hand, so anything below the
floor is simply your call, and a report you disagree with is a `@SuppressWarnings` away.

```java
Mockito.doReturn(true)             // reported: 8 characters
Collections.unmodifiableList(xs)   // reported: self-describing, so not excluded
Collectors.toList()                // reported
AccessType.WRITE                   // reported: fields count too

Math.max(a, b)                     // not reported: 3 characters, under the floor
Optional.empty()                   // not reported: empty what?
Duration.ofSeconds(3)              // not reported: of[A-Z] prefix
Registry.INSTANCE                  // not reported: instance of what?
List.copyOf(xs)                    // not reported: a copy of what, into what?
Example.class.getName()            // not reported: a class literal cannot be imported
```

Short names are left alone because the short static members of the JDK are overwhelmingly the
ambiguous ones — `of`, `get`, `min`, `max`, `now`, `abs`, `sum`.

**Ambiguity is handled structurally, not by the exclusion list.** If a file uses both
`Arrays.toString` and `Objects.toString`, neither is reported and you may import one, the other, or
neither. If it uses only one, the bare name is unambiguous *in that file* and the import line names
the owner. A name already bound in the file by a method, field, parameter or local variable is left
alone too, since the import would be shadowed.

The exclusion list is therefore only about *uninformative* names — factory-shaped members where the
member name says what it produces but not of what:

```
exact:   value  values  valueOf  from  empty  create  builder  parse
         now  between  copyOf  getInstance  newInstance  INSTANCE
prefix:  of…  from…   at a camelCase boundary, so ofSeconds is excluded and offer is not
```

Self-describing members such as `unmodifiableList`, `toList` and `groupingBy` are deliberately
absent, and are reported. The list is fixed — suppress at the site for your own factory methods.

Violations are reported **once per member per file**, not once per occurrence, because one import
fixes them all. A first run over an existing codebase therefore reports roughly the number of import
lines you need to add.

### UseTypeImports

Reports a fully-qualified type name used in code where an import would let the simple name stand.

This does **not** overlap with PMD's stock `UnnecessaryFullyQualifiedName`, which sounds like it
covers the same ground and does not. That rule fires only when the simple name is *already* in
scope, and its fix is to drop the qualifier:

```
simple name already in scope   →  UnnecessaryFullyQualifiedName (stock)   "drop the qualifier"
simple name not yet in scope   →  UseTypeImports (this artifact)          "add an import"
```

The two partition the space, so enabling both never produces two reports for one name.

```java
private java.util.List<String> names;      // reported: add an import
private java.time.Duration timeout;        // reported

private List<String> names;                // not reported: imported
java.lang.String name = "joke";            // not reported: java.lang, the stock rule's job
private com.example.Helper helper;         // not reported: same package
```

If two types in one file want the same simple name, only one can be imported — the rule reports
neither and leaves the choice to you. A qualified nested type such as `java.util.Map.Entry` is
reported without prescribing which of the two valid fixes to apply. Reported once per name per file.

Note that when PMD cannot resolve a qualifier, both import rules stay silent rather than guess, so a
clean run is not by itself proof of compliance.

### The rules cascade

They are designed to fire one at a time rather than all at once, so each violation has a single
obvious fix. The full chain spans this ruleset and PMD's own:

```java
items.forEach(item -> { validate(item); save(item); });  // AvoidLambdaBlockBodies: extract the body
items.forEach(item -> process(item));                    // LambdaCanBeMethodReference (PMD stock)
items.forEach(this::process);                            // ↓ now the method itself
private static void process(…)                           // StaticMethodsModifyStaticState: drop static
private void process(…)                                  // AvoidPrivateAndProtectedMethods: widen
void process(…)                                          // UseVisibleForTestingAnnotation: annotate

@VisibleForTesting
void process(final Item item) { … }                      // clean
```

`LambdaCanBeMethodReference` is PMD's own rule, in `category/java/codestyle.xml` — this artifact
ships nothing that references it, but the two compose if you enable that category.

`AvoidPrivateAndProtectedMethods` deliberately skips `static` methods so that
`StaticMethodsModifyStaticState` reports them first, and `AvoidLambdaBlockBodies` stops at the block
body rather than also demanding a method reference. Expect several build runs when adopting these
rules on an existing codebase — each one surfaces the next step, and each step is mechanical.

Not every `static` method enters the chain. A **named constructor** — a `static` method returning its
own declaring type or a directly declared interface — is exempt, so it exits at the first step rather
than being reported by it and pushed toward an instance method it was never meant to become:

```java
static Cost finite(int amount) { … }                     // exits here: a named constructor
private static void process(…)                           // continues down the chain
```

`UseStaticImports` and `UseTypeImports` sit outside that chain: they report independently and each
violation is one import line.

## CodeNarc rules

Every rule below is gated on the class being a Spock specification, and reports nothing outside one.
See [The specification gate](#the-specification-gate) for what a consumer with a different base class
sets.

**Expect volume on a codebase that has not adopted strict mocking.** The fixture rules fire on a
declaration here and there. The four interaction rules judge a whole block, and
`RequireStrictMockingTerminator` alone reports once per `then:` block that does not end with `0 * _` —
which on a project that never adopted strict mocking is every `then:` block it has. A first run
producing hundreds of violations is the adoption cost of these conventions, not a broken ruleset.

Every rule is individually selectable and individually excludable. To take the artifact without one:

```groovy
ruleset {
    ruleset('rulesets/groovy/joke.groovy') {
        exclude 'RequireStrictMockingTerminator'
    }
}
```

### AvoidSetupAndGivenLabels

Reports a `setup:` or `given:` statement label inside a feature method.

Spock treats unlabelled statements at the top of a feature method as the implicit setup block, so the
label states what the statement's position already says. Removed, the blank line before `when:`
carries the boundary.

```groovy
class ExampleSpec extends Specification {

    def 'charges the order total'() {
        given:                                     // violation
        PaymentGateway gateway = Mock()

        when:
        service.checkout(order)

        then:
        1 * gateway.charge(49.99G)
        0 * _
    }

    def 'refunds the order total'() {
        PaymentGateway gateway = Mock()            // no violation: position says it

        when:
        service.refund(order)

        then:
        1 * gateway.refund(49.99G)
        0 * _
    }
}
```

`when:`, `then:`, `expect:`, `where:`, `cleanup:`, `and:`, `filter:` and `combined:` are untouched.
`and:` is a continuation with no semantic weight — CodeNarc's own label vocabulary excludes it for
that reason — and `cleanup:` and `where:` have no unlabelled equivalent, so reporting them would
demand a rewrite that does not exist.

A `def setup()` or `def setupSpec()` fixture *method* is not reported. The rule reports only inside a
method Spock would treat as a feature method, which is one carrying at least one statement label —
the same definition Spock's own `SpecParser` uses. That is what keeps the rule off the fixture
methods whose name it shares.

### DeclareMockWithExplicitType

Reports a dynamically-typed declaration whose initialiser is a call to `Mock`, `Stub` or `Spy`.

**The rule is about the declaration, not about `Mock(Type)`.** An inline `Mock(Type)` passed straight
to a constructor or a method is the documented way to supply a collaborator the specification never
refers to again, and is never reported — there is no variable whose type could have been written.

```groovy
def service = new CheckoutService(Mock(PaymentGateway))  // no violation: declares no collaborator
service.register(Mock(Listener))                         // no violation

def repository = Mock(CustomerRepository)                // violation
CustomerRepository repository = Mock()                   // no violation

def service = Spy(OrderService, constructorArgs: [repo]) // violation
OrderService service = Spy(constructorArgs: [repo])      // no violation
```

The declared type is what a reader looks at to learn who the subject collaborates with. Moved into
the initialiser it is still present but no longer in the position that answers the question, and the
variable itself is untyped for every later line that uses it.

Fields are reported as well as local variables — a local is a declaration expression and a field is a
field node with an initial expression, which are different nodes reached by different visits.
Declaring collaborators as fields is the more common Spock form, so covering only locals would miss
most of what the rule is for.

### AvoidMockInitializerClosure

Reports a `Mock`, `Stub` or `Spy` call whose last argument is a closure.

```groovy
CustomerRepository repository = Mock() {       // violation
    findById(_) >> customer
}

CustomerRepository repository = Mock()         // no violation
...
then:
1 * repository.findById('cust-1') >> customer
0 * _
```

An interaction declared in the initialiser sits away from the `when:` it answers and reads as
configuration rather than as verification. It also loses its cardinality: the initialiser form stubs
a return without asserting that the call happened, which is the half of the contract worth having.

`Spy(constructorArgs: [repository])` passes a map rather than a closure and is not reported —
reporting it would leave no way to declare a spy over a real object.

`def repository = Mock(Repo) { … }` violates this rule *and* `DeclareMockWithExplicitType`, and is
reported by both. Each rule is independently selectable, so each has to be correct on its own;
suppressing one report because another rule also fires would leave a hole for whoever adopted only
one of the two.

### InteractionsBelongInThenBlock

Reports a Spock interaction anywhere in a feature method other than a `then:` block.

```groovy
def 'charges the order total'() {
    repository.findById('cust-1') >> customer      // violation: reads as configuration

    when:
    def receipt = service.checkout(order)

    then:
    1 * gateway.charge(49.99G) >> receipt          // no violation
    0 * _
}
```

An interaction declares what the subject must call. Placed above `when:` it reads as configuration
rather than verification and sits away from the action it answers; placed in `expect:` it mixes the
collaboration contract into the value assertions.

An interaction is recognised by its shape: a cardinality (`1 * mock.foo()`, `(1..3) * mock.foo()`,
`_ * mock.foo()`), a stubbed return (`mock.foo() >> value`, `mock.foo() >>> [a, b]`), the two combined,
or an `interaction { … }` block. Nothing asks whether the target is a mock, a stub, a spy or a real
object — CodeNarc analyses source without a compile classpath, and the shape is what distinguishes an
interaction in the first place. `service.warmUp()` and `def total = 2 * price` are not interactions.

`and:` continues the block it follows rather than starting one, matching CodeNarc's own label
vocabulary, which excludes `and:` because it carries no semantic weight. So an interaction in an
`and:` after `then:` is compliant, and the same `and:` after `when:` is not.

### RequireStrictMockingTerminator

Reports a `then:` block whose last statement is not `0 * _`.

```groovy
then:
1 * gateway.charge(49.99G) >> receipt
0 * _                                              // no violation
```

`0 * _` asserts that no interaction other than the declared ones happened on any double. Without it a
specification silently tolerates an extra call, which is the regression strict mocking exists to
catch: an unplanned collaborator call should break a test until someone declares it on purpose.

**The terminator is required whether or not the feature method declares a `Mock`, `Stub` or `Spy`.**
Requiring it only when a double is in scope was rejected on the failing case rather than the common
one: a specification with no collaborators today acquires one the moment the subject grows a
dependency, and a conditional rule goes quiet exactly then — the `then:` block that was compliant
yesterday is unterminated and nothing reports it.

So a `then:` block containing nothing but the terminator is the intended shape, not an artefact. This
repository's own `AvoidUnrollAnnotationRuleSpec` is the worked example:

```groovy
def 'the name and priority are settable, as CodeNarc ruleset configuration requires'() {
    when:
    rule.name = 'Renamed'
    rule.priority = 3

    then:
    0 * _

    expect:
    rule.name == 'Renamed'
    rule.priority == 3
}
```

A `then:` and every `and:` following it are one run, terminated once at the end rather than once per
`and:` — a terminator per `and:` would assert "nothing else happened" in the middle of a list still
being declared. Only `then:` is asked for one: a feature method with no collaborators and a standalone
`expect:` is a documented shape, and demanding a `then:` block there would demand a block with nothing
to verify.

### ValueAssertionsBelongInExpectBlock

Reports a boolean expression in a `then:` block.

```groovy
then:
1 * gateway.charge(49.99G) >> receipt
receipt.total == 49.99G                            // violation
0 * _

expect:
receipt.total == 49.99G                            // no violation
```

`then:` states who the subject called; `expect:` states what it returned. A `then:` block full of `==`
buries the interaction contract in the middle of value checks, and the contract is the half a reader
most needs to find.

An interaction, a `thrown(…)` or `notThrown(…)` call, and a `def error = thrown(…)` capture all stay
where they are. The exception's *message* does not — `error.message == '…'` is a value assertion like
any other.

**Known gap: a bare truthiness check is not reported.** `receipt.valid` in a `then:` block stays
silent, while `receipt.isValid()` is reported. What counts as a value assertion is CodeNarc's own
boolean-expression test, which recognises comparison operators and a set of method-name patterns. The
alternative — report anything that is not an interaction and not `thrown` — would report an ordinary
helper call in a `then:` block, a false positive on code that is not wrong. Sharing CodeNarc's
judgement also means this rule and stock `SpockMissingAssert` never disagree about the same line. The
rule under-reports rather than guesses, and widening the definition later is a change with its own
evidence.

### RequireValidatedInteractionArguments

Reports an interaction argument that places no constraint on the value passed.

```groovy
1 * repository.save(_)                             // violation
1 * repository.save(_ as Customer)                 // violation
1 * repository.save(*_)                            // violation
1 * repository.save({ true })                      // violation

1 * repository.save(expectedCustomer)              // no violation
1 * repository.save({ it.id == 'cust-1' })         // no violation
```

A mocked interaction is a contract about what the subject passes its collaborator. An argument that
matches anything lets a wrong value through and leaves the contract asserting only that a call
happened, which is the weaker half.

`_ as Type` is reported too. It carries more information than a bare `_`, which makes it the closest
call in the set, but it still accepts every instance of that type and the rule's subject is whether
the *argument* was the right one.

For the closure form the parameter list is irrelevant, so only the body is read: `{ true }`,
`{ _ -> true }` and `{ it -> true }` differ in a parameter the body never uses. A body of one
statement whose expression is a constant truthy under Groovy truth is reported, so `{ 1 }` is a
violation and `{ 0 }` is not.

**Only argument positions are examined.** `_` in target position is required by this rule's own
family — `0 * _` by `RequireStrictMockingTerminator` — so `0 * _`, `1 * service._` and
`1 * repository._(expectedCustomer)` are all silent.

The rule applies to every interaction form on every kind of double, and reports wherever the
interaction appears — including a field initialiser and a `Mock() { … }` closure — so an interaction
moved out of a `then:` block to escape a sibling rule is still checked here.

**Known gap: a closure that is truthy without being a bare constant is not reported.** `{ it }` is
truthy for anything non-null and non-empty and constrains nothing, and `{ return true }` is the same
closure written with a statement rather than an expression. Neither is reported: detecting the first
would begin a general truthiness analysis this rule declines to start, and the second is the same
check applied to a body shape the rule does not read. Both under-report rather than guess.

### The interaction rules overlap on purpose

`Mock() { findById(_) >> row }` violates `AvoidMockInitializerClosure` and
`RequireValidatedInteractionArguments`, and both fire. An interaction in the setup region passing `_`
violates `InteractionsBelongInThenBlock` and `RequireValidatedInteractionArguments`, and both fire. A
`then:` block full of `==` with no terminator is reported by `ValueAssertionsBelongInExpectBlock` once
per assertion and by `RequireStrictMockingTerminator` once for the block.

Suppressing the second report because the first already condemned the line was considered and
rejected. Every rule here is individually selectable, so a consumer may have adopted only one of the
pair — and a rule that defers to a sibling would leave that consumer's ruleset silently missing the
violation it selected a rule to catch. Two reports on one line is the lesser cost, and one edit
removes both.

### The specification gate

Every rule in this artifact reports only inside a class it recognises as a Spock specification, and
each exposes the two properties CodeNarc's own Spock rules expose:

| property | default | matched against |
|---|---|---|
| `specificationSuperclassNames` | `*Specification` | the superclass name as written |
| `specificationClassNames` | unset | the class name |

The rules match identifiers — `Mock`, `Stub`, `Spy`, and Spock's block labels — that are legal
elsewhere in Groovy, and `DeclareMockWithExplicitType` is the one for which a collision outside a
specification is realistic rather than theoretical. The gate is what keeps them off production code.

If your specifications extend a base class of your own, name it:

```groovy
ruleset {
    ruleset('rulesets/groovy/joke.groovy') {
        'DeclareMockWithExplicitType' {
            specificationSuperclassNames = '*Specification,*IntegrationBase'
        }
    }
}
```

Both properties accept a comma-separated list and the `*` and `?` wildcards. A specification whose
superclass matches neither is not analysed by any rule here.

### AvoidUnrollAnnotation

Reports `@Unroll` on a specification class or a feature method.

Spock 2 unrolls every data-driven feature by default, so the annotation changes nothing. Left in
place it reads as though it were switching a behaviour on, which sends a reader looking for the
un-annotated features that supposedly behave differently.

```groovy
class ExampleSpec extends Specification {

    @Unroll                                    // violation
    def 'adds #a and #b'() {
        expect:
        a + b == sum

        where:
        a | b || sum
        1 | 2 || 3
    }

    def 'adds #a and #b'() {                   // no violation: already unrolled
        expect:
        a + b == sum

        where:
        a | b || sum
        1 | 2 || 3
    }
}
```

The annotation is matched on the name as written — both `@Unroll` and `@spock.lang.Unroll` — rather
than resolved to a type. CodeNarc analyses source without a compile classpath, so a rule that
resolved the annotation would report nothing whenever that classpath was incomplete. The cost is
that a deliberately misleading `Unroll` from another package is reported too, which is the cheaper
failure.

The remaining house Spock conventions land in small thematic changes.

## Build

```
./gradlew check
```

Runs the unit tests, the integration tests at each tool's compile floor, Spotless, PMD, CodeNarc,
Error Prone with NullAway, and mutation testing at 100% mutation, coverage and test strength.

Groovy source is deliberately **not** formatted: Spotless's Groovy support is `greclipse`, which
reformats Spock's labelled-block layout badly enough to fight the specifications it would be tidying.
CodeNarc carries Groovy style instead.

### This project runs its own rules on itself

Both artifacts analyse this repository with the artifacts this repository builds, through the same
published resources a consumer references, resolved off the analysis classpath the same way:

| module | declares | task | analyses |
|---|---|---|---|
| `pmd-rules` | `pmd project(':pmd-rules')` | `pmdMain`, `pmdTest` | its own Java rule classes |
| `codenarc-rules` | `pmd project(':pmd-rules')` | `pmdMain` | its Java rule classes |
| `codenarc-rules` | `codenarc project(':codenarc-rules')` | `codenarcTest` | its own Spock specifications |

There is no ruleset file in this repository; both compositions under test are the shipped ones. A new
rule therefore has to leave this repository clean as part of the change that adds it.

The CodeNarc rules are Spock-focused and this module's tests *are* Spock specifications, which is
what makes that last row real dogfooding rather than a unit test wearing a costume.

The consequence is that **a broken rule breaks the build that produces it**, and the repair is to
edit the rule that is currently failing. To build past it:

```
./gradlew check -x pmdMain -x pmdTest -x codenarcTest
```

### How the supported PMD range is covered

Two signals, at the two ends of the range:

| signal | PMD version | comes from | ruleset | code analysed |
|---|---|---|---|---|
| `integrationTest` | 7.0.0, the compile floor | the `dependencies` platform | both shipped rulesets | synthetic fixtures |
| `pmdMain` / `pmdTest` | 7.26.0 | the `pmd-dist` coordinate in the convention plugin | `joke-strict.xml` | this repository's real source |

A rule compiled against API absent from the floor fails `integrationTest`. A rule or ruleset broken
by a newer PMD fails `pmdMain`, against real source rather than fixtures. Versions between the two
ends are not exercised, which is a deliberate trade: a compile-floor break shows at the floor and an
API removal shows at the ceiling, so the interior was carrying no weight.

`pmdMain` therefore says nothing about the 7.0.0 floor on its own — `integrationTest` owns that — and
the `pmd-dist` version must stay at or above the floor `rulesets/java/joke-strict.xml` declares,
since `pmdMain` resolves that ruleset.

**Adopting a newer PMD** is one coordinate: raise `net.sourceforge.pmd:pmd-dist` in
`buildSrc/src/main/groovy/conventions.gradle` and run `./gradlew check`. There is no version list to
extend and no guard to satisfy.

### Rule test data stays in XML

The `pmd-test` descriptors under `pmd-rules/src/test/resources` deliberately contain violating code, and
the examples in `category/java/joke.xml` do too. Both are invisible to `pmdMain` and `pmdTest` only
because they are XML. Do not move rule fixtures into `.java` files — the build would flag its own
test data, and excluding the fixture path to fix it would silently exclude whatever moved there
next.

## License

[Apache License 2.0](LICENSE)
