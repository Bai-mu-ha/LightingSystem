package com.example.myapp.core.components

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.myapp.R
import kotlinx.coroutines.delay

private val BottomSheetWidth = 1.0f // 调整为100%宽度
private val BottomSheetHeight = 0.85f // 调整为85%高度

/**
 * 验证旧邮箱内容组件
 *
 * 用于显示验证旧邮箱的界面，包括输入验证码、获取验证码倒计时等功能
 *
 * @param email 当前邮箱地址
 * @param errorMessage 错误消息，可为null
 * @param onDismiss 关闭界面回调
 * @param onVerify 验证验证码回调
 * @param onResend 重新发送验证码回调
 * @param onClearError 清除错误消息回调
 *
 * @author Your Name
 * @since 1.0.0
 */
@Composable
fun VerifyOldEmailContent(
    email: String,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit,
    onResend: () -> Unit,
    onClearError: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(60) }
    var isCountingDown by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    // 错误消息自动消失
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            showError = true
            delay(3000)
            showError = false
            onClearError()
        }
    }

    LaunchedEffect(isCountingDown) {
        if (isCountingDown) {
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            isCountingDown = false
        }
    }

    Column(
        modifier = Modifier
            .widthIn(max = 500.dp) // 限制最大宽度
            .fillMaxWidth(BottomSheetWidth)
            .fillMaxHeight(BottomSheetHeight)
            .padding(24.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraLarge
            )
    ) {
        // 错误提示（固定位置，不影响布局）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = showError && errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(8.dp)
                )
            }
        }
        // 标题和关闭按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "验证旧邮箱",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "关闭",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp)
        ) {
            Text(
                text = "当前邮箱",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 验证码输入区域
        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("验证码") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            shape = MaterialTheme.shapes.medium,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 获取验证码按钮
        Button(
            onClick = {
                onResend()
                isCountingDown = true
                countdown = 60
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCountingDown,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (!isCountingDown) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (!isCountingDown) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
            )
        ) {
            if (isCountingDown) {
                Text("${countdown}秒后重新获取")
            } else {
                Text("获取验证码")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 验证按钮
        Button(
            onClick = { onVerify(code) },
            enabled = code.length == 6,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("下一步", style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * 设置新邮箱内容组件
 *
 * 用于显示设置新邮箱的界面，包括输入新邮箱地址、验证码等功能
 *
 * @param errorMessage 错误消息，可为null
 * @param isCountingDown 是否正在倒计时
 * @param onDismiss 关闭界面回调
 * @param onVerify 验证新邮箱和验证码回调
 * @param onResend 重新发送验证码回调
 * @param onStartCountdown 开始倒计时回调
 * @param onClearError 清除错误消息回调
 *
 * @author Your Name
 * @since 1.0.0
 */
@Composable
fun SetNewEmailContent(
    errorMessage: String?,
    isCountingDown: Boolean,
    onDismiss: () -> Unit,
    onVerify: (String, String) -> Unit,
    onResend: (String) -> Unit,
    onStartCountdown: () -> Unit,
    onClearError: () -> Unit
) {
    var newEmail by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(60) }
    var showError by remember { mutableStateOf(false) }

    // 错误消息自动消失
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            showError = true
            delay(3000)
            showError = false
            onClearError()
        }
    }

    LaunchedEffect(isCountingDown) {
        if (isCountingDown) {
            countdown = 60
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            onStartCountdown()
        }
    }

    Column(
        modifier = Modifier
            .widthIn(max = 500.dp) // 限制最大宽度
            .fillMaxWidth(BottomSheetWidth)
            .fillMaxHeight(BottomSheetHeight)
            .padding(24.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraLarge
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp), // 固定高度
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = showError && errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(8.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "设置新邮箱",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "关闭",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 新邮箱输入
        OutlinedTextField(
            value = newEmail,
            onValueChange = { newEmail = it },
            label = { Text("新邮箱地址") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            shape = MaterialTheme.shapes.medium,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 验证码输入
        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("验证码") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            shape = MaterialTheme.shapes.medium,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 获取验证码按钮
        Button(
            onClick = { onResend(newEmail) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCountingDown && newEmail.isNotEmpty(),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (!isCountingDown) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (!isCountingDown) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
            )
        ) {
            if (isCountingDown) {
                Text("${countdown}秒后重新获取")
            } else {
                Text("获取验证码")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 完成按钮
        Button(
            onClick = { onVerify(newEmail, code) },
            enabled = code.length == 6,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("完成", style = MaterialTheme.typography.labelLarge)
        }
    }
}
