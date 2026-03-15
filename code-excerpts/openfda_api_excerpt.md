[⬅️ Back to Interpill Project page](https://github.com/olgaleobel/interpill)

```kotlin
                                    
package com.olga.interpill.data.api

import com.olga.interpill.data.model.DrugLabel
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// NOTE: This is an EXCERPT for the Appendix (DTO models and client factory omitted).
class OpenFdaApi(
    private val apiKey: String? = null,
    private val client: HttpClient
) {
    private val base = "https://api.fda.gov/drug/label.json"

    // Local JSON parser for raw (full-section) methods.
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    /**
     * Fallback label lookup by name (INN/alias/brand).
     * Strategy: exact generic/substance → wildcard → brand → full-text.
     */
    suspend fun getLabelByName(name: String): DrugLabel? {
        val q = name.trim()
        if (q.isEmpty()) return null

        // 1) exact generic/substance
        firstItemBySearch("""openfda.generic_name:"$q"""")?.let { return it.toDrugLabelShort() }
        firstItemBySearch("""openfda.substance_name:"$q"""")?.let { return it.toDrugLabelShort() }

        // 2) wildcard generic/substance
        firstItemBySearch("""openfda.generic_name:${q}*""")?.let { return it.toDrugLabelShort() }
        firstItemBySearch("""openfda.substance_name:${q}*""")?.let { return it.toDrugLabelShort() }

        // 3) brand exact → brand wildcard
        firstItemBySearch("""openfda.brand_name:"$q"""")?.let { return it.toDrugLabelShort() }
        firstItemBySearch("""openfda.brand_name:${q}*""")?.let { return it.toDrugLabelShort() }

        // 4) final fallback: full-text search
        firstItemBySearch(""""$q"""")?.let { return it.toDrugLabelShort() }

        return null
    }

    /**
     * Full label (with ordered sections) by name (INN/alias/brand).
     * Uses the same staged search strategy as getLabelByName.
     */
    suspend fun getFullLabelByName(name: String, limit: Int = 1): DrugLabel? {
        val q = name.trim()
        if (q.isEmpty()) return null

        getFullLabelByQuery("""openfda.generic_name:"$q"""", limit)?.let { return it }
        getFullLabelByQuery("""openfda.substance_name:"$q"""", limit)?.let { return it }

        getFullLabelByQuery("""openfda.generic_name:${q}*""", limit)?.let { return it }
        getFullLabelByQuery("""openfda.substance_name:${q}*""", limit)?.let { return it }

        getFullLabelByQuery("""openfda.brand_name:"$q"""", limit)?.let { return it }
        getFullLabelByQuery("""openfda.brand_name:${q}*""", limit)?.let { return it }

        return getFullLabelByQuery(""""$q"""", limit)
    }

    /**
     * Generic raw OpenFDA query: fetch the first result and map it to DrugLabel (including sections).
     */
    suspend fun getFullLabelByQuery(searchExpr: String, limit: Int = 1): DrugLabel? {
        val raw = try {
            client.get(base) {
                parameter("search", searchExpr)
                parameter("limit", limit)
                apiKey?.let { parameter("api_key", it) }
            }.bodyAsText()
        } catch (_: Throwable) {
            return null
        }

        val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return null
        val first: JsonObject = root["results"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
        return drugLabelFromRawJson(first)
    }

    // ----------------- section ordering (for consistent UI rendering) -----------------

    private val DISPLAY_SECTION_ORDER: List<String> = listOf(
        "Indications and Usage",
        "Description",
        "Dosage and Administration",
        "Dosage Forms and Strengths",
        "Patient Counseling Information",
        "Medication Guide",
        "Warnings and Precautions",
        "Warnings and Cautions",
        "Warnings",
        "Boxed Warning",
        "Contraindications",
        "Precautions",
        "Drug Interactions",
        "Adverse Reactions",
        "Use in Specific Populations",
        "Overdosage",
        "Clinical Pharmacology",
        "Clinical Studies",
        "Nonclinical Toxicology",
        "How Supplied",
        "Storage and Handling"
    )

    // OpenFDA JSON keys → UI section titles.
    private val LABEL_SECTION_ORDER: List<Pair<String, String>> = listOf(
        "indications_and_usage" to "Indications and Usage",
        "description" to "Description",
        "dosage_and_administration" to "Dosage and Administration",
        "dosage_forms_and_strengths" to "Dosage Forms and Strengths",
        "patient_counseling_information" to "Patient Counseling Information",
        "spl_medguide" to "Medication Guide",
        "warnings_and_precautions" to "Warnings and Precautions",
        "warnings_and_cautions" to "Warnings and Cautions",
        "warnings" to "Warnings",
        "boxed_warning" to "Boxed Warning",
        "contraindications" to "Contraindications",
        "precautions" to "Precautions",
        "drug_interactions" to "Drug Interactions",
        "adverse_reactions" to "Adverse Reactions",
        "use_in_specific_populations" to "Use in Specific Populations",
        "overdosage" to "Overdosage",
        "clinical_pharmacology" to "Clinical Pharmacology",
        "clinical_studies" to "Clinical Studies",
        "nonclinical_toxicology" to "Nonclinical Toxicology",
        "how_supplied" to "How Supplied",
        "storage_and_handling" to "Storage and Handling"
    )

    /**
     * Maps raw OpenFDA JSON into the unified DrugLabel model (including ordered sections).
     */
    private fun drugLabelFromRawJson(item: JsonObject): DrugLabel {
        val openfda = item["openfda"]?.jsonObject

        fun arr(obj: JsonObject?, key: String): List<String> =
            obj?.get(key)?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

        val brandNames = arr(openfda, "brand_name")
        val genericNames = arr(openfda, "generic_name")

        // Build sections in a stable UI order; keep only non-empty sections.
        val sections = linkedMapOf<String, List<String>>()
        for ((key, title) in LABEL_SECTION_ORDER) {
            val values = arr(item, key).takeIf { it.isNotEmpty() }
            if (values != null) sections[title] = values
        }
        val orderedSections = reorderSections(sections)

        // Legacy fields kept for backward compatibility (v1.1).
        val boxed = arr(item, "boxed_warning")
        val warns = arr(item, "warnings")
        val contra = arr(item, "contraindications")
        val inter = arr(item, "drug_interactions")

        return DrugLabel(
            brandNames = brandNames,
            genericNames = genericNames,
            sections = orderedSections,
            boxedWarning = boxed,
            warnings = warns,
            contraindications = contra,
            interactions = inter
        )
    }

    private fun reorderSections(sections: Map<String, List<String>>): LinkedHashMap<String, List<String>> {
        val priority = DISPLAY_SECTION_ORDER.withIndex().associate { it.value to it.index }
        val unknownStart = DISPLAY_SECTION_ORDER.size + 1000

        return sections
            .toList()
            .sortedWith(
                compareBy(
                    { priority[it.first] ?: unknownStart },
                    { it.first }
                )
            )
            .fold(linkedMapOf()) { acc, (k, v) -> acc.apply { put(k, v) } }
    }

    // ----------------- minimal placeholders for omitted parts -----------------

    // In the original file, firstItemBySearch returns a typed DTO (LabelItem).
    private suspend fun firstItemBySearch(search: String, limit: Int = 1): LabelItem? {
        // EXCERPT NOTE: implementation omitted (HTTP + kotlinx serialization DTO).
        return null
    }

    // In the original file, LabelItem.toDrugLabel() builds a short DrugLabel (legacy fields only).
    private fun LabelItem.toDrugLabelShort(): DrugLabel = DrugLabel()

    // DTO type placeholder for the excerpt.
    private class LabelItem
}

``` 

[⬅️ Back to Interpill Project page](https://github.com/olgaleobel/interpill)
