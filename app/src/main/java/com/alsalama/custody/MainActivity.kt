package com.alsalama.custody

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SalamaGreen = Color(0xFF176B55)
private val SalamaDark = Color(0xFF173D35)
private val PageBackground = Color(0xFFF5F7F6)

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
private fun LoginScreen() {
    var accessCode by remember { mutableStateOf("") }
    var showCode by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = PageBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(SalamaGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "س",
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "مصنع السلامة",
                color = SalamaDark,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "نظام العهد والمصروفات",
                color = Color(0xFF687A74),
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(34.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "تسجيل الدخول",
                        color = SalamaDark,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "أدخل رمز الدخول المخصص لك للمتابعة",
                        color = Color(0xFF718079),
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
                        placeholder = { Text("أدخل رمز الدخول") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (showCode) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showCode = !showCode }) {
                                Text(if (showCode) "إخفاء" else "إظهار", color = SalamaGreen)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SalamaGreen,
                            cursorColor = SalamaGreen
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            message = if (accessCode.isBlank()) {
                                "من فضلك أدخل رمز الدخول أولًا"
                            } else {
                                "سيتم تفعيل تسجيل الدخول بعد ربط نظام التحقق الآمن"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SalamaGreen)
                    ) {
                        Text(
                            text = "دخول",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (message != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = message.orEmpty(),
                            color = Color(0xFF9A6A22),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "يتم تحديد صلاحياتك تلقائيًا حسب حسابك",
                color = Color(0xFF718079),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "نظام داخلي لإدارة العهد والمصروفات",
                color = Color(0xFF9AA7A2),
                fontSize = 11.sp
            )
        }
    }
}
