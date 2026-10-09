package com.example.engine

import androidx.compose.ui.graphics.Color

enum class PhilippineCarrier(
    val displayName: String,
    val shortName: String,
    val brandColorHex: Long
) {
    GLOBE_TM_GOMO(
        displayName = "Globe Telecom / TM / GOMO",
        shortName = "Globe / TM",
        brandColorHex = 0xFF0284C7 // Vibrant Telecom Blue
    ),
    SMART_TNT_SUN(
        displayName = "Smart / TNT / Sun Cellular",
        shortName = "Smart / TNT",
        brandColorHex = 0xFF16A34A // Vibrant Cellular Green
    ),
    DITO(
        displayName = "DITO Telecommunity",
        shortName = "DITO",
        brandColorHex = 0xFFDC2626 // DITO Red
    ),
    UNKNOWN(
        displayName = "Other / International",
        shortName = "Other",
        brandColorHex = 0xFF6B7280 // Neutral Slate
    );

    val composeColor: Color
        get() = Color(brandColorHex)
}

data class CarrierInfo(
    val carrier: PhilippineCarrier,
    val normalizedNumber: String,
    val detectedPrefix: String
)

object PhilippineCarrierClassifier {

    // 5-digit Globe Postpaid Prefixes
    private val GLOBE_POSTPAID_5_DIGIT = setOf(
        "09175", "09176", "09178",
        "09253", "09255", "09256", "09257", "09258"
    )

    // 4-digit Globe Telecom / TM (Touch Mobile) / GOMO Prefixes
    private val GLOBE_TM_GOMO_4_DIGIT = setOf(
        "0817", "0905", "0906", "0915", "0916", "0917", "0926", "0927",
        "0935", "0936", "0945", "0953", "0954", "0955", "0956", "0965",
        "0966", "0967", "0975", "0976", "0977", "0978", "0979", "0995", "0997"
    )

    // 4-digit Smart Communications / TNT (Talk 'N Text) Prefixes
    private val SMART_TNT_4_DIGIT = setOf(
        "0813", "0907", "0908", "0909", "0910", "0911", "0912", "0913",
        "0914", "0918", "0919", "0920", "0921", "0928", "0929", "0930",
        "0938", "0939", "0946", "0947", "0948", "0949", "0950", "0961",
        "0962", "0963", "0968", "0970", "0981", "0989", "0992", "0998", "0999"
    )

    // 4-digit Sun Cellular Prefixes (merged with Smart)
    private val SUN_CELLULAR_4_DIGIT = setOf(
        "0922", "0923", "0924", "0925", "0931", "0932", "0933", "0934",
        "0940", "0941", "0942", "0943", "0944", "0973", "0974"
    )

    // 4-digit DITO Telecommunity Prefixes
    private val DITO_4_DIGIT = setOf(
        "0895", "0896", "0897", "0898", "0991", "0992", "0993", "0994"
    )

    /**
     * Normalizes any input format (+639171234567, 639171234567, 9171234567, 09171234567)
     * to the standard 11-digit local format: 09XXXXXXXXX or 08XXXXXXXXX.
     */
    fun normalizeToStandardPrefix(phoneNumber: String): String {
        val clean = phoneNumber.filter { it.isDigit() }
        return when {
            clean.startsWith("63") && clean.length == 12 -> "0" + clean.substring(2)
            clean.startsWith("0") && clean.length == 11 -> clean
            clean.length == 10 && (clean.startsWith("9") || clean.startsWith("8")) -> "0$clean"
            else -> clean
        }
    }

    /**
     * Detects carrier based on the phone number prefix.
     * Evaluates 5-digit postpaid prefixes first, then 4-digit prefixes.
     */
    fun detectCarrier(phoneNumber: String): PhilippineCarrier {
        return getCarrierInfo(phoneNumber).carrier
    }

    /**
     * Returns full carrier classification and matched prefix.
     */
    fun getCarrierInfo(phoneNumber: String): CarrierInfo {
        val standard = normalizeToStandardPrefix(phoneNumber)
        if (standard.length < 4) {
            return CarrierInfo(PhilippineCarrier.UNKNOWN, standard, "")
        }

        // 1. Check 5-digit prefixes (Globe Postpaid e.g. 09175, 09253)
        if (standard.length >= 5) {
            val prefix5 = standard.substring(0, 5)
            if (GLOBE_POSTPAID_5_DIGIT.contains(prefix5)) {
                return CarrierInfo(PhilippineCarrier.GLOBE_TM_GOMO, standard, prefix5)
            }
        }

        // 2. Check 4-digit prefixes
        val prefix4 = standard.substring(0, 4)

        // Globe / TM / GOMO
        if (GLOBE_TM_GOMO_4_DIGIT.contains(prefix4)) {
            return CarrierInfo(PhilippineCarrier.GLOBE_TM_GOMO, standard, prefix4)
        }

        // Smart / TNT
        if (SMART_TNT_4_DIGIT.contains(prefix4) || SUN_CELLULAR_4_DIGIT.contains(prefix4)) {
            return CarrierInfo(PhilippineCarrier.SMART_TNT_SUN, standard, prefix4)
        }

        // DITO
        if (DITO_4_DIGIT.contains(prefix4)) {
            return CarrierInfo(PhilippineCarrier.DITO, standard, prefix4)
        }

        return CarrierInfo(PhilippineCarrier.UNKNOWN, standard, prefix4)
    }

    /**
     * Returns recommended SIM slot index (0 for SIM 1, 1 for SIM 2)
     * based on default routing configuration:
     * SIM 1 -> Globe Telecom / TM / GOMO
     * SIM 2 -> Smart Communications / TNT / Sun Cellular
     */
    fun getRecommendedSimSlot(phoneNumber: String): Int {
        return when (detectCarrier(phoneNumber)) {
            PhilippineCarrier.GLOBE_TM_GOMO -> 0 // SIM 1
            PhilippineCarrier.SMART_TNT_SUN -> 1 // SIM 2
            PhilippineCarrier.DITO -> 0          // Fallback to SIM 1
            PhilippineCarrier.UNKNOWN -> 0       // Fallback to SIM 1
        }
    }
}
