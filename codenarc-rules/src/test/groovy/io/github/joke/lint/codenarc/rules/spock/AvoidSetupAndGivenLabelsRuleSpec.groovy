package io.github.joke.lint.codenarc.rules.spock

import org.codenarc.rule.Rule
import org.codenarc.source.SourceString
import spock.lang.Specification
import spock.lang.Tag

@Tag('unit')
class AvoidSetupAndGivenLabelsRuleSpec extends Specification {

    Rule rule = new AvoidSetupAndGivenLabelsRule()

    def 'the rule is named and prioritised'() {
        expect:
        rule.name == 'AvoidSetupAndGivenLabels'
        rule.priority == 2
    }

    def 'a setup label is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    setup:
                    def gateway = 1
                    when:
                    def r = gateway
                    then:
                    r
                }
            }
        ''')*.message == ["Spock treats unlabelled statements at the top of a feature method as the setup block, so 'setup:' adds nothing."]
    }

    def 'a given label is reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    given:
                    def gateway = 1
                    when:
                    def r = gateway
                    then:
                    r
                }
            }
        ''')*.message == ["Spock treats unlabelled statements at the top of a feature method as the setup block, so 'given:' adds nothing."]
    }

    def 'unlabelled fixture statements are not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    def gateway = 1
                    def service = gateway

                    when:
                    def r = service
                    then:
                    r
                }
            }
        ''').empty
    }

    def 'both labels in one method are reported separately'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    given:
                    def gateway = 1
                    when:
                    def r = gateway
                    then:
                    r
                    setup:
                    def more = 2
                    expect:
                    more
                }
            }
        ''').size() == 2
    }

    def 'the other Spock labels are not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    when:
                    def r = compute()
                    and:
                    def s = compute()
                    then:
                    r
                    and:
                    s
                    expect:
                    r == s
                    cleanup:
                    close()
                    where:
                    x << [1, 2]
                }
            }
        ''').empty
    }

    def 'the filter and combined labels are not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    filter:
                    x > 0
                    combined:
                    y > 0
                    expect:
                    x == y
                }
            }
        ''').empty
    }

    def 'a non-Spock label is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    expect:
                    outer:
                    for (x in [1]) { break outer }
                }
            }
        ''').empty
    }

    def 'an empty given block followed by when is reported once'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    given:
                    when:
                    def r = compute()
                    then:
                    r
                }
            }
        ''').size() == 1
    }

    def 'a statement carrying the same label twice is reported once'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def 'a feature'() {
                    given:
                    given:
                    when:
                    def r = compute()
                    then:
                    r
                }
            }
        ''').size() == 1
    }

    def 'a setup fixture method is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def setup() {
                    prepare()
                }
            }
        ''').empty
    }

    def 'a setupSpec fixture method is not reported'() {
        expect:
        violationsIn('''
            class ExampleSpec extends Specification {
                def setupSpec() {
                    prepare()
                }
            }
        ''').empty
    }

    def 'an abstract method on a base specification is not reported'() {
        expect:
        violationsIn('''
            abstract class ExampleSpec extends Specification {
                abstract void prepare()
            }
        ''').empty
    }

    def 'a class that is not a specification is not reported'() {
        expect:
        violationsIn('''
            class PlainClass {
                def 'a method'() {
                    given:
                    def gateway = 1
                    when:
                    def r = gateway
                    then:
                    r
                }
            }
        ''').empty
    }

    def 'specificationClassNames widens the gate to a class that extends nothing'() {
        rule.specificationClassNames = 'PlainClass'

        expect:
        violationsIn('''
            class PlainClass {
                def 'a method'() {
                    given:
                    def gateway = 1
                    when:
                    def r = gateway
                    then:
                    r
                }
            }
        ''').size() == 1
    }

    private List violationsIn(String source) {
        rule.applyTo(new SourceString(source))
    }
}
