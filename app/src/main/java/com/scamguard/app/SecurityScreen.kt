
package com.scamguard.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

@Composable
fun SecurityScreen(onBack: () -> Unit) {

    val context = LocalContext.current

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var notificationAllowed by remember {
        mutableStateOf(checkNotificationPermission(context))
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "BẢO MẬT & QUYỀN RIÊNG TƯ",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Quản lý quyền và dữ liệu của ScamGuard"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 1. QUYỀN THÔNG BÁO
            Text(
                text = "1. Quyền thông báo",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = if (notificationAllowed) {
                    "Trạng thái: Đã được cấp quyền thông báo"
                } else {
                    "Trạng thái: Chưa được cấp quyền thông báo"
                }
            )

            OutlinedButton(
                onClick = {
                    notificationAllowed =
                        checkNotificationPermission(context)

                    Toast.makeText(
                        context,
                        if (notificationAllowed) {
                            "Quyền thông báo đang được cấp"
                        } else {
                            "Bạn chưa cấp quyền thông báo"
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ) {
                Text("KIỂM TRA QUYỀN THÔNG BÁO")
            }

            Button(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts(
                            "package",
                            context.packageName,
                            null
                        )
                    )
                    context.startActivity(intent)
                }
            ) {
                Text("MỞ CÀI ĐẶT QUYỀN ỨNG DỤNG")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. QUYỀN SMS
            Text(
                text = "2. Quyền SMS",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Bản demo không tự động đọc hộp thư SMS. " +
                        "Tin nhắn chỉ được xử lý khi người dùng chủ động cung cấp."
            )

            Text(
                text = "Trạng thái: Không sử dụng quyền đọc toàn bộ SMS"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. QUẢN LÝ DỮ LIỆU
            Text(
                text = "3. Quản lý dữ liệu",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Bạn có thể xóa dữ liệu lịch sử quét được " +
                        "lưu trong bộ nhớ riêng của ứng dụng."
            )

            Button(
                onClick = {
                    showDeleteDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("XÓA LỊCH SỬ QUÉT")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("QUAY LẠI")
            }
        }
    }

    // HỘP THOẠI XÁC NHẬN XÓA
    if (showDeleteDialog) {

        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },

            title = {
                Text("Xác nhận xóa dữ liệu")
            },

            text = {
                Text(
                    "Bạn có chắc chắn muốn xóa lịch sử quét " +
                            "được lưu trên thiết bị không?"
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        val preferences =
                            context.getSharedPreferences(
                                "scamguard_history",
                                Context.MODE_PRIVATE
                            )

                        preferences.edit()
                            .clear()
                            .apply()

                        showDeleteDialog = false

                        Toast.makeText(
                            context,
                            "Đã xóa lịch sử quét thành công!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Text("XÓA")
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("HỦY")
                }
            }
        )
    }
}

// KIỂM TRA QUYỀN THÔNG BÁO
private fun checkNotificationPermission(
    context: Context
): Boolean {

    if (!NotificationManagerCompat
            .from(context)
            .areNotificationsEnabled()
    ) {
        return false
    }

    if (Build.VERSION.SDK_INT >= 33) {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    return true
}
