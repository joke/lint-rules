package io.github.joke.lint.pmd.rules.java

import spock.lang.Specification
import spock.lang.Tag

import static io.github.joke.lint.pmd.rules.java.PmdRuleRunner.violationLines

@Tag('unit')
class AvoidLambdaBlockBodiesSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        violationLines('AvoidLambdaBlockBodies', fixture, version) == lines

        where:
        fixture                                                                   | version | lines
        'AMultiStatementBlockBodyIsReported'                                      | null  | [3]
        'ASingleStatementBlockBodyIsReportedItConvertsMechanicallyToAnExpression' | null  | [3]
        'ABlockBodyReturningAValueIsReported'                                     | null  | [3]
        'AnExpressionBodyIsNotReported'                                           | null  | []
        'AMethodReferenceIsNotReportedItHasNoBodyAtAll'                           | null  | []
        'AnExpressionLambdaClosingOverALocalIsNotReported'                        | null  | []
        'ABareDelegationIsLeftToPMDSLambdaCanBeMethodReference'                   | null  | []
        'AnEmptyBlockBodyIsNotReportedThereIsNothingToExtract'                    | null  | []
        'AnEmptyBlockBodyWithExplicitParametersIsNotReported'                     | null  | []
        'ABlockContainingOnlyACommentIsNotReportedACommentIsNotAStatement'        | null  | []
        'ABranchingExpressionBodyIsNotReportedExpressionBodiesAreOutOfScope'      | null  | []
        'ALongChainInAnExpressionBodyIsNotReported'                               | null  | []
        'ANestedBlockLambdaIsReported'                                            | null  | [3]
        'SuppressionIsAvailableAtTheSite'                                         | null  | []
        'SeveralBlockLambdasInOneMethodAreEachReported'                           | null  | [3, 6]
        'TheRuleIsSkippedBelowJava8WhereLambdasDoNotExist'                        | '1.7' | []
        'TheRuleAppliesFromJava8'                                                 | '8'   | [3]
    }
}
