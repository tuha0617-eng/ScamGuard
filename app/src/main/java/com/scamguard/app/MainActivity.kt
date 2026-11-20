package com.scamguard.app

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ScanHistoryItem(
    val timestamp: Long,
    val value: String
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // XÓA LỊCH SỬ CŨ MỘT LẦN ĐỂ ĐẢM BẢO
        // KHÔNG CÒN BẢN GHI CŨ CÓ THỂ CHỨA SMS GỐC

        val settings = getSharedPreferences(
            "scamguard_settings",
            Context.MODE_PRIVATE
        )

        val migrationKey = "history_privacy_migrated_v1"

        if (!settings.getBoolean(migrationKey, false)) {

            getSharedPreferences(
                "scamguard_history",
                Context.MODE_PRIVATE
            ).edit().clear().apply()

            settings.edit()
                .putBoolean(migrationKey, true)
                .apply()
        }

        setContent {
            ScamGuardTestScreen()
        }
    }

    @Composable
    fun ScamGuardTestScreen() {

        // 1. LƯU TRẠNG THÁI BẢO VỆ

        val preferences = remember {
            getSharedPreferences(
                "scamguard_settings",
                Context.MODE_PRIVATE
            )
        }

        var protectionEnabled by remember {
            mutableStateOf(
                preferences.getBoolean(
                    "protection_enabled",
                    true
                )
            )
        }

        // 2. ĐIỀU HƯỚNG MÀN HÌNH

        var showSecurityScreen by remember {
            mutableStateOf(false)
        }

        var showSmsScreen by remember {
            mutableStateOf(false)
        }

        var showHistoryScreen by remember {
            mutableStateOf(false)
        }

        var historyRefreshKey by remember {
            mutableStateOf(0)
        }

        var showDeleteDialog by remember {
            mutableStateOf(false)
        }

        // 3. DỮ LIỆU THÔNG BÁO CHỜ CẤP QUYỀN

        var pendingRisk by remember {
            mutableStateOf("HIGH")
        }

        var pendingReason by remember {
            mutableStateOf("")
        }

        var pendingRecommendation by remember {
            mutableStateOf("")
        }

        // 4. XIN QUYỀN THÔNG BÁO

        val permissionLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->

                if (isGranted && protectionEnabled) {

                    NotificationHelper.showRiskNotification(
                        context = this@MainActivity,
                        riskLevel = pendingRisk,
                        reason = pendingReason,
                        recommendation = pendingRecommendation
                    )

                    Toast.makeText(
                        this@MainActivity,
                        "Đã gửi thông báo ScamGuard",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        this@MainActivity,
                        "Bạn chưa cấp quyền thông báo hoặc đã tắt bảo vệ",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        // 5. HÀM GỬI THÔNG BÁO

        fun sendTestNotification(
            riskLevel: String,
            reason: String,
            recommendation: String
        ) {

            if (!protectionEnabled) {

                Toast.makeText(
                    this@MainActivity,
                    "Bạn đang tắt chế độ bảo vệ",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            val hasPermission =
                NotificationHelper.hasNotificationPermission(
                    this@MainActivity
                )

            if (!hasPermission) {

                pendingRisk = riskLevel
                pendingReason = reason
                pendingRecommendation = recommendation

                if (Build.VERSION.SDK_INT >= 33) {

                    permissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )

                } else {

                    Toast.makeText(
                        this@MainActivity,
                        "Vui lòng bật thông báo trong cài đặt ứng dụng",
                        Toast.LENGTH_LONG
                    ).show()
                }

                return
            }

            NotificationHelper.showRiskNotification(
                context = this@MainActivity,
                riskLevel = riskLevel,
                reason = reason,
                recommendation = recommendation
            )
        }

        // 6. MÀN HÌNH KIỂM TRA SMS

        if (showSmsScreen) {

            SmsScreen(
                onBack = {
                    showSmsScreen = false
                },

                onAnalyze = { smsText ->

                    // GỌI BACKEND API
                    // SMS ĐƯỢC GỬI LÊN BACKEND ĐỂ PHÂN TÍCH

                    val analysis = BackendApi.analyzeSms(smsText)

                    val riskLevel = analysis.riskLevel
                    val reason = analysis.reasons
                    val recommendation = analysis.recommendation

                    Toast.makeText(
                        this@MainActivity,
                        "Backend phân tích: $riskLevel",
                        Toast.LENGTH_LONG
                    ).show()

                    // 7. LƯU LỊCH SỬ AN TOÀN
                    // KHÔNG LƯU SMS GỐC

                    val historyPreferences =
                        getSharedPreferences(
                            "scamguard_history",
                            Context.MODE_PRIVATE
                        )

                    val historyKey =
                        "scan_${System.currentTimeMillis()}"

                    val historyValue = buildString {

                        append("Mức độ: $riskLevel\n")
                        append("Điểm rủi ro: ${analysis.riskScore}\n")
                        append("Lý do: $reason")
                    }

                    // CHỈ LƯU KẾT QUẢ PHÂN TÍCH

                    historyPreferences.edit()
                        .putString(historyKey, historyValue)
                        .apply()

                    historyRefreshKey++

                    // 8. GỬI THÔNG BÁO THEO KẾT QUẢ BACKEND

                    when (riskLevel.uppercase()) {

                        "HIGH" -> {
                            sendTestNotification(
                                riskLevel = "HIGH",
                                reason = reason,
                                recommendation = recommendation
                            )
                        }

                        "MEDIUM" -> {
                            sendTestNotification(
                                riskLevel = "MEDIUM",
                                reason = reason,
                                recommendation = recommendation
                            )
                        }

                        "LOW" -> {
                            sendTestNotification(
                                riskLevel = "LOW",
                                reason = reason,
                                recommendation = recommendation
                            )
                        }

                        else -> {
                            Toast.makeText(
                                this@MainActivity,
                                "Backend trả về mức độ rủi ro không hợp lệ",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    // TRẢ KẾT QUẢ BACKEND VỀ SMS SCREEN

                    analysis
                }
            )

        } else if (showSecurityScreen) {

            // 9. MÀN HÌNH BẢO MẬT

            SecurityScreen(
                onBack = {
                    showSecurityScreen = false
                }
            )

        } else if (showHistoryScreen) {

            // 10. MÀN HÌNH LỊCH SỬ

            val historyPreferences = remember {
                getSharedPreferences(
                    "scamguard_history",
                    Context.MODE_PRIVATE
                )
            }

            val historyItems = remember(historyRefreshKey) {

                historyPreferences.all.entries
                    .filter { it.key.startsWith("scan_") }
                    .mapNotNull { entry ->

                        val value = entry.value as? String
                            ?: return@mapNotNull null

                        val timestamp = entry.key
                            .removePrefix("scan_")
                            .toLongOrNull() ?: 0L

                        ScanHistoryItem(
                            timestamp = timestamp,
                            value = value
                        )
                    }
                    .sortedByDescending { it.timestamp }
            }

            if (showDeleteDialog) {

                AlertDialog(
                    onDismissRequest = {
                        showDeleteDialog = false
                    },

                    title = {
                        Text("Xóa lịch sử?")
                    },

                    text = {
                        Text(
                            "Bạn có chắc chắn muốn xóa toàn bộ lịch sử phân tích SMS không?"
                        )
                    },

                    confirmButton = {

                        TextButton(
                            onClick = {

                                historyPreferences.edit()
                                    .clear()
                                    .apply()

                                historyRefreshKey++
                                showDeleteDialog = false

                                Toast.makeText(
                                    this@MainActivity,
                                    "Đã xóa lịch sử",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Text("Xóa")
                        }
                    },

                    dismissButton = {

                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                            }
                        ) {
                            Text("Hủy")
                        }
                    }
                )
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {

                    Text(
                        text = "LỊCH SỬ QUÉT SMS",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Các tin nhắn đã được ScamGuard phân tích",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text("Tổng số lần quét: ${historyItems.size}")

                        TextButton(
                            onClick = {
                                historyRefreshKey++
                            }
                        ) {
                            Text("Làm mới")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (historyItems.isEmpty()) {

                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Text(
                                text = "Chưa có lịch sử phân tích SMS.",
                                modifier = Modifier.padding(20.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                    } else {

                        historyItems.forEach { item ->

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {

                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {

                                    val riskLevel = when {

                                        item.value.contains(
                                            "Mức độ: HIGH",
                                            ignoreCase = true
                                        ) -> "HIGH"

                                        item.value.contains(
                                            "Mức độ: MEDIUM",
                                            ignoreCase = true
                                        ) -> "MEDIUM"

                                        item.value.contains(
                                            "Mức độ: LOW",
                                            ignoreCase = true
                                        ) -> "LOW"

                                        else -> "UNKNOWN"
                                    }

                                    val riskColor = when (riskLevel) {

                                        "HIGH" ->
                                            MaterialTheme.colorScheme.error

                                        "MEDIUM" ->
                                            MaterialTheme.colorScheme.tertiary

                                        "LOW" ->
                                            MaterialTheme.colorScheme.primary

                                        else ->
                                            MaterialTheme.colorScheme.onSurface
                                    }

                                    Text(
                                        text = "MỨC ĐỘ: $riskLevel",
                                        color = riskColor,
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Spacer(
                                        modifier = Modifier.height(6.dp)
                                    )

                                    val dateText =
                                        if (item.timestamp > 0L) {

                                            SimpleDateFormat(
                                                "dd/MM/yyyy HH:mm:ss",
                                                Locale.getDefault()
                                            ).format(Date(item.timestamp))

                                        } else {
                                            "Không xác định"
                                        }

                                    Text(
                                        text = "Thời gian: $dateText",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    Spacer(
                                        modifier = Modifier.height(8.dp)
                                    )

                                    Text(
                                        text = item.value,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            showDeleteDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = historyItems.isNotEmpty()
                    ) {
                        Text("XÓA TOÀN BỘ LỊCH SỬ")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            showHistoryScreen = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("QUAY LẠI")
                    }
                }
            }

        } else {

            // 11. MÀN HÌNH CHÍNH SCAMGUARD

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),

                    horizontalAlignment = Alignment.CenterHorizontally,

                    verticalArrangement = Arrangement.Center
                ) {

                    Text(
                        text = "SCAMGUARD",
                        style = MaterialTheme.typography.headlineLarge
                    )

                    Text(
                        text = "Hệ thống cảnh báo lừa đảo",
                        modifier = Modifier.padding(top = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 12. CHẾ ĐỘ BẢO VỆ

                    Text("Chế độ bảo vệ")

                    Switch(
                        checked = protectionEnabled,

                        onCheckedChange = { isEnabled ->

                            protectionEnabled = isEnabled

                            preferences.edit()
                                .putBoolean(
                                    "protection_enabled",
                                    isEnabled
                                )
                                .apply()

                            val message =
                                if (isEnabled) {
                                    "Bạn đã bật chế độ bảo vệ"
                                } else {
                                    "Bạn đã tắt chế độ bảo vệ"
                                }

                            Toast.makeText(
                                this@MainActivity,
                                message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )

                    Text(
                        text = if (protectionEnabled) {
                            "Đang bật bảo vệ"
                        } else {
                            "Đang tắt bảo vệ"
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 13. THỬ CẢNH BÁO HIGH

                    Button(
                        onClick = {

                            sendTestNotification(
                                riskLevel = "HIGH",
                                reason = "Phát hiện dấu hiệu lừa đảo hoặc chiếm đoạt tài sản.",
                                recommendation = "Không nhấn vào liên kết, không cung cấp mật khẩu hoặc mã OTP."
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("THỬ CẢNH BÁO HIGH")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 14. THỬ CẢNH BÁO MEDIUM

                    Button(
                        onClick = {

                            sendTestNotification(
                                riskLevel = "MEDIUM",
                                reason = "Nội dung có dấu hiệu đáng ngờ, cần kiểm tra kỹ.",
                                recommendation = "Hãy xác minh người gửi trước khi thực hiện giao dịch."
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("THỬ CẢNH BÁO MEDIUM")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 15. THỬ CẢNH BÁO LOW

                    Button(
                        onClick = {

                            sendTestNotification(
                                riskLevel = "LOW",
                                reason = "Chưa phát hiện dấu hiệu lừa đảo rõ ràng.",
                                recommendation = "Bạn vẫn nên kiểm tra người gửi và nội dung trước khi thực hiện yêu cầu."
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("THỬ KẾT QUẢ LOW")
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 16. MỞ MÀN HÌNH SMS

                    Button(
                        onClick = {
                            showSmsScreen = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("KIỂM TRA TIN NHẮN SMS")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 17. MỞ MÀN HÌNH LỊCH SỬ

                    Button(
                        onClick = {
                            showHistoryScreen = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("LỊCH SỬ QUÉT SMS")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 18. MỞ MÀN HÌNH BẢO MẬT

                    Button(
                        onClick = {
                            showSecurityScreen = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("BẢO MẬT & QUYỀN RIÊNG TƯ")
                    }
                }
            }
        }
    }
}