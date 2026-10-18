package io.github.joke.lint.pmd.rules.java

import spock.lang.Specification
import spock.lang.Tag

import static io.github.joke.lint.pmd.rules.java.PmdRuleRunner.violationLines

@Tag('unit')
class UseVarForLocalVariablesSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        violationLines('UseVarForLocalVariables', fixture, version) == lines

        where:
        fixture                                                               | version | lines
        'AnExplicitlyTypedLocalVariableIsReported'                            | null  | [3]
        'AVarDeclaredLocalVariableIsNotReported'                              | null  | []
        'ABasicForLoopInitializerIsReported'                                  | null  | [3]
        'AVarDeclaredBasicForLoopInitializerIsNotReported'                    | null  | []
        'AnEnhancedForLoopVariableIsReported'                                 | null  | [3]
        'AVarDeclaredEnhancedForLoopVariableIsNotReported'                    | null  | []
        'ATryWithResourcesDeclarationIsReported'                              | null  | [3]
        'ADeclarationWithNoInitializerIsNotReportedVarRequiresOne'            | null  | []
        'ANullInitializedDeclarationIsNotReportedNullHasNoTypeToInfer'        | null  | []
        'AnArrayInitializerShorthandIsNotReportedVarForbidsTheForm'           | null  | []
        'AnExplicitArrayAllocationIsReportedItIsNotTheShorthandForm'          | null  | [3]
        'ALambdaInitializerIsNotReportedALambdaNeedsATargetType'              | null  | []
        'AMethodReferenceInitializerIsNotReportedItNeedsATargetTypeToo'       | null  | []
        'AMultiVariableDeclarationIsNotReportedVarForbidsMultipleDeclarators' | null  | []
        'AFieldIsNotReported'                                                 | null  | []
        'MethodAndConstructorParametersAreNotReported'                        | null  | []
        'ACatchParameterIsNotReported'                                        | null  | []
        'AnExplicitlyTypedLambdaParameterIsNotReported'                       | null  | []
        'SeveralDeclarationsInOneMethodAreEachReported'                       | null  | [3, 4]
        'TheRuleIsSkippedBelowJava10WhereVarDoesNotExist'                     | '1.8' | []
        'TheRuleAppliesFromJava10'                                            | '10'  | [3]
    }
}
