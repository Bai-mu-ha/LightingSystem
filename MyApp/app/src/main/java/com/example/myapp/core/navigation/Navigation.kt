package com.example.myapp.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapp.feature.auth.ForgotPasswordScreen
import com.example.myapp.feature.auth.LoginScreen
import com.example.myapp.feature.auth.RegisterScreen
import com.example.myapp.feature.main.DeviceDiscoveryScreen
import com.example.myapp.feature.main.DeviceControlScreen
import com.example.myapp.feature.main.MainScreen
import com.example.myapp.feature.model.AuthViewModel
import com.example.myapp.feature.model.UserViewModel
import com.example.myapp.feature.profile.ChangePasswordScreen
import com.example.myapp.feature.profile.ProfileScreen
import com.example.myapp.feature.profile.SettingsScreen

/**
 * 应用屏幕路由枚举类，定义了所有可用的导航目的地
 *
 * @param route 屏幕对应的路由路径
 * @author Your Name
 * @since 1.0.0
 */
sealed class Screen(val route: String) {
    /** 登录屏幕 */
    data object Login : Screen("login")

    /** 注册屏幕 */
    data object Register : Screen("register")

    /** 忘记密码屏幕 */
    data object ForgotPassword : Screen("forgotPassword")

    /** 主页屏幕 */
    data object Home : Screen("home")

    /** 个人资料屏幕 */
    data object Profile : Screen("profile")

    /** 设置屏幕 */
    data object Settings : Screen("settings")

    /** 修改密码屏幕 */
    data object ChangePwd : Screen("changePassword")

    /**
     * 为特定屏幕添加加载状态参数
     *
     * @return 带有加载状态参数的路由字符串
     */
    fun withLoadingState(): String {
        return when (this) {
            is Home -> "${this.route}?isLoading=true"
            else -> this.route
        }
    }

    companion object {
        /**
         * 定义底部导航栏项目列表
         */
        val bottomNavItems = listOf(
            BottomNavItem("首页", "home", Icons.Default.Home),
            BottomNavItem("我的", "profile", Icons.Default.Person)
        )
    }
}

/**
 * 底部导航项数据类
 *
 * @param title 导航项标题
 * @param route 导航项对应路由
 * @param icon 导航项图标
 *
 * @author Your Name
 * @since 1.0.0
 */
data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)

/**
 * 应用导航宿主组件，管理整个应用的导航路由和页面跳转
 *
 * 该组件定义了应用的所有可导航屏幕以及它们之间的导航关系
 *
 * @param navController 导航控制器，用于管理导航状态
 * @param userViewModel 用户相关数据的ViewModel
 * @param authViewModel 认证相关数据的ViewModel
 *
 * @author Your Name
 * @since 1.0.0
 */
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    userViewModel: UserViewModel,
    authViewModel: AuthViewModel,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                navController = navController,
                userViewModel = userViewModel,
                authViewModel = authViewModel,
                onRegisterClicked = { navController.navigate(Screen.Register.route) },
                onForgotPasswordClicked = { navController.navigate(Screen.ForgotPassword.route) },
                onLoginSuccess = {
                    navController.navigate("main_flow") {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                navController = navController,
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    //navController.popBackStack()
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable(Screen.Home.route) {
            MainScreen(
                navController = navController,
            )
        }

        composable("discovery") {
            DeviceDiscoveryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            "device_control/{deviceMac}",
            arguments = listOf(navArgument("deviceMac") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceMac = backStackEntry.arguments?.getString("deviceMac") ?: ""

            DeviceControlScreen(
                macAddress = deviceMac,  // 改为传递MAC地址
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                navController = navController,
                userViewModel = userViewModel,
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                navController = navController,
                userViewModel = userViewModel
            )
        }

        composable(Screen.ChangePwd.route) {
            ChangePasswordScreen(
                navController = navController,
                userViewModel = userViewModel
            )
        }
    }
}
