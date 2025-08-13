package com.example.myapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapp.core.navigation.AppNavHost
import com.example.myapp.feature.model.AuthViewModel
import com.example.myapp.feature.model.UserViewModel
import com.example.myapp.ui.theme.MyAppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 应用的主Activity，负责设置应用的初始界面和导航宿主
 *
 * 该Activity使用AndroidEntryPoint注解启用Hilt依赖注入，
 * 并通过Jetpack Compose设置应用的UI内容。
 *
 * @author Your Name
 * @since 1.0.0
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * 在Activity创建时调用，设置应用的Compose内容
     *
     * 此方法执行以下操作：
     * 1. 调用父类的onCreate方法
     * 2. 使用setContent设置Compose UI
     * 3. 应用主题MyAppTheme
     * 4. 通过Hilt获取AuthViewModel和UserViewModel实例
     * 5. 启动应用导航宿主AppNavHost
     *
     * @param savedInstanceState 用于恢复状态的Bundle对象
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyAppTheme {
                val authViewModel: AuthViewModel = hiltViewModel()
                val userViewModel: UserViewModel = hiltViewModel()
                AppNavHost(
                    authViewModel = authViewModel,
                    userViewModel = userViewModel
                )
            }
        }
    }
}
