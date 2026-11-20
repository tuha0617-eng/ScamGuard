
package com.scamguard.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SmsScreen(
    onBack: () -> Unit,
    onAnalyze: suspend (String) -> BackendScanResult
) {
    var smsContent by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<BackendScanResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val purple = Color(0xFF6D50A8)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "KIỂM TRA TIN NHẮN SMS",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "ScamGuard giúp bạn kiểm tra tin nhắn có dấu hiệu lừa đảo."
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Hướng dẫn: Sao chép nội dung tin nhắn và dán vào ô bên dưới."
        )

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = smsContent,
            onValueChange = {
                smsContent = it
                result = null
                errorMessage = null
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            placeholder = {
                Text("Nội dung tin nhắn SMS")
            },
            maxLines = 7,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Số ký tự: ${smsContent.length}")

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (smsContent.isNotBlank() && !isLoading) {
                    coroutineScope.launch {
                        isLoading = true
                        result = null
                        errorMessage = null

                        try {
                            result = onAnalyze(smsContent.trim())
                        } catch (e: Exception) {
                            errorMessage = e.message
                                ?: "Không thể kết nối Backend."
                        } finally {
                            isLoading = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = purple
            ),
            enabled = smsContent.isNotBlank() && !isLoading
        ) {
            Text(
                if (isLoading) "ĐANG PHÂN TÍCH..."
                else "GỬI TIN NHẮN ĐỂ KIỂM TRA"
            )
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))

            CircularProgressIndicator()

            Spacer(modifier = Modifier.height(8.dp))

            Text("Đang gửi tin nhắn đến Backend ScamGuard...")
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "LỖI KẾT NỐI",
                        color = Color(0xFFD32F2F),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(errorMessage!!)
                }
            }
        }

        if (result != null) {
            val currentResult = result!!

            val riskColor = when (currentResult.riskLevel) {
                "HIGH" -> Color(0xFFD32F2F)
                "MEDIUM" -> Color(0xFFE69500)
                else -> Color(0xFF388E3C)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = riskColor.copy(alpha = 0.10f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "KẾT QUẢ PHÂN TÍCH TỪ BACKEND",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Mức độ rủi ro: ${currentResult.riskLevel}",
                        color = riskColor,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Điểm rủi ro: ${currentResult.riskScore}/100"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Lý do nhận diện:",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Text(currentResult.reasons)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Khuyến nghị:",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Text(currentResult.recommendation)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Công cụ phân tích: ${currentResult.engine}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                smsContent = ""
                result = null
                errorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("XÓA NỘI DUNG")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = purple
            ),
            enabled = !isLoading
        ) {
            Text("QUAY LẠI")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Quyền riêng tư: Tin nhắn chỉ được gửi đến Backend khi bạn chủ động yêu cầu kiểm tra.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}