[⬅️ Back to Interpill Project page](https://github.com/olgaleobel/interpill)

```kotlin

package com.olga.interpill.domain

import com.olga.interpill.data.api.RxNavInteractionApi
import com.olga.interpill.data.api.RxNavInteractionResponse
import com.olga.interpill.data.model.Drug
import com.olga.interpill.data.profile.PatientContext
import com.olga.interpill.data.net.HttpClients
import com.olga.interpill.domain.rules.BuiltInClassRules
import com.olga.interpill.domain.rules.ExternalClassRules
import com.olga.interpill.domain.rules.PatientRules
import io.ktor.client.HttpClient

// Contract for interaction analysis
interface InteractionService {
    suspend fun checkInteractions(
        meds: List<Drug>,
        patient: PatientContext? = null
    ): InteractionResult
}

// Default implementation aggregating multiple sources and rules
class InteractionServiceDefault(
    private val http: HttpClient,
    private val api: RxNavInteractionApi,
    private val fdaFallback: OpenFdaFallback,
    private val dailyMedFallback: DailyMedFallback
) : InteractionService {

    // Uses shared HTTP client
    constructor() : this(
        http = HttpClients.shared,
        api = RxNavInteractionApi(HttpClients.shared),
        fdaFallback = OpenFdaFallback(),
        dailyMedFallback = DailyMedFallback()
    )

    // Allows explicit HTTP injection
    constructor(http: HttpClient) : this(
        http = http,
        api = RxNavInteractionApi(http),
        fdaFallback = OpenFdaFallback(),
        dailyMedFallback = DailyMedFallback()
    )

    override suspend fun checkInteractions(
        meds: List<Drug>,
        patient: PatientContext?
    ): InteractionResult {

        if (meds.isEmpty()) return InteractionResult()

        // Primary source: official RxNav interactions
        val official = try { rxNavPairs(meds) } catch (_: Throwable) { emptyList() }

        // Fallback sources based on label heuristics
        val fda = try { fdaFallback.check(meds) } catch (_: Throwable) { InteractionResult() }
        val dailymed = try { dailyMedFallback.check(meds) } catch (_: Throwable) { InteractionResult() }

        // Class-based rules (external preferred, built-in as fallback)
        val classExt = try { ExternalClassRules.evaluate(http, meds) } catch (_: Throwable) { emptyList() }
        val classFallback = if (classExt.isEmpty()) BuiltInClassRules.evaluate(meds) else emptyList()

        // Patient-aware rules (allergies, conditions)
        val patientItems = try {
            if (patient != null) PatientRules.evaluate(meds, patient) else emptyList()
        } catch (_: Throwable) { emptyList() }

        // Local heuristic rules
        val localRules = ClassRules.applyRules(meds)

        // Merge and deduplicate all findings
        val allItems =
            (official + fda.items + dailymed.items + classExt + classFallback + localRules + patientItems)
                .distinctBy { item ->
                    listOf(item.a.query.lowercase(), item.b.query.lowercase())
                        .sorted()
                        .joinToString("::") +
                            "::" + item.severity.name +
                            "::" + item.source
                }

        // Additional safety notes (e.g. duplicate ingredients)
        val notes =
            (fda.notes + dailymed.notes + InteractionChecker.duplicateIngredients(meds)).distinct()

        return InteractionResult(items = allItems, notes = notes)
    }

    // RxNav interaction lookup by RXCUI
    private suspend fun rxNavPairs(meds: List<Drug>): List<InteractionItem> {
        val rxcuis = meds.mapNotNull { it.rxcui }.distinct()
        if (rxcuis.size < 2) return emptyList()
        return toItems(api.interactionsByRxcuis(rxcuis), meds)
    }

    // Maps RxNav response to internal interaction model
    private fun toItems(
        resp: RxNavInteractionResponse?,
        meds: List<Drug>
    ): List<InteractionItem> {

        if (resp == null) return emptyList()

        val byRx = meds.associateBy { it.rxcui }
        val out = mutableListOf<InteractionItem>()

        val groups = resp.fullInteractionTypeGroup ?: return emptyList()
        for (g in groups) {
            val entries = g.fullInteractionType ?: continue
            for (e in entries) {
                val a = byRx[e.minConcept?.getOrNull(0)?.rxcui]
                val b = byRx[e.minConcept?.getOrNull(1)?.rxcui]
                if (a != null && b != null) {
                    for (p in e.interactionPair.orEmpty()) {
                        out += InteractionItem(
                            a = a,
                            b = b,
                            severity = when (p.severity?.lowercase()) {
                                "high" -> InteractionSeverity.HIGH
                                "moderate" -> InteractionSeverity.MODERATE
                                "low" -> InteractionSeverity.LOW
                                else -> InteractionSeverity.UNKNOWN
                            },
                            description = p.description,
                            source = g.sourceName ?: "RxNav"
                        )
                    }
                }
            }
        }
        return out
    }
}

[⬅️ Back to Interpill Project page](https://github.com/olgaleobel/interpill)
