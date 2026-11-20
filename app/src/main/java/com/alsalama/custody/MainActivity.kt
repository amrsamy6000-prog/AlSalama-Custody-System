package com.alsalama.custody

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SalamaGreen = Color(0xFF176B55)
private val SalamaDark = Color(0xFF153F35)
private val PageBackground = Color(0xFFF2F6F4)
private val MutedText = Color(0xFF718079)

private data class LoginAccount(
    val code: String,
    val displayName: String,
    val roleTitle: String,
    val sections: List<String>
)

// رمز الدخول يحدد واجهة الموظف فقط.
private val loginAccounts = listOf(
    LoginAccount(charArrayOf(52.toChar(), 54.toChar(), 57.toChar(), 49.toChar(), 51.toChar(), 54.toChar()).concatToString(), "عمرو سامي", "مدير التطبيق", listOf(
        "إدارة المستخدمين والصلاحيات", "جميع العهد والمصروفات",
        "التقارير والإعدادات", "تسجيل مصروفاته ومصروفات أحمد حسن"
    )),
    LoginAccount(charArrayOf(52.toChar(), 52.toChar(), 49.toChar(), 52.toChar(), 48.toChar(), 48.toChar()).concatToString(), "سامي هنداوي", "الإدارة المالية", listOf(
        "لوحة متابعة العهد الأربعة", "تمويل العهد",
        "تسوية وإغلاق العهد", "الإجماليات والأرصدة والتقارير"
    )),
    LoginAccount(charArrayOf(49.toChar(), 48.toChar(), 48.toChar(), 49.toChar(), 48.toChar(), 48.toChar()).concatToString(), "عبدالله", "مسؤول العمليات", listOf(
        "تسجيل مصروفاته ومصروفات أحمد حسن", "الاطلاع على جميع العهد والمصروفات"
    )),
    LoginAccount(charArrayOf(50.toChar(), 48.toChar(), 48.toChar(), 50.toChar(), 48.toChar(), 48.toChar()).concatToString(), "أحمد حمدي", "موظف", listOf(
        "تسجيل مصروفاته الشخصية", "الاطلاع على بياناته المالية فقط"
    ))
)

private fun findAccountByCode(code: String): LoginAccount? =
    loginAccounts.firstOrNull { it.code == code }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    LoginScreen()
                }
            }
        }
    }
}

@Composable
private fun SignedInScreen(account: LoginAccount, onSignOut: () -> Unit) {
    var openedSection by remember(account.code) { mutableStateOf<String?>(null) }
    var professionalMode by remember(account.code) { mutableStateOf(false) }
    var selectedPerson by remember(account.code) { mutableStateOf<String?>(null) }
    var showAllExpenses by remember(account.code) { mutableStateOf(false) }
    var showSettlement by remember(account.code) { mutableStateOf(false) }

    val isFinance = account.displayName == "سامي هنداوي"
    val isExpenseUser = account.displayName == "عمرو سامي" ||
        account.displayName == "عبدالله" || account.displayName == "أحمد حمدي"

    // Android system Back navigates through the in-app screens.
    androidx.activity.compose.BackHandler(
        enabled = openedSection != null || selectedPerson != null || showSettlement || professionalMode
    ) {
        when {
            showSettlement -> showSettlement = false
            openedSection != null -> openedSection = null
            selectedPerson != null -> {
                selectedPerson = null
                showAllExpenses = false
            }
            professionalMode -> professionalMode = false
        }
    }

    if (showSettlement) {
        SimplePlaceholderPage(
            title = "تصفية عهدة ${selectedPerson.orEmpty()}",
            description = "هنا هتظهر مراجعة العهدة وإجمالي التمويل والمصروف والمتبقي قبل اعتماد التصفية.",
            onBack = { showSettlement = false }
        )
        return
    }

    if (openedSection != null) {
        SimplePlaceholderPage(
            title = openedSection.orEmpty(),
            description = "هنضيف وظائف القسم وبياناته الفعلية في المرحلة التالية، مع الحفاظ على سهولة الاستخدام.",
            onBack = { openedSection = null }
        )
        return
    }

    if (isFinance) {
        if (selectedPerson != null) {
            PersonExpensesScreen(
                person = selectedPerson.orEmpty(),
                showAll = showAllExpenses,
                onShowAll = { showAllExpenses = true },
                onSettle = { showSettlement = true },
                onBack = { selectedPerson = null; showAllExpenses = false },
                onSignOut = onSignOut
            )
            return
        }
        Surface(modifier = Modifier.fillMaxSize(), color = PageBackground) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopBar(onSignOut)
                Spacer(modifier = Modifier.height(28.dp))
                Text("متابعة العُهد", color = SalamaDark, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("اختار الاسم لمراجعة مصروفاته", color = MutedText, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(24.dp))
                listOf("عمرو", "عبدالله", "أحمد حسن", "أحمد حمدي").forEach { person ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp).clickable {
                            selectedPerson = person
                            showAllExpenses = false
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 25.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(50.dp).background(Color(0xFFE6F1EC), RoundedCornerShape(15.dp)),
                                contentAlignment = Alignment.Center
                            ) { Text("👤", fontSize = 24.sp) }
                            Text(
                                person,
                                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                                color = SalamaDark, fontSize = 21.sp, fontWeight = FontWeight.Bold
                            )
                            Text("‹", color = SalamaGreen, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        return
    }

    if (isExpenseUser && !professionalMode) {
        ExpenseQuickEntryScreen(
            account = account,
            onProfessional = { professionalMode = true },
            onSignOut = onSignOut
        )
        return
    }

    Surface(modifier = Modifier.fillMaxSize(), color = PageBackground) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBar(onSignOut)
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = SalamaDark)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("أهلاً بيك يا ${account.displayName}", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(account.roleTitle, color = Color(0xFFD1E8DC), fontSize = 15.sp)
                    }
                    FactoryLogoMark()
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("الوضع الاحترافي", modifier = Modifier.fillMaxWidth(), color = SalamaDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(14.dp))
            account.sections.forEachIndexed { index, section ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 11.dp).clickable { openedSection = section },
                    shape = RoundedCornerShape(17.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${index + 1}", color = SalamaGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(section, modifier = Modifier.weight(1f).padding(horizontal = 15.dp), color = SalamaDark, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("‹", color = SalamaGreen, fontSize = 26.sp)
                    }
                }
            }
            if (isExpenseUser) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { professionalMode = false },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SalamaGreen)
                ) { Text("العودة لتسجيل المصروف", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun TopBar(onSignOut: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onSignOut) {
            Text("تغيير الحساب", color = SalamaGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ExpenseQuickEntryScreen(
    account: LoginAccount,
    onProfessional: () -> Unit,
    onSignOut: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    Surface(modifier = Modifier.fillMaxSize(), color = PageBackground) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            TopBar(onSignOut)
            Spacer(modifier = Modifier.height(30.dp))
            Text("تسجيل مصروف", color = SalamaDark, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("حساب ${account.displayName}", color = MutedText, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(28.dp))
            OutlinedTextField(
                value = details, onValueChange = { details = it },
                modifier = Modifier.fillMaxWidth(), minLines = 2,
                label = { Text("بيان المصروف") }, placeholder = { Text("اكتب المصروف باختصار") },
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = amount, onValueChange = { amount = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("المبلغ بالجنيه") }, placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = { },
                enabled = false,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SalamaGreen)
            ) { Text("تسجيل المصروف", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(44.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProfessional() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFFE7F1EC), RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚙", color = SalamaGreen, fontSize = 29.sp)
                    }
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text("الوضع الاحترافي", color = SalamaDark, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text("باقي الخصائص والإعدادات", color = MutedText, fontSize = 13.sp)
                    }
                    Text("‹", color = SalamaGreen, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PersonExpensesScreen(
    person: String,
    showAll: Boolean,
    onShowAll: () -> Unit,
    onSettle: () -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = PageBackground) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                TopBar(onSignOut)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "مصروفات $person",
                    color = SalamaDark,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                FinancialSummaryCard()
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    if (showAll) "كل مصروفات العهدة الحالية" else "مصروفات اليوم",
                    color = SalamaDark,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 190.dp)
                        .background(Color.White, RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "لا توجد مصروفات لعرضها حاليًا",
                        color = MutedText,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 25.sp
                    )
                }
            }

            // Keep the actions above Android's gesture/navigation area.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onShowAll,
                    modifier = Modifier.weight(1f).heightIn(min = 68.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SalamaDark),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("☷", fontSize = 25.sp, lineHeight = 27.sp)
                        Text(
                            "عرض كل المصروفات",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
                Button(
                    onClick = onSettle,
                    modifier = Modifier.weight(1f).heightIn(min = 68.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB7791F)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✓", fontSize = 21.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "تصفية العهدة",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinancialSummaryCard() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactFinancialValue(
            label = "قيمة العهدة",
            value = "—",
            background = Color(0xFFEAF0F7),
            valueColor = Color(0xFF315A83),
            modifier = Modifier.weight(1f)
        )
        CompactFinancialValue(
            label = "المصروفات",
            value = "—",
            background = Color(0xFFFFF2E5),
            valueColor = Color(0xFFB66A20),
            modifier = Modifier.weight(1f)
        )
        CompactFinancialValue(
            label = "المتبقي",
            value = "—",
            background = Color(0xFFE8F4EC),
            valueColor = Color(0xFF23734D),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CompactFinancialValue(
    label: String,
    value: String,
    background: Color,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(background, RoundedCornerShape(14.dp))
            .padding(horizontal = 7.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            color = SalamaDark,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            value,
            color = valueColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun SimplePlaceholderPage(title: String, description: String, onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = PageBackground) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(18.dp))
            Text(title, color = SalamaDark, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Text(description, modifier = Modifier.padding(22.dp), color = MutedText, fontSize = 16.sp, lineHeight = 25.sp, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onBack) { Text("رجوع", color = SalamaGreen, fontSize = 16.sp) }
        }
    }
}

@Composable
private fun FactoryLogoMark() {
    Box(
        modifier = Modifier
            .size(88.dp)
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(Color(0xFF21866A), Color(0xFF104C3E))
                ),
                shape = RoundedCornerShape(26.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(54.dp)) {
            val w = size.width
            val h = size.height
            val white = Color.White
            val stroke = 3.2.dp.toPx()
            // Factory roof and chimney
            drawLine(white, Offset(w * .12f, h * .43f), Offset(w * .43f, h * .20f), stroke)
            drawLine(white, Offset(w * .43f, h * .20f), Offset(w * .68f, h * .40f), stroke)
            drawLine(white, Offset(w * .68f, h * .40f), Offset(w * .88f, h * .27f), stroke)
            drawLine(white, Offset(w * .88f, h * .27f), Offset(w * .88f, h * .82f), stroke)
            drawLine(white, Offset(w * .12f, h * .43f), Offset(w * .12f, h * .82f), stroke)
            drawLine(white, Offset(w * .12f, h * .82f), Offset(w * .88f, h * .82f), stroke)
            drawLine(white, Offset(w * .72f, h * .30f), Offset(w * .72f, h * .08f), stroke)
            // Factory windows and door
            drawLine(white, Offset(w * .27f, h * .57f), Offset(w * .27f, h * .68f), stroke)
            drawLine(white, Offset(w * .43f, h * .57f), Offset(w * .43f, h * .68f), stroke)
            drawLine(white, Offset(w * .59f, h * .57f), Offset(w * .59f, h * .68f), stroke)
            drawLine(white, Offset(w * .76f, h * .57f), Offset(w * .76f, h * .68f), stroke)
        }
    }
}

@Composable
private fun LoginScreen() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("custody_app", Context.MODE_PRIVATE) }
    var signedInAccount by remember {
        mutableStateOf(loginAccounts.firstOrNull { it.code == preferences.getString("selected_code", null) })
    }

    if (signedInAccount != null) {
        SignedInScreen(account = signedInAccount!!, onSignOut = {
            preferences.edit().remove("selected_code").apply()
            signedInAccount = null
        })
        return
    }
    var accessCode by remember { mutableStateOf("") }
    var showCode by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current
    val whatsappUrl = "https://wa.me/201212663313"

    Surface(modifier = Modifier.fillMaxSize(), color = PageBackground) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color(0xFFE1F0E9), PageBackground)
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                FactoryLogoMark()
                Spacer(modifier = Modifier.height(15.dp))
                Text(
                    text = "مصنع السلامة",
                    color = SalamaDark,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "إدارة العُهد والمصروفات",
                    color = SalamaGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "كل حساباتك ومصروفاتك في مكان واحد",
                    color = MutedText,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "أهلاً بيك 👋",
                            color = SalamaDark,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(7.dp))
                        Text(
                            text = "اكتب رمز الدخول بتاعك علشان تدخل على حسابك.",
                            color = MutedText,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(22.dp))
                        Text(
                            text = "رمز الدخول",
                            color = SalamaDark,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = accessCode,
                            onValueChange = {
                                accessCode = it
                                message = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("اكتب الرمز هنا") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = if (showCode) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                TextButton(onClick = { showCode = !showCode }) {
                                    Text(if (showCode) "إخفاء" else "إظهار", color = SalamaGreen)
                                }
                            },
                            shape = RoundedCornerShape(15.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SalamaGreen,
                                unfocusedBorderColor = Color(0xFFD7E2DC),
                                cursorColor = SalamaGreen
                            )
                        )
                        Spacer(modifier = Modifier.height(17.dp))
                        Button(
                            onClick = {
                                if (accessCode.isBlank()) {
                                    message = "اكتب رمز الدخول الأول، وبعدها دوس دخول."
                                } else {
                                    val account = findAccountByCode(accessCode.trim())
                                    if (account != null) {
                                        preferences.edit().putString("selected_code", account.code).apply()
                                        signedInAccount = account
                                        accessCode = ""
                                        message = null
                                    } else {
                                        message = "رمز الدخول غير صحيح. راجع الرمز وحاول مرة تانية."
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SalamaGreen)
                        ) {
                            Text(
                                "دخول لحسابي",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (message != null) {
                            Spacer(modifier = Modifier.height(13.dp))
                            Text(
                                text = message.orEmpty(),
                                color = Color(0xFF94651F),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F1EC)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("💡", fontSize = 21.sp)
                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text(
                                text = "تدخل إزاي؟",
                                color = SalamaDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "اكتب الكود اللي استلمته من عمرو هنداوي، وبعدها دوس على «دخول لحسابي». ومتعرّفش الكود بتاعك لأي حد.",
                                color = Color(0xFF52675E),
                                fontSize = 13.sp,
                                lineHeight = 21.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "نسيت رمز الدخول بتاعك؟",
                    color = MutedText,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = { uriHandler.openUri(whatsappUrl) }
                ) {
                    Text(
                        text = "💬 كلم عمرو من هنا على واتساب",
                        color = SalamaGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "مصنع السلامة  •  نظام داخلي",
                    color = Color(0xFF9AA9A2),
                    fontSize = 11.sp
                )
            }
        }
    }
}
