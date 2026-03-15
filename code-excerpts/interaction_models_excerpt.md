[⬅️ Back to Interpill Project page](https://github.com/olgaleobel/interpill)

```kotlin

package com.olga.interpill.domain

import com.olga.interpill.data.model.Drug

// Unified severity scale used across all interaction sources
enum class InteractionSeverity { HIGH, MODERATE, LOW, UNKNOWN }

// Single interaction finding for a pair of medicines
data class InteractionItem(
    val a: Drug,                       // first medicine
    val b: Drug,                       // second medicine
    val severity: InteractionSeverity, // normalised severity level
    val description: String? = null,   // optional source-provided explanation
    val source: String = "RxNav"       // origin of the finding
)

// Aggregated interaction result returned to the UI layer
data class InteractionResult(
    val items: List<InteractionItem> = emptyList(), // consolidated interaction list
    val notes: List<String> = emptyList()           // additional safety notes
)

``` 

[⬅️ Back to Interpill Project page](https://github.com/olgaleobel/interpill)
