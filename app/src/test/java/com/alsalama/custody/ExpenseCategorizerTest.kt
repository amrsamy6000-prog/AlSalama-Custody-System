package com.alsalama.custody

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseCategorizerTest {
    @Test fun fuelMustNotBeMistakenForCoffeeBeans() {
        assertEquals("وقود", classifyExpense("بنزين رضا حمدي").category)
        assertEquals("وقود", classifyExpense("سولار جامبو").category)
        assertEquals("مأكولات ومشروبات", classifyExpense("نص بن للمكتب").category)
    }

    @Test fun automotiveOilsBelongToCarMaintenanceNotFuel() {
        listOf("زيت محرك الفيرنا", "زيت موتور السيارة", "زيت فتيس شاهين").forEach {
            assertEquals("صيانة سيارات", classifyExpense(it).category)
        }
        val unclearOil = classifyExpense("شراء زيت")
        assertEquals("أخرى", unclearOil.category)
        assertTrue(unclearOil.needsManagerReview)
    }

    @Test fun smokeHasTheApprovedGratuitiesPriority() {
        val result = classifyExpense("دخان ورشة عفشة")
        assertEquals("إكراميات", result.category)
        assertFalse(result.needsManagerReview)
    }

    @Test fun transportAndRoadFeesUseTheirOwnApprovedCategories() {
        listOf(
            "توك توك من صهرجت إلى ميت أبو خالد",
            "تروسيكل لنقل قلب الخلاط إلى المخرطة",
            "نولون مصطفى أبيار"
        ).forEach { assertEquals("مواصلات وانتقالات", classifyExpense(it).category) }
        listOf(
            "كارتة بوابة مدينة بدر",
            "ميزان جيش الفيوم",
            "رسوم دخول الريف الأوروبي"
        ).forEach { assertEquals("كارتات ورسوم عبور", classifyExpense(it).category) }
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
        assertEquals("صيانة سيارات", classifyExpense("خرط طنابير الفيرنا").category)
        assertEquals("صيانة سيارات", classifyExpense("مصنعية مخرطة وفريزة").category)
        assertEquals("مستلزمات ورشة", classifyExpense("مسامير وصواميل لمساحات الخلاط").category)
    }

    @Test fun ambiguousDescriptionsStayInOtherForManagerReview() {
        listOf("مصنعية محمد النمس", "منظفات للحمام").forEach { description ->
            val result = classifyExpense(description)
            assertEquals("أخرى", result.category)
            assertTrue(result.needsManagerReview)
        }
    }
}
