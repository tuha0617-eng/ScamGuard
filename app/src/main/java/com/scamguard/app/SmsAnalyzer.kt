package com.scamguard.app
data class SmsAnalysisResult(
    val riskLevel: String,
    val reason: String,
    val recommendation: String
)
object SmsAnalyzer {
    fun analyze(message: String): SmsAnalysisResult {
        val content = message.lowercase().trim()
        if (content.isBlank()) {
            return SmsAnalysisResult(
                riskLevel = "LOW",
                reason = "Chưa có nội dung tin nhắn để phân tích.",
                recommendation = "Vui lòng nhập nội dung SMS."
            )
        }
        val highRiskKeywords = listOf(
            "chuyen tien",
            "chuyển tiền",
            "ma otp",
            "mã otp",
            "mat khau",
            "mật khẩu",
            "tai khoan bi khoa",
            "tài khoản bị khóa",
            "xac minh ngay",
            "xác minh ngay",
            "cung cap thong tin",
            "cung cấp thông tin",
            "nhap ma",
            "nhập mã",
            "bam vao duong dan",
            "bấm vào đường dẫn"
        )
        val mediumRiskKeywords = listOf(
            "trung thuong",
            "trúng thưởng",
            "nhan qua",
            "nhận quà",
            "khuyen mai",
            "khuyến mãi",
            "xac nhan",
            "xác nhận",
            "cap nhat tai khoan",
            "cập nhật tài khoản",
            "duong dan",
            "đường dẫn",
            "http://",
            "https://",
            "bit.ly",
            "tinyurl"
        )
        val hasHighRiskKeyword =
            highRiskKeywords.any { content.contains(it) }
        val hasMediumRiskKeyword =
            mediumRiskKeywords.any { content.contains(it) }
        val hasUrl =
            content.contains("http://") ||
                    content.contains("https://") ||
                    content.contains("www.") ||
                    content.contains("bit.ly")
        return when {
            hasHighRiskKeyword -> SmsAnalysisResult(
                riskLevel = "HIGH",
                reason = "Tin nhắn chứa nội dung đáng ngờ như yêu cầu cung cấp thông tin, mã OTP, mật khẩu hoặc xác minh tài khoản.",
                recommendation = "Không nhấn vào đường dẫn, không cung cấp mã OTP hoặc mật khẩu. Hãy liên hệ tổ chức qua kênh chính thức."
            )
            hasUrl || hasMediumRiskKeyword -> SmsAnalysisResult(
                riskLevel = "MEDIUM",
                reason = "Tin nhắn có đường dẫn hoặc nội dung cần kiểm tra thêm. Chưa đủ căn cứ để kết luận đây là lừa đảo.",
                recommendation = "Không vội nhấn vào đường dẫn. Hãy kiểm tra người gửi và xác minh thông tin qua kênh chính thức."
            )
            else -> SmsAnalysisResult(
                riskLevel = "LOW",
                reason = "Chưa phát hiện từ khóa đáng ngờ theo bộ quy tắc kiểm tra thử nghiệm.",
                recommendation = "Vẫn nên kiểm tra người gửi và nội dung trước khi thực hiện yêu cầu trong tin nhắn."
            )
        }
    }
}
