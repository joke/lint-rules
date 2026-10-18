package io.github.joke.lint.pmd.rules.java

import spock.lang.Specification
import spock.lang.Tag

import static io.github.joke.lint.pmd.rules.java.PmdRuleRunner.violationLines

@Tag('unit')
class AvoidAnonymousClassesSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        violationLines('AvoidAnonymousClasses', fixture, version) == lines

        where:
        fixture                                                              | version | lines
        'AnAnonymousClassWithAMethodBodyIsReported'                          | null | [3]
        'AnAnonymousClassDeclaringOnlyAFieldIsReported'                      | null | [3]
        'ANamedNestedClassIsNotReported'                                     | null | []
        'ALambdaIsNotReportedAvoidLambdaBlockBodiesOwnsLambdas'              | null | []
        'TheLambdaWorkaroundIsReportedTheTwoRulesCloseEachOtherSBypass'      | null | [3]
        'AnEmptyAnonymousClassBodyIsNotReportedTheBodyIsTheMechanism'        | null | []
        'ATypeCaptureIdiomIsNotReported'                                     | null | []
        'AStrategyEnumIsNotReportedPMDModelsAConstantBodyAsAnAnonymousClass' | null | []
        'AnAnonymousClassInsideAnEnumMethodIsStillReported'                  | null | [5]
        'AnEnumConstantWithArgumentsButNoBodyIsNotReported'                  | null | []
        'SuppressionIsAvailableAtTheSite'                                    | null | []
        'SeveralAnonymousClassesInOneMethodAreEachReported'                  | null | [3, 7]
    }
}
