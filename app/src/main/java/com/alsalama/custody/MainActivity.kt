package com.alsalama.custody

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.delay

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
    private var showResumeSplash by mutableStateOf(false)

    override fun onStart() {
        super.onStart()
        // Persist the date before showing: only the first foreground opening of each day qualifies.
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val preferences = getSharedPreferences("alsalama_daily_intro", MODE_PRIVATE)
        val lastIntroDate = preferences.getString("last_intro_date", null)
        if (lastIntroDate != today) {
            preferences.edit().putString("last_intro_date", today).apply()
            showResumeSplash = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep Firestore's persistent on-device cache enabled for offline reads and queued writes.
        FirebaseFirestore.getInstance().firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Keep the existing screen composed underneath the temporary overlay,
                        // preserving its navigation and input state while the app resumes.
                        LoginScreen()
                        if (showResumeSplash) {
                            ResumeSplashOverlay(onFinished = { showResumeSplash = false })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumeSplashOverlay(onFinished: () -> Unit) {
    val context = LocalContext.current

    // Keep the original artwork untouched; only animate its opacity.
    DisposableEffect(context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val canPlay = audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL &&
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) > 0
        if (canPlay) playSalamaIntroSound()
        onDispose { }
    }

    val imageAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        imageAlpha.animateTo(
            1f,
            animationSpec = tween(
                durationMillis = 650,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        )
        // Let the complete original artwork remain clearly visible.
        delay(2_300)
        imageAlpha.animateTo(
            0f,
            animationSpec = tween(
                durationMillis = 600,
                easing = androidx.compose.animation.core.FastOutLinearInEasing
            )
        )
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF071D17)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.alsalama_resume_splash_2x),
            contentDescription = "مصنع السلامة - م/ سامي هنداوي",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // No zoom, crop, translation, or color changes: the source image stays intact.
                    alpha = imageAlpha.value
                }
        )
    }
}

@Composable
private fun SignedInScreen(account: LoginAccount, onSignOut: () -> Unit) {
    var openedSection by remember(account.code) { mutableStateOf<String?>(null) }
    var professionalMode by remember(account.code) { mutableStateOf(false) }
    var selectedPerson by remember(account.code) { mutableStateOf<String?>(null) }
    var showAllExpenses by remember(account.code) { mutableStateOf(false) }
    var showSettlement by remember(account.code) { mutableStateOf(false) }
    var showFunding by remember(account.code) { mutableStateOf(false) }
    var showFinancialActions by remember(account.code) { mutableStateOf(false) }

    val isFinance = account.displayName == "سامي هنداوي"
    val isExpenseUser = account.displayName == "عمرو سامي" ||
        account.displayName == "عبدالله" || account.displayName == "أحمد حمدي"

    // Android system Back navigates through the in-app screens.
    androidx.activity.compose.BackHandler(
        enabled = openedSection != null || selectedPerson != null || showSettlement || showFunding || showFinancialActions || professionalMode
    ) {
        when {
            showSettlement -> showSettlement = false
            showFunding -> showFunding = false
            showFinancialActions -> showFinancialActions = false
            openedSection != null -> openedSection = null
            selectedPerson != null -> {
                selectedPerson = null
                showAllExpenses = false
            }
            professionalMode -> professionalMode = false
        }
    }

    if (showSettlement) {
        SettlementReviewScreen(
            person = selectedPerson.orEmpty(),
            settledBy = account.displayName,
            onBack = { showSettlement = false },
            onSettled = {
                showSettlement = false
                selectedPerson = null
                showAllExpenses = false
            }
        )
        return
    }

    if (openedSection != null) {
        if (isFinance && openedSection == "التقارير المالية") {
            FinancialReportsScreen(onBack = { openedSection = null }, onSignOut = onSignOut)
        } else if (isFinance && openedSection == "التسويات وإغلاق العهد") {
            SettlementHubScreen(
                onSelectPerson = { person ->
                    selectedPerson = person
                    showSettlement = true
                    openedSection = null
                },
                onBack = { openedSection = null },
                onSignOut = onSignOut
            )
        } else {
            SimplePlaceholderPage(
                title = openedSection.orEmpty(),
                description = "هنضيف وظائف القسم وبياناته الفعلية في المرحلة التالية، مع الحفاظ على سهولة الاستخدام.",
                onBack = { openedSection = null }
            )
        }
        return
    }

    if (isFinance) {
        if (showFinancialActions) {
            FinancialActionsScreen(
                onFundCustody = { showFinancialActions = false; showFunding = true },
                onOpenSettlements = { showFinancialActions = false; openedSection = "التسويات وإغلاق العهد" },
                onOpenReports = { showFinancialActions = false; openedSection = "التقارير المالية" },
                onBack = { showFinancialActions = false },
                onSignOut = onSignOut
            )
            return
        }
        if (showFunding) {
            FundingEntryScreen(
                createdBy = account.displayName,
                onBack = { showFunding = false }
            )
            return
        }
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
        FinanceDashboardScreen(
            onOpenPerson = { person ->
                selectedPerson = person
                showAllExpenses = false
            },
            onOpenFinancialActions = { showFinancialActions = true },
            onSignOut = onSignOut
        )
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

private fun playSalamaSuccessSound() {
    // Original in-app success chime: three soft, bright notes.
    // It is synthesized locally and does not use the phone's notification sounds.
    val sampleRate = 44_100
    val durationMs = 620
    val sampleCount = sampleRate * durationMs / 1_000
    val samples = ShortArray(sampleCount)
    val notes = listOf(
        Triple(659.25, 0.00, 0.22),
        Triple(783.99, 0.16, 0.24),
        Triple(987.77, 0.34, 0.28)
    )

    for (i in samples.indices) {
        val time = i.toDouble() / sampleRate
        var value = 0.0
        for ((frequency, start, length) in notes) {
            val localTime = time - start
            if (localTime >= 0.0 && localTime < length) {
                val attack = (localTime / 0.018).coerceIn(0.0, 1.0)
                val release = ((length - localTime) / 0.075).coerceIn(0.0, 1.0)
                val envelope = minOf(attack, release)
                val fundamental = kotlin.math.sin(2.0 * Math.PI * frequency * localTime)
                val shimmer = 0.22 * kotlin.math.sin(2.0 * Math.PI * frequency * 2.01 * localTime)
                value += (fundamental + shimmer) * envelope * 0.24
            }
        }
        samples[i] = (value.coerceIn(-0.95, 0.95) * Short.MAX_VALUE).toInt().toShort()
    }

    val audioFormat = AudioFormat.Builder()
        .setSampleRate(sampleRate)
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()
    val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    val track = AudioTrack.Builder()
        .setAudioAttributes(audioAttributes)
        .setAudioFormat(audioFormat)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .setBufferSizeInBytes(samples.size * 2)
        .build()

    try {
        track.write(samples, 0, samples.size)
        track.play()
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            } catch (_: IllegalStateException) {
            } finally {
                track.release()
            }
        }, durationMs.toLong() + 180L)
    } catch (_: Exception) {
        track.release()
    }
}

private fun playSalamaIntroSound() {
    // One clean, modern tonal chime: a soft attack, a single sustained pitch,
    // and a smooth release. Generated locally; no external audio asset needed.
    val sampleRate = 44_100
    val durationMs = 1_250
    val totalSamples = sampleRate * durationMs / 1_000
    val samples = ShortArray(totalSamples)
    val frequency = 660.0 // E5: warm, clear, and unobtrusive

    for (i in samples.indices) {
        val time = i.toDouble() / sampleRate
        val attack = (time / 0.055).coerceIn(0.0, 1.0)
        val release = ((durationMs / 1_000.0 - time) / 0.32).coerceIn(0.0, 1.0)
        val envelope = attack * release
        val fundamental = sin(2.0 * Math.PI * frequency * time)
        val subtleOvertone = sin(2.0 * Math.PI * frequency * 2.0 * time) * 0.08
        val value = (fundamental + subtleOvertone) * envelope * 0.22
        samples[i] = (value.coerceIn(-0.85, 0.85) * Short.MAX_VALUE).toInt().toShort()
    }

    val audioTrack = AudioTrack(
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build(),
        AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build(),
        samples.size * 2,
        AudioTrack.MODE_STATIC,
        AudioManager.AUDIO_SESSION_ID_GENERATE
    )

    audioTrack.write(samples, 0, samples.size)
    audioTrack.setNotificationMarkerPosition(samples.size)
    audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
        override fun onMarkerReached(track: AudioTrack) {
            track.release()
        }

        override fun onPeriodicNotification(track: AudioTrack) = Unit
    })
    audioTrack.play()
}

@Composable
private fun ExpenseQuickEntryScreen(
    account: LoginAccount,
    onProfessional: () -> Unit,
    onSignOut: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    var lastSuccessMessage by remember { mutableStateOf<String?>(null) }
    var isPendingSync by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var feedbackAnimationKey by remember { mutableStateOf(0) }
    val feedbackScale = remember { Animatable(0.72f) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(saveMessage) {
        if (saveMessage != null) {
            delay(10_000L)
            saveMessage = null
        }
    }
    LaunchedEffect(feedbackAnimationKey) {
        if (feedbackAnimationKey > 0) {
            feedbackScale.snapTo(0.72f)
            feedbackScale.animateTo(1.12f, tween(220))
            feedbackScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        }
    }
    val amountValue = amount.trim().replace(',', '.').toDoubleOrNull()
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
                modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) saveMessage = null }, minLines = 2,
                label = { Text("بيان المصروف") }, placeholder = { Text("اكتب المصروف باختصار") },
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = amount, onValueChange = { amount = it },
                modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) saveMessage = null }, singleLine = true,
                label = { Text("المبلغ بالجنيه") }, placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = {
                    keyboardController?.hide()
                    val cleanDetails = details.trim()
                    val cleanAmount = amountValue
                    if (cleanDetails.isNotEmpty() && cleanAmount != null && cleanAmount > 0.0) {
                        isSaving = true
                        val offlineMessages = listOf(
                            "تمام، المصروف اتحفظ على جهازك وهيتبعت أول ما النت يرجع ☁️",
                            "ولا يهمك.. حفظناه عندك، وأول ما النت يرجع هيتبعت تلقائيًا 📲",
                            "المصروف في الأمان على جهازك.. والمزامنة أول ما الاتصال يرجع 🔄",
                            "كله تمام! المصروف اتحفظ محليًا ومستني الإنترنت 🌥️"
                        )
                        saveMessage = offlineMessages.random()
                        isPendingSync = true
                        saveFailed = false
                        feedbackAnimationKey += 1
                        playSalamaSuccessSound()
                        val classification = classifyExpense(cleanDetails)
                        val expense = hashMapOf<String, Any>(
                            "description" to cleanDetails,
                            "amount" to cleanAmount,
                            "currency" to "EGP",
                            "employeeName" to account.displayName,
                            "employeeCode" to account.code,
                            "category" to classification.category,
                            "needsManagerReview" to classification.needsManagerReview,
                            "reviewStatus" to if (classification.needsManagerReview) "pending" else "not_required",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "createdAtLocal" to Date(),
                            "status" to "active"
                        )
                        // A client-generated ID makes retries idempotent: the same expense
                        // is written to the same document instead of creating duplicates.
                        val expenseId = UUID.randomUUID().toString()
                        FirebaseFirestore.getInstance()
                            .collection("expenses")
                            .document(expenseId)
                            .set(expense)
                            .addOnSuccessListener {
                                isPendingSync = false
                                saveFailed = false
                                val successMessages = listOf(
                                    "عاش يا بطل! المصروف اتسجل بنجاح 👏",
                                    "تمام كده.. شغل مظبوط ✅",
                                    "تسلم إيدك.. العملية تمت بنجاح 🌟",
                                    "واحدة كمان في السليم! 😎",
                                    "كده تمام يا معلم.. المصروف اتسجل ❤️",
                                    "برافو عليك! كل حاجة تمام 💪",
                                    "المصروف اتسجل.. يومك إنجازات 🎯",
                                    "فل الفل! العملية تمت بنجاح ✨"
                                )
                                val availableMessages = successMessages.filterNot { it == lastSuccessMessage }
                                saveMessage = availableMessages.random()
                                lastSuccessMessage = saveMessage
                                feedbackAnimationKey += 1
                            }
                            .addOnFailureListener { error ->
                                isPendingSync = false
                                saveFailed = true
                                saveMessage = "مقدرناش نأكد تسجيل المصروف. راجع الاتصال وحاول تاني."
                            }
                        // Firestore persists this write locally and queues it while offline.
                        // Do not keep the form locked while waiting for the server acknowledgement.
                        isSaving = false
                        details = ""
                        amount = ""
                    }
                },
                enabled = !isSaving && details.isNotBlank() && amountValue != null && amountValue > 0.0,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SalamaGreen)
            ) {
                Text(
                    if (isSaving) "جارٍ تسجيل المصروف..." else "تسجيل المصروف",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            AnimatedVisibility(
                visible = saveMessage != null,
                enter = fadeIn() + scaleIn()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            saveFailed -> Color(0xFFFFEEEE)
                            isPendingSync -> Color(0xFFFFF6E5)
                            else -> Color(0xFFE8F5EE)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .scale(feedbackScale.value)
                                .background(
                                    when {
                                        saveFailed -> Color(0xFFF9DADA)
                                        isPendingSync -> Color(0xFFFFE7B5)
                                        else -> Color(0xFFCDEBD8)
                                    },
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when {
                                    saveFailed -> "!"
                                    isPendingSync -> "☁"
                                    else -> "✓"
                                },
                                color = when {
                                    saveFailed -> Color(0xFFB42318)
                                    isPendingSync -> Color(0xFF9A6700)
                                    else -> Color(0xFF176B55)
                                },
                                fontSize = 27.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = saveMessage.orEmpty(),
                            modifier = Modifier.weight(1f),
                            color = when {
                                saveFailed -> Color(0xFFB42318)
                                isPendingSync -> Color(0xFF805600)
                                else -> SalamaDark
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
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

private data class ExpenseRecord(
    val id: String,
    val description: String,
    val amount: Double,
    val category: String,
    val needsManagerReview: Boolean,
    val createdAt: Date?,
    val pendingSync: Boolean = false
)

private fun formatExpenseAmount(amount: Double): String =
    BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString()

private fun formatArabicDateTime(date: Date): String {
    val dateText = SimpleDateFormat("yyyy/MM/dd - hh:mm", Locale.US).format(date)
    val period = if (Calendar.getInstance().apply { time = date }.get(Calendar.HOUR_OF_DAY) < 12) {
        "صباحًا"
    } else {
        "مساءً"
    }
    return dateText + " " + period
}

private fun displayExpenseCategory(category: String): String = when (category) {
    "صيانة ومصنعية" -> "صيانة سيارات"
    "خامات وقطع غيار ومستلزمات تشغيل", "أدوات وسلامة ومستلزمات عامة" -> "مستلزمات ورشة"
    else -> category.ifBlank { "أخرى" }
}

private fun expenseCategoryIcon(category: String): String = when (displayExpenseCategory(category)) {
    "مأكولات ومشروبات" -> "🥪"
    "مواصلات وانتقالات" -> "🚚"
    "وقود" -> "⛽"
    "صيانة سيارات" -> "🚗"
    "مستلزمات ورشة" -> "🛠️"
    "كارتات ورسوم عبور" -> "🛣️"
    "إكراميات" -> "💵"
    else -> "📌"
}

@Composable
private fun ExpenseEntryCard(item: ExpenseRecord, isNew: Boolean) {
    val categoryName = displayExpenseCategory(item.category)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNew) Color(0xFFF0F8F3) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isNew) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(17.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.description,
                    modifier = Modifier.weight(1f),
                    color = SalamaDark,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 27.sp
                )
                if (isNew) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        "جديد",
                        modifier = Modifier
                            .background(Color(0xFFD9EFE2), RoundedCornerShape(9.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        color = Color(0xFF176B45),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                formatExpenseAmount(item.amount) + " جنيه",
                color = SalamaGreen,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            item.createdAt?.let { date ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(formatArabicDateTime(date), color = MutedText, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(9.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        categoryName,
                        color = MutedText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(expenseCategoryIcon(categoryName), fontSize = 15.sp)
                    if (item.pendingSync) {
                        Text(
                            "بانتظار المزامنة",
                            modifier = Modifier
                                .background(Color(0xFFE7EEF9), RoundedCornerShape(7.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            color = Color(0xFF315A91),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (item.needsManagerReview) {
                        Text(
                            "يحتاج مراجعة",
                            modifier = Modifier
                                .background(Color(0xFFFFF1D8), RoundedCornerShape(7.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            color = Color(0xFF9A6718),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}



@Composable
private fun FinancialReportsScreen(onBack: () -> Unit, onSignOut: () -> Unit) {
    val db = remember { FirebaseFirestore.getInstance() }
    var funded by remember { mutableStateOf(0.0) }
    var spent by remember { mutableStateOf(0.0) }
    var fundingCount by remember { mutableStateOf(0) }
    var expenseCount by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(db) {
        val fundingListener = db.collection("custody_funding")
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) error = exception.localizedMessage
                else if (snapshot != null) {
                    funded = snapshot.documents.sumOf { it.getDouble("amount") ?: 0.0 }
                    fundingCount = snapshot.size()
                }
            }
        val expenseListener = db.collection("expenses")
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) error = exception.localizedMessage
                else if (snapshot != null) {
                    spent = snapshot.documents.sumOf { it.getDouble("amount") ?: 0.0 }
                    expenseCount = snapshot.size()
                }
            }
        onDispose {
            fundingListener.remove()
            expenseListener.remove()
        }
    }

    Surface(Modifier.fillMaxSize(), color = PageBackground) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            TopBar(onSignOut)
            Text("التقارير المالية", color = SalamaDark, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Text("ملخص مباشر للحركات النشطة المسجلة على النظام", color = MutedText, fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
            CompactFinancialValue("إجمالي التمويل", String.format(Locale.US, "%,.2f ج.م", funded),
                Color(0xFFEAF0F7), Color(0xFF315A83), Modifier.fillMaxWidth())
            Spacer(Modifier.height(9.dp))
            CompactFinancialValue("إجمالي المصروفات", String.format(Locale.US, "%,.2f ج.م", spent),
                Color(0xFFFFF2E5), Color(0xFFB66A20), Modifier.fillMaxWidth())
            Spacer(Modifier.height(9.dp))
            CompactFinancialValue("الرصيد الحالي", String.format(Locale.US, "%,.2f ج.م", funded - spent),
                Color(0xFFE8F4EC), if (funded - spent < 0) Color(0xFFB42318) else Color(0xFF23734D),
                Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(17.dp)) {
                    Text("عدد الحركات النشطة", color = SalamaDark, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("عمليات التمويل: $fundingCount", color = MutedText)
                    Text("عمليات المصروفات: $expenseCount", color = MutedText)
                }
            }
            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text("تعذر تحديث التقرير: " + error.orEmpty(), color = Color(0xFFB45309), fontSize = 13.sp)
            }
            Spacer(Modifier.height(20.dp))
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("رجوع", color = SalamaGreen)
            }
        }
    }
}

@Composable
private fun SettlementHubScreen(
    onSelectPerson: (String) -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    val people = listOf("عمرو سامي", "عبدالله", "أحمد حسن", "أحمد حمدي")
    Surface(Modifier.fillMaxSize(), color = PageBackground) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            TopBar(onSignOut)
            Text("التسويات وإغلاق العهد", color = SalamaDark, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text("اختر العهدة لمراجعة أرقامها قبل الإغلاق", color = MutedText, fontSize = 14.sp)
            Spacer(Modifier.height(18.dp))
            people.forEach { person ->
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onSelectPerson(person) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(person, color = SalamaDark, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("مراجعة التسوية  ‹", color = SalamaGreen, fontSize = 13.sp)
                    }
                }
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("رجوع", color = SalamaGreen)
            }
        }
    }
}

@Composable
private fun SettlementReviewScreen(
    person: String,
    settledBy: String,
    onBack: () -> Unit,
    onSettled: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    var fundingDocs by remember(person) { mutableStateOf<List<com.google.firebase.firestore.DocumentSnapshot>>(emptyList()) }
    var expenseDocs by remember(person) { mutableStateOf<List<com.google.firebase.firestore.DocumentSnapshot>>(emptyList()) }
    var confirming by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val names = if (person == "عمرو سامي") listOf("عمرو سامي", "عمرو") else listOf(person)
    val funded = fundingDocs.sumOf { it.getDouble("amount") ?: 0.0 }
    val spent = expenseDocs.sumOf { it.getDouble("amount") ?: 0.0 }
    val remaining = funded - spent

    DisposableEffect(person) {
        val fundingListener = db.collection("custody_funding")
            .whereEqualTo("personName", person)
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) fundingDocs = snapshot.documents
                else if (error != null) message = error.localizedMessage
            }
        val expenseListener = db.collection("expenses")
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    expenseDocs = snapshot.documents.filter { it.getString("employeeName") in names }
                } else if (error != null) message = error.localizedMessage
            }
        onDispose {
            fundingListener.remove()
            expenseListener.remove()
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { if (!saving) confirming = false },
            title = { Text("تأكيد إغلاق العهدة") },
            text = {
                Text(
                    "سيتم إغلاق " + fundingDocs.size + " عملية تمويل و" + expenseDocs.size +
                        " عملية مصروف للعهدة: $person. المتبقي الحالي: " +
                        String.format(Locale.US, "%,.2f ج.م", remaining) +
                        ". لا يمكن اعتبار العملية قابلة للتراجع من داخل التطبيق."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !saving,
                    onClick = {
                        saving = true
                        val batch = db.batch()
                        fundingDocs.forEach { batch.update(it.reference, "status", "settled") }
                        expenseDocs.forEach { batch.update(it.reference, "status", "settled") }
                        val settlementRef = db.collection("custody_settlements").document()
                        batch.set(settlementRef, hashMapOf<String, Any>(
                            "personName" to person,
                            "fundedAmount" to funded,
                            "spentAmount" to spent,
                            "remainingAmount" to remaining,
                            "fundingCount" to fundingDocs.size,
                            "expenseCount" to expenseDocs.size,
                            "settledBy" to settledBy,
                            "settledAt" to FieldValue.serverTimestamp(),
                            "settledAtLocal" to Date(),
                            "status" to "closed"
                        ))
                        batch.commit()
                            .addOnSuccessListener {
                                saving = false
                                confirming = false
                                onSettled()
                            }
                            .addOnFailureListener { exception ->
                                saving = false
                                message = "فشل إغلاق العهدة: " + (exception.localizedMessage ?: "خطأ غير معروف")
                                confirming = false
                            }
                    }
                ) { Text(if (saving) "جارٍ الإغلاق…" else "تأكيد الإغلاق", color = Color(0xFFB42318)) }
            },
            dismissButton = {
                TextButton(enabled = !saving, onClick = { confirming = false }) { Text("إلغاء") }
            }
        )
    }

    Surface(Modifier.fillMaxSize(), color = PageBackground) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("مراجعة تصفية العهدة", color = SalamaDark, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text(person, color = MutedText, fontSize = 16.sp)
            Spacer(Modifier.height(20.dp))
            CompactFinancialValue("إجمالي التمويل", String.format(Locale.US, "%,.2f ج.م", funded),
                Color(0xFFEAF0F7), Color(0xFF315A83), Modifier.fillMaxWidth())
            Spacer(Modifier.height(9.dp))
            CompactFinancialValue("إجمالي المصروفات", String.format(Locale.US, "%,.2f ج.م", spent),
                Color(0xFFFFF2E5), Color(0xFFB66A20), Modifier.fillMaxWidth())
            Spacer(Modifier.height(9.dp))
            CompactFinancialValue("المتبقي", String.format(Locale.US, "%,.2f ج.م", remaining),
                Color(0xFFE8F4EC), if (remaining < 0) Color(0xFFB42318) else Color(0xFF23734D),
                Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Text("عدد التمويلات: " + fundingDocs.size, color = MutedText)
            Text("عدد المصروفات: " + expenseDocs.size, color = MutedText)
            if (message != null) {
                Spacer(Modifier.height(10.dp))
                Text(message.orEmpty(), color = Color(0xFFB42318), fontSize = 13.sp)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { confirming = true },
                enabled = !saving && (fundingDocs.isNotEmpty() || expenseDocs.isNotEmpty()),
                modifier = Modifier.fillMaxWidth().height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB7791F))
            ) { Text("مراجعة الأرقام ثم إغلاق العهدة", fontWeight = FontWeight.Bold) }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع دون إغلاق", color = SalamaGreen) }
        }
    }
}

private data class ExpenseClassification(
    val category: String,
    val needsManagerReview: Boolean
)

private fun classifyExpense(description: String): ExpenseClassification {
    val value = description.lowercase(Locale("ar"))
    val category = when {
        listOf("سولار", "بنزين", "زيت", "وقود").any { value.contains(it) } -> "وقود وزيوت"
        listOf("صيانة", "تصليح", "إصلاح", "قطع غيار").any { value.contains(it) } -> "صيانة"
        listOf("أكل", "طعام", "وجبة", "مشروبات").any { value.contains(it) } -> "إعاشة"
        listOf("نقل", "مواصلات", "شحن", "تحميل").any { value.contains(it) } -> "نقل وشحن"
        listOf("عدد", "خامات", "صاج", "حديد", "مستلزمات").any { value.contains(it) } -> "مستلزمات وخامات"
        else -> "مصروفات متنوعة"
    }
    return ExpenseClassification(
        category = category,
        needsManagerReview = category == "مصروفات متنوعة" || description.trim().length > 140
    )
}

private data class CustodyTotals(
    val person: String,
    val funded: Double,
    val spent: Double
) {
    val remaining: Double get() = funded - spent
}

@Composable
private fun FinanceDashboardScreen(
    onOpenPerson: (String) -> Unit,
    onOpenFinancialActions: () -> Unit,
    onSignOut: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    var fundingTotals by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var expenseTotals by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    val people = listOf("عمرو سامي", "عبدالله", "أحمد حسن", "أحمد حمدي")

    DisposableEffect(db) {
        val fundingListener = db.collection("custody_funding")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    loadError = error.localizedMessage ?: "تعذر تحميل التمويلات"
                } else if (snapshot != null) {
                    fundingTotals = snapshot.documents
                        .filter { it.getString("status") == "active" }
                        .groupBy { it.getString("personName").orEmpty() }
                        .mapValues { entry -> entry.value.sumOf { it.getDouble("amount") ?: 0.0 } }
                    loading = false
                }
            }
        val expenseListener = db.collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    loadError = error.localizedMessage ?: "تعذر تحميل المصروفات"
                } else if (snapshot != null) {
                    expenseTotals = snapshot.documents
                        .filter { it.getString("status") == "active" }
                        .groupBy { it.getString("employeeName").orEmpty() }
                        .mapValues { entry -> entry.value.sumOf { it.getDouble("amount") ?: 0.0 } }
                    loading = false
                }
            }
        onDispose {
            fundingListener.remove()
            expenseListener.remove()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = PageBackground) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            TopBar(onSignOut)
            Spacer(Modifier.height(10.dp))
            Text("الإدارة المالية", color = SalamaDark, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
            Text("متابعة العهد والتمويل والمصروفات", color = MutedText, fontSize = 14.sp)
            Spacer(Modifier.height(18.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SalamaDark)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("إجمالي العهد النشطة", color = Color(0xFFD7E9E0), fontSize = 14.sp)
                    Spacer(Modifier.height(7.dp))
                    Text(
                        String.format(Locale.US, "%,.2f ج.م", fundingTotals.values.sum()),
                        color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "المصروفات النشطة: " + String.format(Locale.US, "%,.2f ج.م", expenseTotals.values.sum()),
                        color = Color(0xFFD7E9E0), fontSize = 13.sp
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("العهد الأربع", color = SalamaDark, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (loading) Text("جارٍ التحديث…", color = MutedText, fontSize = 12.sp)
            }
            if (loadError != null) {
                Spacer(Modifier.height(8.dp))
                Text("تعذر تحديث بعض البيانات: " + loadError.orEmpty(), color = Color(0xFFB45309), fontSize = 13.sp)
            }
            people.forEach { person ->
                val totals = CustodyTotals(
                    person,
                    fundingTotals[person] ?: 0.0,
                    expenseTotals[person] ?: 0.0
                )
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        .clickable { onOpenPerson(person) },
                    shape = RoundedCornerShape(17.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(15.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(person, color = SalamaDark, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("التفاصيل  ‹", color = SalamaGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                            CompactFinancialValue(
                                "التمويل", String.format(Locale.US, "%,.0f", totals.funded),
                                Color(0xFFEAF0F7), Color(0xFF315A83), Modifier.weight(1f)
                            )
                            CompactFinancialValue(
                                "المصروف", String.format(Locale.US, "%,.0f", totals.spent),
                                Color(0xFFFFF2E5), Color(0xFFB66A20), Modifier.weight(1f)
                            )
                            CompactFinancialValue(
                                "المتبقي", String.format(Locale.US, "%,.0f", totals.remaining),
                                Color(0xFFE8F4EC), if (totals.remaining < 0) Color(0xFFB42318) else Color(0xFF23734D),
                                Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onOpenFinancialActions,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SalamaGreen)
            ) {
                Text("التمويل والتسويات والتقارير", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FinancialActionsScreen(
    onFundCustody: () -> Unit,
    onOpenSettlements: () -> Unit,
    onOpenReports: () -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(Modifier.fillMaxSize(), color = PageBackground) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            TopBar(onSignOut)
            Text("إدارة العهد المالية", color = SalamaDark, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            Text("اختر العملية المطلوبة", color = MutedText, fontSize = 14.sp)
            Spacer(Modifier.height(22.dp))
            val actions = listOf(
                Triple("تمويل عهدة جديدة", "إضافة مبلغ إلى عهدة أحد العاملين", onFundCustody),
                Triple("التسويات وإغلاق العهد", "مراجعة التمويل والمصروف والمتبقي قبل التسوية", onOpenSettlements),
                Triple("التقارير المالية", "ملخص التمويلات والمصروفات والأرصدة", onOpenReports)
            )
            actions.forEach { action ->
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { action.third() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(action.first, color = SalamaDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text(action.second, color = MutedText, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("رجوع للوحة الإدارة", color = SalamaGreen)
            }
        }
    }
}

@Composable
private fun FundingEntryScreen(
    createdBy: String,
    onBack: () -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val people = listOf("عمرو سامي", "عبدالله", "أحمد حسن", "أحمد حمدي")
    var selectedPerson by remember { mutableStateOf<String?>(null) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val amountValue = amount.trim().replace(',', '.').toDoubleOrNull()

    Surface(Modifier.fillMaxSize(), color = PageBackground) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            Text("تمويل عهدة", color = SalamaDark, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text("تسجيل مبلغ جديد مع الاحتفاظ بسجل التمويلات", color = MutedText, fontSize = 14.sp)
            Spacer(Modifier.height(22.dp))
            Text("صاحب العهدة", color = SalamaDark, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            people.forEach { person ->
                val selected = selectedPerson == person
                TextButton(
                    onClick = { selectedPerson = person; message = null },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = if (selected) Color.White else SalamaGreen,
                        containerColor = if (selected) SalamaGreen else Color.White
                    )
                ) {
                    Text(if (selected) "✓  " + person else person, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(5.dp))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it; message = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("مبلغ التمويل بالجنيه") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ملاحظات (اختياري)") },
                minLines = 2,
                shape = RoundedCornerShape(14.dp)
            )
            if (message != null) {
                Spacer(Modifier.height(10.dp))
                Text(message.orEmpty(), color = if (saving) MutedText else SalamaGreen, fontSize = 14.sp)
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    val person = selectedPerson
                    val value = amountValue
                    if (person == null) {
                        message = "اختار صاحب العهدة الأول."
                    } else if (value == null || value <= 0.0) {
                        message = "اكتب مبلغ تمويل صحيح أكبر من صفر."
                    } else {
                        saving = true
                        message = "جارٍ حفظ التمويل…"
                        val record = hashMapOf<String, Any>(
                            "personName" to person,
                            "amount" to value,
                            "note" to note.trim(),
                            "createdBy" to createdBy,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "createdAtLocal" to Date(),
                            "status" to "active"
                        )
                        db.collection("custody_funding").add(record)
                            .addOnSuccessListener {
                                saving = false
                                amount = ""
                                note = ""
                                selectedPerson = null
                                message = "تم تسجيل التمويل بنجاح."
                            }
                            .addOnFailureListener { error ->
                                saving = false
                                message = "تعذر حفظ التمويل: " + (error.localizedMessage ?: "راجع الاتصال والصلاحيات")
                            }
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SalamaGreen)
            ) {
                Text(if (saving) "جارٍ الحفظ…" else "حفظ التمويل", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("رجوع", color = SalamaGreen)
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
    val context = LocalContext.current
    val seenPreferences = remember(person) {
        context.getSharedPreferences("seen_expenses_$person", Context.MODE_PRIVATE)
    }
    var seenExpenseIds by remember(person) {
        mutableStateOf(seenPreferences.getStringSet("seen_ids", emptySet())?.toSet() ?: emptySet())
    }
    var visibleExpenseIds by remember(person) { mutableStateOf<Set<String>>(emptySet()) }
    var expenses by remember(person) { mutableStateOf<List<ExpenseRecord>>(emptyList()) }
    var loading by remember(person) { mutableStateOf(true) }
    var errorText by remember(person) { mutableStateOf<String?>(null) }
    var fundedAmount by remember(person) { mutableStateOf(0.0) }

    DisposableEffect(person) {
        val fundingRegistration = FirebaseFirestore.getInstance()
            .collection("custody_funding")
            .whereEqualTo("personName", person)
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    fundedAmount = snapshot.documents.sumOf { it.getDouble("amount") ?: 0.0 }
                }
            }
        onDispose { fundingRegistration.remove() }
    }

    DisposableEffect(person) {
        val names = if (person == "عمرو") listOf("عمرو سامي", "عمرو") else listOf(person)
        val registration: ListenerRegistration = FirebaseFirestore.getInstance()
            .collection("expenses")
            .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    errorText = error.localizedMessage ?: "تعذر تحميل المصروفات"
                    loading = false
                } else if (snapshot != null) {
                    expenses = snapshot.documents
                        .filter { it.getString("employeeName") in names && it.getString("status") == "active" }
                        .map { doc ->
                            ExpenseRecord(
                                id = doc.id,
                                description = doc.getString("description").orEmpty(),
                                amount = doc.getDouble("amount") ?: 0.0,
                                category = doc.getString("category").orEmpty(),
                                needsManagerReview = doc.getBoolean("needsManagerReview") ?: false,
                                createdAt = (doc.getTimestamp("createdAt") ?: doc.getTimestamp("createdAtLocal"))?.toDate(),
                                pendingSync = doc.metadata.hasPendingWrites()
                            )
                        }
                        .sortedByDescending { it.createdAt ?: Date(0) }
                    errorText = null
                    loading = false
                }
            }
        onDispose {
            registration.remove()
            seenPreferences.edit()
                .putStringSet("seen_ids", (seenExpenseIds + visibleExpenseIds).toSet())
                .apply()
        }
    }

    val today = Calendar.getInstance()
    val todayExpenses = expenses.filter { item ->
        item.createdAt?.let { date ->
            val c = Calendar.getInstance().apply { time = date }
            c.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                c.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        } ?: false
    }
    val shown = if (showAll) expenses else todayExpenses
    val newExpenses = shown.filter { it.id !in seenExpenseIds }
    val previousExpenses = shown.filter { it.id in seenExpenseIds }
    androidx.compose.runtime.SideEffect {
        visibleExpenseIds = shown.map { it.id }.toSet()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = PageBackground) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                TopBar(onSignOut)
                Spacer(modifier = Modifier.height(16.dp))
                Text("مصروفات $person", color = SalamaDark, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                FinancialSummaryCard(
                    fundedAmount = fundedAmount,
                    spentAmount = expenses.sumOf { it.amount }
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(if (showAll) "كل مصروفات العهدة الحالية" else "مصروفات اليوم",
                    color = SalamaDark, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                when {
                    loading -> Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp)
                            .background(Color.White, RoundedCornerShape(20.dp)).padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("جارٍ تحميل المصروفات...", color = MutedText) }
                    errorText != null -> Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp)
                            .background(Color.White, RoundedCornerShape(20.dp)).padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("تعذر تحميل المصروفات: $errorText", color = Color(0xFFB45309), textAlign = TextAlign.Center) }
                    shown.isEmpty() -> Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp)
                            .background(Color.White, RoundedCornerShape(20.dp)).padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (showAll) "لا توجد مصروفات مسجلة لهذه العهدة حتى الآن" else "لا توجد مصروفات مسجلة اليوم",
                            color = MutedText, fontSize = 17.sp, textAlign = TextAlign.Center, lineHeight = 25.sp)
                    }
                    else -> Column(
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(11.dp)
                    ) {
                        if (newExpenses.isNotEmpty()) {
                            Text(
                                "مصروفات جديدة (" + newExpenses.size + ")",
                                color = SalamaGreen,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            newExpenses.forEach { item ->
                                ExpenseEntryCard(item = item, isNew = true)
                            }
                        }
                        if (previousExpenses.isNotEmpty()) {
                            if (newExpenses.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(5.dp))
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(1.dp)
                                        .background(Color(0xFFD5DFD9))
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                            }
                            Text(
                                "مصروفات سابقة (" + previousExpenses.size + ")",
                                color = MutedText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            previousExpenses.forEach { item ->
                                ExpenseEntryCard(item = item, isNew = false)
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onShowAll, modifier = Modifier.weight(1f).heightIn(min = 68.dp),
                    shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = SalamaDark),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp, vertical = 8.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("☷", fontSize = 25.sp, lineHeight = 27.sp)
                        Text("عرض كل المصروفات", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, lineHeight = 16.sp)
                    }
                }
                Button(onClick = onSettle, modifier = Modifier.weight(1f).heightIn(min = 68.dp),
                    shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB7791F)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp, vertical = 8.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✓", fontSize = 21.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
                        Text("تصفية العهدة", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FinancialSummaryCard(fundedAmount: Double, spentAmount: Double) {
    val remaining = fundedAmount - spentAmount
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactFinancialValue(
            label = "قيمة العهدة",
            value = String.format(Locale.US, "%,.0f", fundedAmount),
            background = Color(0xFFEAF0F7),
            valueColor = Color(0xFF315A83),
            modifier = Modifier.weight(1f)
        )
        CompactFinancialValue(
            label = "المصروفات",
            value = String.format(Locale.US, "%,.0f", spentAmount),
            background = Color(0xFFFFF2E5),
            valueColor = Color(0xFFB66A20),
            modifier = Modifier.weight(1f)
        )
        CompactFinancialValue(
            label = "المتبقي",
            value = String.format(Locale.US, "%,.0f", remaining),
            background = Color(0xFFE8F4EC),
            valueColor = if (remaining < 0) Color(0xFFB42318) else Color(0xFF23734D),
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
