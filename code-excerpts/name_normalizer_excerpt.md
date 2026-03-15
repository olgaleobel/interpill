[⬅️ Back to Interpill Project page](https://github.com/olgaleobel/interpill)

```kotlin

package com.olga.interpill.domain

/**
 * Normalises drug names to UK-oriented INN for search and UX (NHS/BNF style).
 *
 * Key features:
 * - Removes strength/pack/form/release noise for stable matching.
 * - Canonicalises brand names and US variants to UK INN.
 * - Supports an external alias layer (editable), which overrides built-in aliases.
 * - Preserves clinically meaningful nitrate naming (e.g., mono-/di-/trinitrate).
 */
object NameNormalizer {

    // --- numeric/format noise ---
    private val STRENGTH_REGEX = Regex(
        """\b\d+(\.\d+)?\s*(mcg|µg|ug|mg|g|gram|ml|mL|iu|IU|units?)\b""",
        RegexOption.IGNORE_CASE
    )
    private val PERCENT_REGEX  = Regex("""\b\d+(\.\d+)?\s*%\b""", RegexOption.IGNORE_CASE)
    private val PACK_REGEX     = Regex("""\b(\d+)\s*(tabs?|tablets?|caps?|capsules?)\b""", RegexOption.IGNORE_CASE)

    // --- release & form markers (whole words) ---
    private val RELEASE_MARKERS = listOf("sr","xr","xl","retard","odt","dr","mr","cr","er","ir","pr")
    private val FORM_WORDS = listOf(
        "tablet","tablets","tab","tabs","cap","caps","capsule","capsules",
        "oral","pill","solution","suspension","elixir","mixture",
        "injection","infusion","syrup","drops","drop","cream","ointment","gel","patch","spray","aerosol",
        "chewable","dispersible","enteric","enteric-coated","delayed-release","extended-release",
        "eye","ear","nasal","buccal","sublingual","suppository","powder","granules",
        "topical","transdermal","im","iv","sc","subcutaneous","intravenous","intramuscular"
    )

    // Salt/carrier words to remove. Note: "nitrate" is intentionally NOT removed.
    private val SALT_WORDS = listOf(
        "hydrochloride","hcl","hydrobromide","hbr",
        "mesylate","maleate","tartrate","succinate","fumarate","bitartrate","lactate",
        "phosphate","sulfate","sulphate","acetate","oxalate","benzoate","bromide",
        "sodium","potassium","magnesium","calcium","ammonium"
    )

    // Nitrates: preserve the full INN to avoid losing mono-/di-/trinitrate information.
    private val NITRATE_FULL = Regex(
        pattern = """\b(?:glyceryl\s+trinitrate|isosorbide\s+(?:mono|di)nitrate)\b""",
        option = RegexOption.IGNORE_CASE
    )

    // --- alias mapping to UK INN (lowercase) ---
    private val BUILTIN_ALIAS_TO_INN: Map<String, String> = mapOf(
        // Common OTC / frequent
        "acetaminophen" to "paracetamol",
        "tylenol"       to "paracetamol",
        "advil"         to "ibuprofen",
        "aleve"         to "naproxen",
        "prilosec"      to "omeprazole",

        // Respiratory (US ↔ UK)
        "albuterol"     to "salbutamol",
        "ventolin"      to "salbutamol",

        // Cardio (nitrates)
        "nitroglycerin" to "glyceryl trinitrate",

        // Common brands
        "zoloft"        to "sertraline",
        "xanax"         to "alprazolam"
    )

    // External (editable) alias layer; overrides built-in mapping.
    private var externalAliasToInn: Map<String, String> = emptyMap()

    /**
     * Updates external aliases (e.g., loaded from JSON).
     * Keys/values are normalised to lowercase for consistent matching.
     */
    fun setExternalAliases(aliases: Map<String, String>) {
        externalAliasToInn = aliases
            .mapKeys { it.key.trim().lowercase() }
            .mapValues { it.value.trim().lowercase() }
    }

    // ---------- core normalisers ----------

    /** Aggressive normalisation used for search and matching. */
    fun normaliseForQuery(raw: String): String {
        var s = raw.lowercase()

        s = STRENGTH_REGEX.replace(s, " ")
        s = PERCENT_REGEX.replace(s, " ")
        s = PACK_REGEX.replace(s, " ")

        // Remove bracketed fragments (typically strength/form notes).
        s = s.replace(Regex("""\([^)]*\)"""), " ")

        // Remove release and form markers as standalone words.
        (RELEASE_MARKERS + FORM_WORDS).forEach { marker ->
            s = s.replace(Regex("""\b${Regex.escape(marker)}\b""", RegexOption.IGNORE_CASE), " ")
        }

        // Remove salt/carrier words (excluding nitrates by design).
        s = s.replace(Regex("""\b(${SALT_WORDS.joinToString("|")})\b""", RegexOption.IGNORE_CASE), " ")

        // Clean separators (keep + and / for combination products).
        s = s.replace(Regex("""[•·,;]|\s-\s"""), " ")

        // Collapse whitespace.
        return s.replace(Regex("""\s+"""), " ").trim()
    }

    /** Extracts the base molecule name (INN-oriented) for query building. */
    fun baseMoleculeName(name: String): String {
        val q = normaliseForQuery(name)

        // Special cases: common compound INN patterns.
        if (Regex("""(?i)\bco[\s\-]?trimoxazole\b""").containsMatchIn(q)) return "co trimoxazole"
        if (Regex("""(?i)\bco[\s\-]?amoxiclav\b""").containsMatchIn(q)) return "co amoxiclav"
        if (Regex("""(?i)\bco[\s\-]?codamol\b""").containsMatchIn(q)) return "co codamol"

        // Nitrates: return full INN phrase, not a single token.
        NITRATE_FULL.find(q)?.let { return it.value }

        // Default: use the first meaningful token.
        val tokens = q.split(' ').filter { it.isNotBlank() }
        return if (tokens.isEmpty()) q else tokens[0]
    }

    /** Canonicalises aliases/brands/US variants to UK INN when known. */
    fun canonicalInn(s: String): String {
        val key = s.trim().lowercase()
        return externalAliasToInn[key] ?: BUILTIN_ALIAS_TO_INN[key] ?: key
    }

    // ---------- helpers for suggestions / UI ----------

    private fun String.capEach(): String =
        trim().split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { w ->
                w.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }

    /**
     * Returns an INN string for display (keeps multi-word INN).
     * The output is cleaned and UK-canonicalised.
     */
    fun displayInn(raw: String): String {
        val canon = canonicalInn(raw)
        val cleaned = normaliseForQuery(canon)
        return cleaned.capEach()
    }

    /** Splits combination products expressed as 'A/B' or 'A + B' into distinct INN display strings. */
    fun explodeComboToInns(raw: String): List<String> =
        raw.split('+', '/')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { part -> displayInn(baseMoleculeName(part)) }
            .distinct()

    /** Provides INN suggestions derived from alias mappings (built-in + external). */
    fun suggestInnsFromAliases(query: String): List<String> {
        val q = query.lowercase()
        if (q.length < 2) return emptyList()

        // Merge with external aliases taking precedence.
        val merged = if (externalAliasToInn.isEmpty()) BUILTIN_ALIAS_TO_INN
        else BUILTIN_ALIAS_TO_INN + externalAliasToInn

        return merged
            .filter { (alias, inn) ->
                alias.startsWith(q) || alias.contains(q) || inn.startsWith(q)
            }
            .map { (_, inn) -> displayInn(inn) }
            .distinct()
    }

    /**
     * Returns alias matches for producing suggestions such as "INN (Brand)".
     * Output pairs are lowercase (alias, inn).
     */
    fun aliasMatches(query: String): List<Pair<String, String>> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()

        val merged = if (externalAliasToInn.isEmpty()) BUILTIN_ALIAS_TO_INN
        else BUILTIN_ALIAS_TO_INN + externalAliasToInn

        return merged.asSequence()
            .filter { (alias, inn) -> alias.contains(q) || alias.startsWith(q) || inn.contains(q) }
            .map { it.key to it.value }
            .toList()
    }
}

``` 

[⬅️ Back to Interpill Project page](https://github.com/olgaleobel/interpill)
