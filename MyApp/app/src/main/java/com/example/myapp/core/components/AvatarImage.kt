package com.example.myapp.core.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import java.io.File

/**
 * 用户头像显示组件，支持加载本地文件头像
 *
 * 当头像文件为空时显示加载指示器，否则显示头像图片
 *
 * @param avatarFile 头像文件，可以为null
 * @param modifier 修饰符
 *
 * @author Your Name
 * @since 1.0.0
 */
@Composable
fun AvatarImage(
    avatarFile: File?,
    modifier: Modifier = Modifier
) {
    if (avatarFile == null) {
        CircularProgressIndicator(
            modifier = modifier
                .size(120.dp)
        )
    } else {
        val painter = rememberAsyncImagePainter(
            model = avatarFile.absolutePath,
            error = null
        )

        Image(
            painter = painter,
            contentDescription = "用户头像",
            modifier = modifier
                .clip(MaterialTheme.shapes.large),
            contentScale = ContentScale.Crop
        )
    }
}
