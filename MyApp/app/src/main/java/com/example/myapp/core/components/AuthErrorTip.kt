package com.example.myapp.core.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

/**
 * 认证错误提示组件，用于显示认证过程中的错误或成功消息
 *
 * 该组件使用动画效果显示和隐藏提示信息，支持成功和错误两种状态显示
 *
 * @param visible 是否显示提示
 * @param message 提示消息内容
 * @param modifier 修饰符
 * @param isSuccess 是否为成功消息，默认为false（错误消息）
 *
 * @author Your Name
 * @since 1.0.0
 */
@Composable
fun AuthErrorTip(
    visible: Boolean,
    message: String,
    modifier: Modifier = Modifier,
    isSuccess: Boolean = false
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                color = if (isSuccess) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .wrapContentWidth()
            ) {
                Text(
                    text = message,
                    color = if (isSuccess) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
