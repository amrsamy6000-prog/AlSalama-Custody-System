package com.alsalama.custody

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

private val FinanceGreen = Color(0xFF176B55)
private val FinanceDark = Color(0xFF153F35)
private val FinanceMuted = Color(0xFF718079)
private val FinancePeople = listOf("عمرو", "عبدالله", "أحمد حسن", "أحمد حمدي")

private data class FinanceExpense(val employeeName: String, val amount: Double)
private data class CustodyMovement(val employeeName: String, val type: String, val amount: Double)

private fun financeEmployeeAliases(name: String): List<String> = when (name) {
    "عمرو" -> listOf("عمرو", "عمرو سامي")
    else -> listOf(name)
}

private fun financeMoney(value: Double): String =
    BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

@Composable
internal fun FinanceDashboardScreen(
    onOpenPerson: (String) -> Unit,
    onOpenFinancialActions: () -> Unit,
    onSignOut: () -> Unit
) {
    var expenses by remember { mutableStateOf<List<FinanceExpense>>(emptyList()) }
    var movements by remember { mutableStateOf<List<CustodyMovement>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        val expenseListener: ListenerRegistration = db.collection("expenses")
            .addSnapshotListener { snapshot, expenseError ->
                if (expenseError != null) {
                    error = expenseError.localizedMessage ?: "تعذر تحميل المصروفات"
                    loading = false
                } else if (snapshot != null) {
                    expenses = snapshot.documents.filter { it.getString("status") == "active" }.map {
                        FinanceExpense(it.getString("employeeName").orEmpty(), it.getDouble("amount") ?: 0.0)
                    }
                    loading = false
                    error = null
                }
            }
        val movementListener: ListenerRegistration = db.collection("custody_transactions")
            .addSnapshotListener { snapshot, movementError ->
                if (movementError != null) {
                    error = movementError.localizedMessage ?: "تعذر تحميل حركات العهد"
                    loading = false
                } else if (snapshot != null) {
                    movements = snapshot.documents.filter { it.getString("status") == "active" }.map {
                        CustodyMovement(
                            it.getString("employeeName").orEmpty(),
                            it.getString("type").orEmpty(),
                            it.getDouble("amount") ?: 0.0
                        )
                    }
                    loading = false
                    error = null
                }
            }
        onDispose { expenseListener.remove(); movementListener.remove() }
    }

    val totalFunding = movements.filter { it.type == "funding" }.sumOf { it.amount }
    val totalReturns = movements.filter { it.type == "return" }.sumOf { it.amount }
    val totalExpenses = expenses.sumOf { it.amount }
    val remaining = totalFunding - totalExpenses - totalReturns
    val spentRatio = if (totalFunding > 0.0) (totalExpenses / totalFunding).coerceIn(0.0, 1.0).toFloat() else 0f

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF2F6F4)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("أهلاً يا سامي", color = FinanceDark, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                    Text("الإدارة المالية", color = FinanceMuted, fontSize = 12.sp)
                }
                TextButton(onClick = onSignOut) {
                    Text("تغيير الحساب", color = FinanceMuted, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
            Card(
                Modifier.fillMaxWidth().clickable(onClick = onOpenFinancialActions),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = FinanceDark),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(horizontal = 9.dp, vertical = 7.dp)) {
                    Text("ملخص حركة الأموال", color = Color(0xFFD7E9E0), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        FinanceMetric("العهد", totalFunding, Color.White, Modifier.weight(1f))
                        FinanceMetric("المصروفات", totalExpenses, Color(0xFFFFD5B7), Modifier.weight(1f))
                        FinanceMetric("المتبقي", remaining, Color(0xFFB7F0CB), Modifier.weight(1f))
                    }
                }
            }

            if (loading) {
                Spacer(Modifier.height(8.dp))
                Text("جارٍ تحديث البيانات...", color = FinanceMuted, fontSize = 11.sp)
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text("تعذر تحديث بعض البيانات: " + error, color = Color(0xFFB45309), fontSize = 11.sp)
            }

            Spacer(Modifier.height(17.dp))
            Text("الموظفون", color = FinanceDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(9.dp))
            FinancePeople.forEach { person ->
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 9.dp).clickable { onOpenPerson(person) },
                    shape = RoundedCornerShape(15.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 17.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(person, modifier = Modifier.weight(1f), color = FinanceDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("‹", color = FinanceGreen, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
internal fun FinancialActionsScreen(
    onFundCustody: () -> Unit,
    onOpenSettlements: () -> Unit,
    onOpenReports: () -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF2F6F4)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("رجوع", color = FinanceGreen, fontWeight = FontWeight.Bold) }
                TextButton(onClick = onSignOut) { Text("تغيير الحساب", color = FinanceMuted) }
            }
            Spacer(Modifier.height(12.dp))
            Text("الإجراءات المالية", color = FinanceDark, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Text("إدارة التمويل والتسويات والتقارير", color = FinanceMuted, fontSize = 13.sp)
            Spacer(Modifier.height(22.dp))
            FinanceAction("＋", "تمويل عهدة", "تسجيل مبلغ جديد مع الموظف", onFundCustody)
            FinanceAction("⇄", "التسويات وإغلاق العهد", "مراجعة المتبقي ورد الأرصدة", onOpenSettlements)
            FinanceAction("▤", "التقارير المالية", "التقارير اليومية والشهرية والسنوية", onOpenReports)
        }
    }
}

@Composable
private fun FinanceMetric(label: String, amount: Double, valueColor: Color, modifier: Modifier) {
    Column(
        modifier.background(Color(0x22FFFFFF), RoundedCornerShape(10.dp))
            .padding(horizontal = 5.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Color(0xFFD7E9E0), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(financeMoney(amount), color = valueColor, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Text("جنيه", color = Color(0xFFD7E9E0), fontSize = 9.sp)
    }
}

@Composable
private fun FinanceAction(icon: String, title: String, description: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(bottom = 9.dp).clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(43.dp).background(Color(0xFFE6F1EC), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Text(icon, color = FinanceGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, color = FinanceDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(description, color = FinanceMuted, fontSize = 11.sp)
            }
            Text("‹", color = FinanceGreen, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun FundingEntryScreen(createdBy: String, onBack: () -> Unit) {
    val people = listOf("عمرو سامي", "عبدالله", "أحمد حسن", "أحمد حمدي")
    var selectedPerson by remember { mutableStateOf(people.first()) }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val amountValue = amount.trim().replace(',', '.').toDoubleOrNull()

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF2F6F4)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            TextButton(onClick = onBack) { Text("رجوع", color = FinanceGreen, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(10.dp))
            Text("تمويل عهدة جديدة", color = FinanceDark, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Text("سجّل المبلغ الذي تم تسليمه للموظف", color = FinanceMuted, fontSize = 14.sp)
            Spacer(Modifier.height(22.dp))
            Text("صاحب العهدة", color = FinanceDark, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(9.dp))
            people.forEach { person ->
                val selected = selectedPerson == person
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 7.dp).clickable { selectedPerson = person },
                    shape = RoundedCornerShape(13.dp),
                    colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFFE2F1E8) else Color.White)
                ) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (selected) "●" else "○", color = FinanceGreen, fontSize = 17.sp)
                        Text(person, modifier = Modifier.padding(start = 10.dp), color = FinanceDark, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(value = amount, onValueChange = { amount = it; message = null }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("مبلغ التمويل بالجنيه") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it; message = null }, modifier = Modifier.fillMaxWidth(), minLines = 2, label = { Text("بيان التمويل (اختياري)") })
            if (message != null) { Spacer(Modifier.height(10.dp)); Text(message.orEmpty(), color = FinanceGreen, fontSize = 13.sp) }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    val value = amountValue
                    if (value == null || value <= 0.0) {
                        message = "اكتب مبلغ صحيح أكبر من صفر."
                    } else {
                        saving = true
                        val movement = hashMapOf<String, Any>(
                            "type" to "funding", "employeeName" to selectedPerson, "amount" to value,
                            "currency" to "EGP", "notes" to notes.trim(), "createdBy" to createdBy,
                            "createdAt" to FieldValue.serverTimestamp(), "createdAtLocal" to Date(), "status" to "active"
                        )
                        FirebaseFirestore.getInstance().collection("custody_transactions")
                            .document(UUID.randomUUID().toString()).set(movement)
                            .addOnSuccessListener {
                                saving = false; amount = ""; notes = ""; message = "تم تسجيل تمويل العهدة بنجاح."
                            }
                            .addOnFailureListener { error ->
                                saving = false; message = "تعذر تأكيد التمويل: " + (error.localizedMessage ?: "راجع الاتصال")
                            }
                    }
                },
                enabled = !saving && amountValue != null && amountValue > 0.0,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinanceGreen),
                shape = RoundedCornerShape(15.dp)
            ) { Text(if (saving) "جارٍ حفظ التمويل..." else "تأكيد تمويل العهدة", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}
