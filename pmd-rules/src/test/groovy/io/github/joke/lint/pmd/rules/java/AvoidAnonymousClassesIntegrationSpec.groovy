package io.github.joke.lint.pmd.rules.java

import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Tag

import static io.github.joke.lint.pmd.rules.java.PmdRuleRunner.violationLines

@Tag('integration')
@Subject(AvoidAnonymousClasses)
class AvoidAnonymousClassesIntegrationSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        violationLines('AvoidAnonymousClasses', fixture, version) == lines

        where:
        fixture                                                              | version | lines
        'AnAnonymousClassWithAMethodBodyIsReported'                          | null | [3]
        'AStrategyEnumIsNotReportedPMDModelsAConstantBodyAsAnAnonymousClass' | null | []
    }
}
