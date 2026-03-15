[⬅️ Back to Interpill Project page](https://github.com/olgaleobel/interpill)

```kotlin

package com.olga.interpill.domain.rules

import com.olga.interpill.data.model.Drug
import com.olga.interpill.domain.InteractionItem
import com.olga.interpill.domain.InteractionSeverity

// Minimal embedded fallback rules used when external class rules are unavailable
object BuiltInClassRules {

    private val nitrates = setOf(
        "nitroglycerin", "isosorbide mononitrate", "isosorbide dinitrate", "glyceryl trinitrate"
    )
    private val pde5 = setOf("sildenafil", "tadalafil", "vardenafil", "avanafil")

    private val anticoagulants = setOf(
        "warfarin", "apixaban", "rivaroxaban", "dabigatran", "edoxaban", "acenocoumarol", "betrixaban"
    )
    private val nsaids = setOf(
        "ibuprofen","naproxen","diclofenac","ketoprofen","indomethacin","meloxicam",
        "piroxicam","celecoxib","etoricoxib","aspirin"
    )

    // Matches a drug against a class keyword set using both query and ingredient strings
    private fun Drug.matches(anyOf: Set<String>): Boolean {
        val q = query.lowercase()
        if (anyOf.any { q.contains(it) }) return true
        val ings = ingredients.map { it.lowercase() }
        return anyOf.any { key -> ings.any { it.contains(key) } }
    }

    // Evaluates pairwise combinations and returns high-risk class-rule findings
    fun evaluate(meds: List<Drug>): List<InteractionItem> {
        if (meds.size < 2) return emptyList()

        val out = mutableListOf<InteractionItem>()

        for (i in meds.indices) for (j in (i + 1) until meds.size) {
            val a = meds[i]
            val b = meds[j]

            // HIGH: nitrate + PDE-5 inhibitor
            if ((a.matches(nitrates) && b.matches(pde5)) || (a.matches(pde5) && b.matches(nitrates))) {
                out += InteractionItem(
                    a, b,
                    severity = InteractionSeverity.HIGH,
                    source = "Class rule",
                    description = "Nitrate + PDE-5 inhibitor → profound hypotension (avoid)."
                )
                continue
            }

            // HIGH: anticoagulant + NSAID
            if ((a.matches(anticoagulants) && b.matches(nsaids)) || (a.matches(nsaids) && b.matches(anticoagulants))) {
                out += InteractionItem(
                    a, b,
                    severity = InteractionSeverity.HIGH,
                    source = "Class rule",
                    description = "Anticoagulant + NSAID → increased bleeding risk (avoid / monitor)."
                )
            }
        }

        return out
    }
}

``` 

[⬅️ Back to Interpill Project page](https://github.com/olgaleobel/interpill)
