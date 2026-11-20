package com.alsalama.custody

import java.util.Locale

internal data class ExpenseClassification(
    val category: String,
    val needsManagerReview: Boolean
)

internal fun classifyExpense(description: String): ExpenseClassification {
    val value = description.trim().lowercase(Locale.ROOT)
        .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا")
        .replace("ى", "ي").replace("ة", "ه")

    // "دخان" has priority and is classified as gratuities.
    if (listOf("دخان", "اكراميه", "اكراميات").any { value.contains(it) }) {
        return ExpenseClassification("إكراميات", false)
    }

    // Known ambiguous phrase: keep it in Other for manager review.
    if (value.contains("مصروفات السادات") || value == "مصروف السادات") {
        return ExpenseClassification("أخرى", true)
    }

    if (listOf("فطار", "غداء", "عشاء", "قهوه", "شاي", "بن", "سندوتش", "بلح", "مشروبات")
            .any { value.contains(it) }) {
        return ExpenseClassification("مأكولات ومشروبات", false)
    }

    if (listOf("بنزين", "سولار", "وقود", "زيت محرك", "زيت موتور")
            .any { value.contains(it) }) {
        return ExpenseClassification("وقود وزيوت", false)
    }

    if (listOf(
            "مواصلات", "انتقالات", "توك توك", "توكتوك", "توتوك", "تروسيكل",
            "نولون", "نقل", "شحن", "تحميل", "مشوار", "كارتة", "كرتة",
            "كارتات", "كرتات", "بوابة", "ميزان جيش", "مرور", "رسوم دخول", "صحراوي", "طريق"
        ).any { value.contains(it) }) {
        return ExpenseClassification("مواصلات وانتقالات", false)
    }

    if (listOf(
            "خرط", "طنابير", "تيل فرامل", "مساعد", "عفشه", "ميكانيكي",
            "كهرباء السياره", "زجاج السياره", "غسيل السياره", "صيانة",
            "صيانه", "مخرطه", "فريزه"
        ).any { value.contains(it) }) {
        return ExpenseClassification("صيانة ومصنعية", false)
    }

    if (listOf(
            "خامة", "خامات", "ماسور", "جلب", "كاتينه", "مسامير", "صواميل",
            "زرجينه", "سيلكون", "سوست", "شحم", "خوص", "قطع غيار", "مستلزمات تشغيل"
        ).any { value.contains(it) }) {
        return ExpenseClassification("خامات وقطع غيار ومستلزمات تشغيل", false)
    }

    if (listOf("نظارة لحام", "نظارات لحام", "عدة", "ادوات", "معدات سلامة")
            .any { value.contains(it) }) {
        return ExpenseClassification("أدوات وسلامة ومستلزمات عامة", false)
    }

    return ExpenseClassification("أخرى", true)
}
