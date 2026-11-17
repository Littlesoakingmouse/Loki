/*
 * TỔNG QUAN FILE: Hilt module cung cấp Retrofit và các API interface.
 *
 * Thành phần chính:
 * - provideRetrofit (@Singleton): baseUrl từ BuildConfig.API_BASE_URL, OkHttp có AuthInterceptor, chuyển JSON
 *   bằng kotlinx.serialization.
 * - Json: ignoreUnknownKeys để backend thêm trường mới không làm app cũ lỗi; coerceInputValues để null dùng
 *   giá trị mặc định.
 * - provideAuthApi, provideUserApi, providePhotoApi, provideChatApi.
 *
 * Lưu ý:
 * - Base URL phải kết thúc bằng "/".
 * - Client này luôn gửi JWT; dịch vụ bên ngoài như Cloudinary phải dùng client riêng.
 * - Timeout đặt rõ: kết nối 15 giây, đọc 30 giây. Mạng chập chờn sẽ ra IOException để outbox xử lý.
 */
package com.example.loki.di

import com.example.loki.BuildConfig
import com.example.loki.data.remote.api.AuthApi
import com.example.loki.data.remote.api.ChatApi
import com.example.loki.data.remote.api.PhotoApi
import com.example.loki.data.remote.api.UserApi
import com.example.loki.data.remote.interceptor.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(authInterceptor: AuthInterceptor): Retrofit {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true // a JSON null for a field with a default uses the default
        }
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build(),
            )
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create()

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create()

    @Provides
    @Singleton
    fun providePhotoApi(retrofit: Retrofit): PhotoApi = retrofit.create()

    @Provides
    @Singleton
    fun provideChatApi(retrofit: Retrofit): ChatApi = retrofit.create()
}
