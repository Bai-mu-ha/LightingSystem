package com.example.myapp.service

import android.content.Context
import android.net.Uri
import com.example.myapp.data.User
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户数据仓库类，负责处理用户数据的存储和获取操作
 *
 * 管理用户头像、个人信息等数据的本地缓存和服务器同步
 *
 * @param apiService API服务接口，用于与后端通信
 * @param userService 用户服务，用于访问用户相关功能
 * @param context 应用上下文
 *
 * @author Your Name
 * @since 1.0.0
 */
@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService,
    private val userService: UserService,
    @ApplicationContext private val context: Context,
) {
    /**
     * 下载并缓存头像
     *
     * 从服务器下载用户头像并保存到本地缓存
     *
     * @param userId 用户ID
     * @param url 头像URL
     * @return 缓存的头像文件
     * @throws Exception 下载或保存失败时抛出异常
     */
    // 下载并缓存头像
    suspend fun cacheAvatar(userId: String, url: String): File {
        val file = getAvatarCacheFile(userId)
        println("DEBUG: 开始缓存头像: $url → ${file.absolutePath}")

        try {
            val finalUrl = url
            // 调用API下载头像文件
            val response = apiService.downloadFile(finalUrl)

            if (!response.isSuccessful) {
                throw Exception("下载失败: ${response.code()}")
            }

            response.body()?.use { responseBody ->
                file.outputStream().use { output ->
                    responseBody.byteStream().copyTo(output) // 简化流操作
                }
            }

            println("DEBUG: 头像缓存成功，文件大小: ${file.length()} bytes")
            return file
        } catch (e: Exception) {
            file.delete() // 删除可能不完整的文件
            throw e
        }
    }

    /**
     * 获取缓存的头像文件
     *
     * 根据用户ID获取对应的本地头像缓存文件
     *
     * @param userId 用户ID
     * @return 头像缓存文件
     */
    // 获取缓存的头像文件
    fun getAvatarCacheFile(userId: String): File {
        // 创建头像缓存目录
        val cacheDir = File(context.cacheDir, "avatars").apply { mkdirs() }
        return File(cacheDir, "avatar_$userId.jpg")
    }

    /**
     * 更新昵称
     *
     * 更新用户的昵称信息
     *
     * @param newName 新昵称
     * @return 更新后的用户对象
     * @throws Exception 操作失败时抛出异常
     */
    //更新昵称
    suspend fun updateUsername(newName: String): User {
        val token = userService.getToken() ?: throw Exception("未登录")
        // 调用API更新用户名
        val response = apiService.updateUsername(
            token = "Bearer $token",
            request = SingleFieldRequest(newName)  // 改为使用SingleFieldRequest
        )
        return processUpdateResponse(response)
    }

    /**
     * 更新头像
     *
     * 更新用户的头像信息
     *
     * @param newAvatarUri 新头像的URI
     * @return 更新后的用户对象
     * @throws Exception 操作失败时抛出异常
     */
    //更新头像
    suspend fun updateAvatar(newAvatarUri: Uri): User {
        val token = userService.getToken() ?: throw Exception("未登录")
        val localFile = prepareAvatarFile(newAvatarUri)

        // 调用API更新头像
        val response = apiService.updateAvatar(
            token = "Bearer $token",
            avatar = createAvatarPart(localFile)
        )

        // 将本地文件移动到正式缓存路径
        val userId = userService.getCurrentUserId() ?: throw Exception("用户ID为空")
        val cacheFile = getAvatarCacheFile(userId).apply {
            if (exists()) delete() // 删除旧文件
            localFile.copyTo(this) // 复制新文件
            localFile.delete() // 删除临时文件
        }

        println("DEBUG: 头像已直接替换为本地文件: ${cacheFile.absolutePath}")
        return processUpdateResponse(response)
    }

    /**
     * 更新响应处理
     *
     * 处理用户信息更新API的响应结果
     *
     * @param response API响应
     * @return 更新后的用户对象
     * @throws Exception 操作失败时抛出异常
     */
    //更新
    private suspend fun processUpdateResponse(response: Response<AuthResponse>): User {
        if (!response.isSuccessful) {
            throw Exception("更新失败: ${response.code()} ${response.errorBody()?.string()}")
        }

        return response.body()?.data?.let { userData ->
            // 更新本地存储
            userService.saveAuthData(
                token = userService.getToken() ?: throw Exception("未登录"),
                userId = userData.userId ?: throw Exception("用户ID为空"),
                username = userData.username,
                email = userData.email,
                phone = userData.phone,
                avatarUrl = userData.avatarUrl
            )

            User(
                id = userData.userId,
                username = userData.username ?: userService.getCurrentUsername() ?: "访客登录",
                email = userData.email ?: userService.getCurrentEmail() ?: "",
                phone = userData.phone ?: userService.getCurrentPhone(),
                avatarUrl = userData.avatarUrl ?: userService.getCurrentAvatarUrl()
            )
        } ?: throw Exception("服务器返回空数据")
    }

    /**
     * 准备头像文件
     *
     * 将URI指向的图片文件复制到临时文件
     *
     * @param uri 头像图片URI
     * @return 临时文件
     * @throws Exception 读取文件失败时抛出异常
     */
    fun prepareAvatarFile(uri: Uri): File {
        return File(context.cacheDir, "avatar_temp_${System.currentTimeMillis()}.jpg").apply {
            context.contentResolver.openInputStream(uri)?.use { input ->
                outputStream().use { output -> input.copyTo(output) }
            } ?: throw Exception("无法读取头像文件")
        }
    }

    /**
     * 创建头像部分
     *
     * 将文件转换为多部分表单数据
     *
     * @param file 头像文件
     * @return 多部分表单数据
     * @throws Exception 文件格式不支持时抛出异常
     */
    private fun createAvatarPart(file: File): MultipartBody.Part {
        val mimeType = when (file.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> throw Exception("只支持JPG/JPEG/PNG格式")
        }
        return MultipartBody.Part.createFormData(
            "avatar",
            file.name,
            file.asRequestBody(mimeType.toMediaTypeOrNull())
        )
    }

    /**
     * 更新邮箱
     *
     * 更新用户的邮箱信息
     *
     * @param newEmail 新邮箱地址
     * @return 更新后的用户对象
     * @throws Exception 操作失败时抛出异常
     */
    suspend fun updateEmail(newEmail: String): User {
        val token = userService.getToken() ?: throw Exception("未登录")
        // 调用API更新邮箱
        val response = apiService.updateEmail(
            token = "Bearer $token",
            request = SingleFieldRequest(newEmail)  // 改为使用SingleFieldRequest
        )
        return processUpdateResponse(response)
    }

    /**
     * 验证密码
     *
     * 验证用户输入的密码是否正确
     *
     * @param password 用户输入的密码
     * @return 密码是否正确
     * @throws Exception 未登录时抛出异常
     */
    suspend fun verifyPassword(password: String): Boolean {
        val token = userService.getToken() ?: throw Exception("未登录")
        // 调用API验证密码
        val response = apiService.verifyPassword(
            token = "Bearer $token",
            request = SingleFieldRequest(password)
        )
        return response.isSuccessful && response.body()?.data == true
    }

    /**
     * 更新密码
     *
     * 更新用户的密码信息
     *
     * @param newPassword 新密码
     * @throws Exception 操作失败时抛出异常
     */
    suspend fun updatePassword(newPassword: String) {
        val token = userService.getToken() ?: throw Exception("未登录")
        // 调用API更新密码
        val response = apiService.updatePassword(
            token = "Bearer $token",
            request = SingleFieldRequest(newPassword)
        )
        if (!response.isSuccessful) {
            throw Exception("更新密码失败: ${response.code()} ${response.errorBody()?.string()}")
        }
    }
}
