package com.alsalama.custody

import java.util.Locale

internal data class ExpenseClassification(
    val category: String,
    val needsManagerReview: Boolean
)

private fun normalizeExpenseText(input: String): String {
    val normalized = input.trim().lowercase(Locale.ROOT)
        .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا")
        .replace("ى", "ي").replace("ة", "ه")
    return normalized.replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim().replace(Regex("\\s+"), " ")
}

private fun hasAnyPhrase(text: String, phrases: List<String>): Boolean {
    val padded = " $text "
    return phrases.any { phrase ->
        val normalizedPhrase = normalizeExpenseText(phrase)
        normalizedPhrase.isNotEmpty() && padded.contains(" $normalizedPhrase ")
    }
}

/**
 * Classifies the real expense descriptions used at AlSalama.
 * Unknown or ambiguous descriptions are kept under "أخرى" for manager review.
 */
internal fun classifyExpense(description: String): ExpenseClassification {
    val text = normalizeExpenseText(description)
    if (text.isBlank()) return ExpenseClassification("أخرى", true)

    // User-approved priority: any description containing "دخان" is gratuities.
    if (hasAnyPhrase(text, listOf("دخان", "إكرامية", "إكراميات", "اكراميه", "اكراميات"))) {
        return ExpenseClassification("إكراميات", false)
    }

    // Explicitly keep this known ambiguous wording in Other until the manager reviews it.
    if (hasAnyPhrase(text, listOf("مصروف السادات", "مصروفات السادات"))) {
        return ExpenseClassification("أخرى", true)
    }

    // Fuel is checked before food because "بن" is also part of "بنزين".
    if (hasAnyPhrase(text, listOf(
            "بنزين", "سولار", "وقود", "زيت محرك", "زيت موتور", "زيت"
        ))) {
        return ExpenseClassification("وقود وزيوت", false)
    }

    if (hasAnyPhrase(text, listOf(
            "فطار", "غداء", "عشاء", "قهوة", "قهوه", "شاي", "بن", "نص بن",
            "سندوتش", "سندوتشات", "بلح", "مشروبات"
        ))) {
        return ExpenseClassification("مأكولات ومشروبات", false)
    }

    // One combined category: people, equipment/material transport, freight and road charges.
    if (hasAnyPhrase(text, listOf(
            "مواصلات", "انتقالات", "توك توك", "توك توك", "توكتوك", "توتوك",
            "تروسيكل", "نولون", "نقل", "شحن", "تحميل", "مشوار", "كارتة",
            "كرتة", "كارتات", "كرتات", "بوابة", "ميزان جيش", "مرور",
            "رسوم دخول", "صحراوي", "طريق"
        ))) {
        return ExpenseClassification("مواصلات وانتقالات", false)
    }

    if (hasAnyPhrase(text, listOf(
            "صيانة", "صيانه", "خرط", "طنابير", "تيل فرامل", "تيل",
            "مساعد", "عفشة", "عفشه", "ميكانيكي", "كهرباء السيارة",
            "كهرباء السياره", "زجاج السيارة", "زجاج السياره", "زجاج أمامي",
            "زجاج امامي", "غسيل السيارة", "غسيل السياره", "لمبة ليد للسيارة",
            "لمبه ليد للسياره", "كوتش كبلان", "مصنعية عفشة", "مصنعية"
        ))) {
        return ExpenseClassification("صيانة ومصنعية", false)
    }

    if (hasAnyPhrase(text, listOf(
            "خامة", "خامات", "ماسور", "جلب", "كاتينة", "كاتينه", "مسامير",
            "صواميل", "زرجينة", "زرجينه", "سيليكون", "سيلكون", "سوست",
            "شحم", "خوص", "قطع غيار", "مستلزمات تشغيل", "قلب الخلاط",
            "صاج تيل", "يد استيم"
        ))) {
        return ExpenseClassification("خامات وقطع غيار ومستلزمات تشغيل", false)
    }

    if (hasAnyPhrase(text, listOf(
            "نظارة لحام", "نظارات لحام", "عدة", "ادوات", "أدوات",
            "معدات سلامة", "منظفات", "منظفات الحمام"
        ))) {
        return ExpenseClassification("أدوات وسلامة ومستلزمات عامة", false)
    }

    return ExpenseClassification("أخرى", true)
}
