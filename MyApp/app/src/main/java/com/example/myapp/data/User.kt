package com.example.myapp.data

/**
 * 用户数据类，表示应用中的用户信息
 *
 * @property id 用户唯一标识符
 * @property username 用户名
 * @property email 用户邮箱地址（可为空）
 * @property phone 用户手机号码（可为空，默认为null）
 * @property avatarUrl 用户头像URL（可为空）
 *
 * @author Your Name
 * @since 1.0.0
 */
data class User(
    val id: String,
    val username: String,
    val email: String?,
    val phone: String? = null,
    val avatarUrl: String?
)
