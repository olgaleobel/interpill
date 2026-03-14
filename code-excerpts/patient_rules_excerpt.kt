package com.olga.interpill.domain.rules

import com.olga.interpill.data.model.Drug
import com.olga.interpill.data.profile.PatientContext
import com.olga.interpill.domain.InteractionItem
import com.olga.interpill.domain.InteractionSeverity

// Patient-aware rules derived from allergies, conditions, and pregnancy/breastfeeding status
// Note: self-pairs (a == b) are emitted to allow the UI to highlight medicine-specific risks.
object PatientRules {

    /* ----- helpers ----- */

    private fun String.norm(): String = trim().lowercase()

    // Checks whether any of the needles appears (as a substring) in any haystack entry
    private fun hasAny(haystack: Collection<String>, vararg needles: String): Boolean {
        if (haystack.isEmpty() || needles.isEmpty()) return false
        val h = haystack.map { it.norm() }
        return needles.any { n ->
            val nn = n.norm()
            h.any { it.contains(nn) }
        }
    }

    // Checks whether a drug token set contains any of the given needles (substring match)
    private fun drugHasAny(drugTokens: Set<String>, vararg needles: String): Boolean {
        if (drugTokens.isEmpty() || needles.isEmpty()) return false
        return needles.any { n ->
            val nn = n.norm()
            drugTokens.any { it.contains(nn) }
        }
    }

    fun evaluate(meds: List<Drug>, patient: PatientContext): List<InteractionItem> {
        if (meds.isEmpty()) return emptyList()

        val out = mutableListOf<InteractionItem>()

        // Patient profile fields (normalised to empty lists when absent)
        val allergies: Collection<String> = patient.allergies ?: emptyList()
        val conditions: Collection<String> = patient.conditions ?: emptyList()

        // --------- conditions ----------
        val hasT1D = hasAny(conditions, "type 1 diabetes", "type i diabetes", "t1d")
        val hasAnyDiabetes = hasT1D || hasAny(conditions, "diabetes", "type 2 diabetes", "type ii diabetes", "t2d")

        // For each medicine, build a token set (query + ingredients) and emit self-pair findings
        meds.forEach { d ->
            val drugTokens: Set<String> = buildSet {
                add(d.query.norm())
                d.ingredients.forEach { add(it.norm()) }
            }

            // (0) Direct allergy match against medicine name or ingredients → HIGH
            allergies.firstOrNull { token ->
                val t = token.norm()
                drugTokens.any { it.contains(t) } || t.contains(d.query.norm())
            }?.let { hit ->
                out += InteractionItem(
                    a = d, b = d,
                    severity = InteractionSeverity.HIGH,
                    description = "Patient allergy: $hit — avoid.",
                    source = "Patient"
                )
            }

            // (1) Sulfonamide allergy ↔ TMP/SMX and related sulfonamide antibiotics → HIGH
            val sulfaAllergy = hasAny(allergies, "sulfa", "sulfonamide", "sulphonamide")
            if (sulfaAllergy && drugHasAny(
                    drugTokens,
                    "sulfamethoxazole", "co-trimoxazole", "trimethoprim/sulfamethoxazole", "tmp/smx"
                )
            ) {
                out += InteractionItem(
                    a = d, b = d,
                    severity = InteractionSeverity.HIGH,
                    description = "Sulfonamide allergy: avoid TMP/SMX and related sulfonamide antibiotics.",
                    source = "Patient"
                )
            }

            // (2) Penicillin allergy ↔ penicillins → HIGH
            val penAllergy = hasAny(allergies, "penicillin", "amoxicillin", "ampicillin")
            if (penAllergy && drugHasAny(drugTokens, "penicillin", "amoxicillin", "ampicillin")) {
                out += InteractionItem(
                    a = d, b = d,
                    severity = InteractionSeverity.HIGH,
                    description = "Penicillin allergy: avoid penicillins.",
                    source = "Patient"
                )
            }

            // (3) Any diabetes ↔ fluoroquinolones → MODERATE (risk of dysglycaemia)
            if (hasAnyDiabetes && drugHasAny(
                    drugTokens,
                    "fluoroquinolone", "ciprofloxacin", "levofloxacin", "moxifloxacin", "ofloxacin"
                )
            ) {
                out += InteractionItem(
                    a = d, b = d,
                    severity = InteractionSeverity.MODERATE,
                    description = "Diabetes: fluoroquinolones may cause dysglycaemia. Monitor glucose closely.",
                    source = "Patient"
                )
            }

            // (4) Type 1 diabetes ↔ sulfonylureas → HIGH (not indicated / avoid)
            if (hasT1D && drugHasAny(
                    drugTokens,
                    "sulfonylurea", "glibenclamide", "glyburide", "gliclazide", "glipizide", "glimepiride"
                )
            ) {
                out += InteractionItem(
                    a = d, b = d,
                    severity = InteractionSeverity.HIGH,
                    description = "Type 1 diabetes: sulfonylureas are not indicated / avoid.",
                    source = "Patient"
                )
            }

            // (5) Pregnancy / breastfeeding → LOW (generic reminder per medicine)
            if (patient.pregnancyOrBreastfeeding) {
                out += InteractionItem(
                    a = d, b = d,
                    severity = InteractionSeverity.LOW,
                    description = "Pregnancy/Breastfeeding: check safety of ${d.query}.",
                    source = "Patient"
                )
            }
        }

        return out
    }
}
