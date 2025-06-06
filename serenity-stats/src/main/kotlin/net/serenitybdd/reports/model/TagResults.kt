package net.serenitybdd.reports.model

import net.serenitybdd.model.di.ModelInfrastructure
import net.thucydides.model.domain.TestResult
import net.thucydides.model.domain.TestTag
import net.thucydides.model.reports.TestOutcomes
import net.thucydides.model.reports.html.ReportNameProvider
import net.thucydides.model.reports.html.TagExclusions
import net.thucydides.model.reports.html.TagFilter
import org.apache.commons.lang3.StringUtils


class TagResults(val testOutcomes: TestOutcomes) {

    val requirementsService =  ModelInfrastructure.getRequirementsService()
    val environmentVariables = ModelInfrastructure.getEnvironmentVariables()
    val tagFilter = TagFilter(environmentVariables)
    var ignoredValues: MutableList<String> = mutableListOf()
    var ignoredTypes: MutableList<String> = mutableListOf()

    companion object {
        @JvmStatic
        fun from(testOutcomes: TestOutcomes) = TagResults(testOutcomes)
    }

    fun groupedByType(): List<TagResultSet> {
        val exclusions = TagExclusions(environmentVariables, testOutcomes)

        return forAllTags()
            .filter { tagResult -> StringUtils.isNotEmpty(tagResult.tag.type) }
            .filter { tagResult -> tagFilter.shouldDisplayTagWithType(tagResult.tag.type) }
            .filter { tagResult -> exclusions.doNotExclude(tagResult.tag) }
            .groupBy { tagResult -> tagResult.type }
            .map { (tagType, tagResults) -> TagResultSet(tagType, tagResults) }
            .sortedBy { tagResultSet -> tagResultSet.tagType }
    }


    fun forAllTags(): List<TagResult> =
        testOutcomes.tags
            .filter { tag -> !requirementsService.requirementTypes.contains(tag.type) }
            .filter { tag -> !ignoredValues.contains(tag.name) }
            .filter { tag -> !ignoredTypes.contains(tag.type) }
            .map { tag ->
                TagResult(
                    tag,
                    ReportNameProvider().forTag(tag),
                    testOutcomes.withTag(tag).total,
                    testOutcomes.withTag(tag).result
                )
            }

    fun ignoringValues(vararg valuesToIgnore: String): TagResults {
        ignoredValues.addAll(valuesToIgnore)
        return this
    }

    fun ignoringTypes(vararg typesToIgnore: String): TagResults {
        ignoredTypes.addAll(typesToIgnore)
        return this
    }

}

class TagResult(val tag: TestTag, val report: String, val count: Int, val result: TestResult) {
    val label = if (tag.type.equals("tag")) tag.name else "${tag.name} (${tag.type})"
    val type = if (tag.type.equals("tag")) "" else tag.type
    val color = BackgroundColor().inDarkforResult(result)
}

class TagResultSet(val tagType: String, val tagResults: List<TagResult>)
