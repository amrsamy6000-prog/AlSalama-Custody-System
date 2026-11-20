package com.alsalama.custody

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseCategorizerTest {
    @Test fun fuelMustNotBeMistakenForCoffeeBeans() {
        assertEquals("وقود وزيوت", classifyExpense("بنزين رضا حمدي").category)
        assertEquals("مأكولات ومشروبات", classifyExpense("نص بن للمكتب").category)
    }

    @Test fun smokeHasTheApprovedGratuitiesPriority() {
        val result = classifyExpense("دخان ورشة عفشة")
        assertEquals("إكراميات", result.category)
        assertFalse(result.needsManagerReview)
    }

    @Test fun allTransportAndFreightUseOneCategory() {
        listOf(
            "توك توك من صهرجت إلى ميت أبو خالد",
            "تروسيكل لنقل قلب الخلاط إلى المخرطة",
            "نولون مصطفى أبيار",
            "كارتة بوابة مدينة بدر",
            "ميزان جيش الفيوم"
        ).forEach { assertEquals(it, "مواصلات وانتقالات", classifyExpense(it).category) }
    }

    @Test fun ambiguousSadatExpenseNeedsManagerReview() {
        val result = classifyExpense("مصروفات السادات")
        assertEquals("أخرى", result.category)
        assertTrue(result.needsManagerReview)
    }

    @Test fun unknownDescriptionGoesToOtherAndNeedsReview() {
        val result = classifyExpense("مبلغ غير واضح")
        assertEquals("أخرى", result.category)
        assertTrue(result.needsManagerReview)
    }

    @Test fun foodAndMaintenanceExamplesAreRecognized() {
        assertEquals("مأكولات ومشروبات", classifyExpense("قهوة على الطريق").category)
        assertEquals("صيانة ومصنعية", classifyExpense("خرط طنابير الفيرنا").category)
        assertEquals("خامات وقطع غيار ومستلزمات تشغيل", classifyExpense("مسامير وصواميل لمساحات الخلاط").category)
    }
}
