package com.alsalama.custody

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
    LoginAccount(charArrayOf(52, 54, 57, 49, 51, 54).concatToString(), "عمرو سامي", "مدير التطبيق", listOf(
        "إدارة المستخدمين والصلاحيات", "جميع العهد والمصروفات",
        "التقارير والإعدادات", "تسجيل مصروفاته ومصروفات أحمد حسن"
    )),
    LoginAccount(charArrayOf(52, 52, 49, 52, 48, 48).concatToString(), "سامي هنداوي", "الإدارة المالية", listOf(
        "لوحة متابعة العهد الأربعة", "تمويل العهد",
        "تسوية وإغلاق العهد", "الإجماليات والأرصدة والتقارير"
    )),
    LoginAccount(charArrayOf(49, 48, 48, 49, 48, 48).concatToString(), "عبدالله", "مسؤول العمليات", listOf(
        "تسجيل مصروفاته ومصروفات أحمد حسن", "الاطلاع على جميع العهد والمصروفات"
    )),
    LoginAccount(charArrayOf(50, 48, 48, 50, 48, 48).concatToString(), "أحمد حمدي", "موظف", listOf(
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
    Surface(modifier = Modifier.fillMaxSize(), color = PageBackground) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            FactoryLogoMark()
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "أهلاً بيك يا ${account.displayName}",
                color = SalamaDark, fontSize = 23.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(account.roleTitle, color = SalamaGreen, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("الأقسام المخصصة لحسابك", color = SalamaDark, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(14.dp))
                    account.sections.forEach { section ->
                        Text("•  $section", color = Color(0xFF52675E), fontSize = 14.sp,
                            lineHeight = 23.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
            Text("دي خريطة الأقسام الخاصة بدورك. هنفعّل وظائف كل قسم ضمن مراحل تطوير التطبيق.",
                color = MutedText, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 21.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onSignOut, shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SalamaGreen)) {
                Text("تغيير الحساب")
            }
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
