package com.olga.interpill.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.CancellationException

// NOTE: This is an EXCERPT for the Appendix (network client setup and DTO models omitted).
class RxNormApi(
    private val client: HttpClient
) {
    private val base = "https://rxnav.nlm.nih.gov/REST"

    /**
     * Resolves RXCUI by a name (INN-first strategy).
     * Fallback chain: drugs.json (preferred TTY) → rxcui.json → approximateTerm.
     */
    suspend fun getRxcuiByName(name: String): String? {
        val q = name.trim()
        if (q.isEmpty()) return null

        // 1) drugs.json: pick "best" candidate by TTY priority and name closeness
        try {
            val byDrugs: DrugsResponse = client.get("$base/drugs.json") {
                parameter("name", q)
            }.body()
            pickPreferredRxcuiFromDrugs(byDrugs, q)?.let { return it }
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            // continue to next fallback
        }

        // 2) direct lookup: rxcui.json
        try {
            val direct: IdGroupResponse = client.get("$base/rxcui.json") {
                parameter("name", q)
            }.body()
            direct.idGroup?.rxnormId?.firstOrNull()?.let { return it }
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
        }

        // 3) approximate match: approximateTerm → top candidate
        return try {
            val approx: ApproxResponse = client.get("$base/approximateTerm.json") {
                parameter("term", q)
                parameter("maxEntries", 5)
            }.body()
            approx.approxGroup?.candidate?.firstOrNull()?.rxcui
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            null
        }
    }

    /**
     * Extracts active ingredients (IN level) for a given RXCUI.
     * For interaction checks and class rules, IN-level ingredients are sufficient.
     */
    suspend fun getIngredients(rxcui: String): List<String> = try {
        val rel: AllRelatedResponse =
            client.get("$base/rxcui/$rxcui/allrelated.json").body()

        rel.allRelatedGroup?.conceptGroup.orEmpty()
            .filter { it.tty.equals("IN", ignoreCase = true) }
            .flatMap { it.conceptProperties.orEmpty() }
            .mapNotNull { it.name }
            .distinct()
    } catch (e: Throwable) {
        if (e is CancellationException) throw e
        emptyList()
    }

    // ----------------- helper: preferred RXCUI selection -----------------

    /**
     * Chooses the most suitable RXCUI from /drugs.json by:
     * (1) TTY priority (IN → MIN → PIN → SCD/SBD → BN → others),
     * (2) name closeness (exact > startsWith > shorter).
     */
    private fun pickPreferredRxcuiFromDrugs(resp: DrugsResponse, query: String): String? {
        val groups = resp.drugGroup?.conceptGroup.orEmpty()
        if (groups.isEmpty()) return null

        val ttyOrder = listOf(
            "IN", "MIN", "PIN", "SCD", "SBD", "BN", "SCDF", "SCDC", "SBDC", "GPCK", "BPCK"
        )

        data class Cand(val tty: String, val rxcui: String, val name: String)

        val cands = buildList {
            for (g in groups) {
                val tty = g.tty?.uppercase() ?: continue
                for (cp in g.conceptProperties.orEmpty()) {
                    val id = cp.rxcui ?: continue
                    val nm = cp.name ?: continue
                    add(Cand(tty, id, nm))
                }
            }
        }
        if (cands.isEmpty()) return null

        val qLower = query.lowercase()
        fun nameScore(n: String): Triple<Boolean, Boolean, Int> {
            val nl = n.lowercase()
            val exact = nl == qLower
            val starts = nl.startsWith(qLower)
            return Triple(exact, starts, nl.length)
        }

        return cands
            .sortedWith(
                compareBy<Cand> {
                    ttyOrder.indexOf(it.tty).let { idx -> if (idx >= 0) idx else Int.MAX_VALUE }
                }.thenComparator { a, b ->
                    val sa = nameScore(a.name)
                    val sb = nameScore(b.name)
                    when {
                        sa.first != sb.first -> if (sa.first) -1 else 1
                        sa.second != sb.second -> if (sa.second) -1 else 1
                        else -> sa.third.compareTo(sb.third)
                    }
                }
            )
            .firstOrNull()?.rxcui
    }

    // DTO models (IdGroupResponse / DrugsResponse / AllRelatedResponse / ApproxResponse) are defined in the original file.
}
