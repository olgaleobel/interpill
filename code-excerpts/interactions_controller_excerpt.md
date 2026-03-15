[⬅️ Back to Interpill Project page](https://github.com/olgaleobel/interpill)

```kotlin

package com.olga.interpill.domain

import com.olga.interpill.data.api.RxNormApi
import com.olga.interpill.data.model.Drug
import com.olga.interpill.data.profile.PatientContext

// Facade that converts user input into normalised Drug objects
class InteractionsController(
    private val rx: RxNormApi = RxNormApi(),
    private val service: InteractionService = InteractionServiceDefault()
) {

    suspend fun computeFromNames(
        names: List<String>,
        patient: PatientContext? = null
    ): InteractionResult {

        val meds = mutableListOf<Drug>()

        for (raw in names) {

            // User-facing base name (without bracketed brand or form)
            val displayBase = raw.substringBefore("(").trim().ifBlank { raw.trim() }

            // INN-oriented key used for RXCUI resolution
            val innKey = NameNormalizer.normaliseForQuery(
                NameNormalizer.canonicalInn(
                    NameNormalizer.baseMoleculeName(displayBase)
                )
            )

            // Resolve RXCUI and active ingredients via RxNorm
            val rxcui = rx.getRxcuiByName(innKey)
            val ingredients = rxcui?.let { rx.getIngredients(it) }.orEmpty()

            // Build internal Drug representation
            meds += Drug(
                query = NameNormalizer.displayInn(displayBase),
                rxcui = rxcui,
                ingredients = ingredients
            )
        }

        // Delegate interaction analysis to the core service
        return service.checkInteractions(meds, patient)
    }
}

``` 

[⬅️ Back to Interpill Project page](https://github.com/olgaleobel/interpill)
