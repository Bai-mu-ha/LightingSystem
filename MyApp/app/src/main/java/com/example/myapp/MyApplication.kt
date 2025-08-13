package com.example.myapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 自定义Application类，作为整个应用的入口点
 *
 * 该类使用Dagger Hilt进行依赖注入，通过@HiltAndroidApp注解标记，
 * 使得整个应用可以使用Hilt提供的依赖注入功能。
 *
 * @author Your Name
 * @since 1.0.0
 */
@HiltAndroidApp
class MyApplication : Application()
