/*
 * TỔNG QUAN FILE: Hilt module cung cấp database Room và các DAO.
 *
 * Thành phần chính:
 * - provideDatabase (@Singleton): tạo file loki-cache.db, đăng ký các Migration.
 * - providePhotoDao, provideChatDao, provideUserDao, provideOutboxDao: lấy DAO từ database dùng chung.
 *
 * Lưu ý:
 * - Có Migration thì Room dùng Migration; fallbackToDestructiveMigration chỉ còn cho các bản quá cũ (trước
 *   version 2) chưa từng phát hành.
 * - Database phải là singleton; DAO không cần vì chúng đều lấy từ cùng một database.
 */
package com.example.loki.di

import android.content.Context
import androidx.room.Room
import com.example.loki.data.local.db.LokiDatabase
import com.example.loki.data.local.db.MIGRATION_2_3
import com.example.loki.data.local.db.dao.ChatDao
import com.example.loki.data.local.db.dao.OutboxDao
import com.example.loki.data.local.db.dao.PhotoDao
import com.example.loki.data.local.db.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LokiDatabase =
        Room.databaseBuilder(context, LokiDatabase::class.java, "loki-cache.db")
            .addMigrations(MIGRATION_2_3)
            // Only for pre-release versions without a Migration path
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun providePhotoDao(db: LokiDatabase): PhotoDao = db.photoDao()

    @Provides fun provideChatDao(db: LokiDatabase): ChatDao = db.chatDao()

    @Provides fun provideUserDao(db: LokiDatabase): UserDao = db.userDao()

    @Provides fun provideOutboxDao(db: LokiDatabase): OutboxDao = db.outboxDao()
}
