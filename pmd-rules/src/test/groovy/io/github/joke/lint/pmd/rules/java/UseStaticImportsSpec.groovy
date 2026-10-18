package io.github.joke.lint.pmd.rules.java

import spock.lang.Specification
import spock.lang.Tag

import static io.github.joke.lint.pmd.rules.java.PmdRuleRunner.violationLines

@Tag('unit')
class UseStaticImportsSpec extends Specification {

    def 'the rule reports the expected lines for each case'() {
        expect:
        violationLines('UseStaticImports', fixture, version) == lines

        where:
        fixture                                                             | version | lines
        'ATypeQualifiedStaticMethodIsReported'                              | null  | [5]
        'ATypeQualifiedStaticFieldIsReported'                               | null  | [5]
        'AnInstanceMethodCallIsNotReported'                                 | null  | []
        'AMemberAlreadyUsedThroughAStaticImportIsNotReported'               | null  | []
        'AThreeCharacterMemberIsNotReportedUnderTheFloor'                   | null  | []
        'AFourCharacterMemberIsReported'                                    | null  | [3]
        'AnExcludedExactNameIsNotReported'                                  | null  | []
        'AnExcludedPrefixIsNotReported'                                     | null  | []
        'TheINSTANCEFieldIsNotReported'                                     | null  | []
        'ACamelCaseBoundaryIsRequiredForAPrefixMatchOfferIsNotOfAZ'         | null  | [5]
        'AMemberWhoseOwnerIsDeclaredInTheSameFileIsTreatedAsShadowed'       | null  | []
        'CollectorsMembersAreReportedTheyAreSelfDescribingNotUninformative' | null  | [5]
        'TwoOwnersOfTheSameSimpleNameAreBothLeftAlone'                      | null  | []
        'ASingleOwnerOfThatNameIsReported'                                  | null  | [5]
        'ASoleOwnerOfCopyOfIsNotReported'                                   | null  | []
        'ANameShadowedByALocalDeclarationIsNotReported'                     | null  | []
        'ANameShadowedByAMethodOfTheEnclosingTypeIsNotReported'             | null  | []
        'AClassLiteralIsNotReported'                                        | null  | []
        'RepeatedUseOfOneMemberYieldsOneViolation'                          | null  | [3]
        'TwoMembersOfTheSameTypeYieldTwoViolations'                         | null  | [3, 3]
        'SuppressionIsAvailableAtTheSite'                                   | null  | []
        'TheRuleIsSkippedBelowJava5WhereStaticImportsDoNotExist'            | '1.4' | []
        'TheRuleAppliesFromJava5'                                           | '1.5' | [3]
    }
}
