package com.scamguard.app
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
data class BackendScanResult(
    val riskLevel: String,
    val riskScore: Int,
    val reasons: String,
    val recommendation: String,
    val engine: String
)
object BackendApi {
    private const val BASE_URL = "http://10.0.2.2:8000"
    private const val ACCESS_TOKEN = "YOUR_ACCESS_TOKEN_HERE"
    suspend fun analyzeSms(
        smsText: String,
        sender: String? = null
    ): BackendScanResult = withContext(Dispatchers.IO) {

        val connection = URL(
            "$BASE_URL/api/v1/scans/sms"
        ).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.doOutput = true

            connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=utf-8"
            )

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            connection.setRequestProperty(
                "Authorization",
                "Bearer $ACCESS_TOKEN"
            )

            val request = JSONObject().apply {
                put("request_id", UUID.randomUUID().toString())
                put("text", smsText.trim())
                put("sender", sender?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
            }

            connection.outputStream.use { output ->
                output.write(request.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode

            val responseStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseText = responseStream?.bufferedReader()?.use {
                it.readText()
            } ?: ""

            if (responseCode !in 200..299) {
                throw Exception(
                    "Backend trả về HTTP $responseCode: $responseText"
                )
            }

            val json = JSONObject(responseText)

            val riskLevel = json.optString(
                "risk_level",
                "LOW"
            ).uppercase()

            val reasonsArray = json.optJSONArray("reasons")

            val reasons = if (reasonsArray != null) {
                (0 until reasonsArray.length()).joinToString("\n") {
                    "• ${reasonsArray.optString(it)}"
                }
            } else {
                "Backend chưa cung cấp lý do phân tích."
            }

            val recommendation = when (riskLevel) {
                "HIGH" ->
                    "Không nhấn vào đường dẫn, không chuyển tiền và không cung cấp mã OTP hoặc mật khẩu."

                "MEDIUM" ->
                    "Hãy kiểm tra người gửi và xác minh thông tin qua kênh chính thức trước khi thực hiện."

                else ->
                    "Chưa phát hiện dấu hiệu nguy hiểm rõ ràng. Vẫn cần thận trọng với nội dung tin nhắn."
            }

            BackendScanResult(
                riskLevel = riskLevel,
                riskScore = json.optInt("risk_score", 0),
                reasons = reasons,
                recommendation = recommendation,
                engine = json.optString("engine", "unknown")
            )

        } finally {
            connection.disconnect()
        }
    }
}
