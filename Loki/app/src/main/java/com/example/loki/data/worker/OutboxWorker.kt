/*
 * TỔNG QUAN FILE: Worker của WorkManager gửi outbox khi máy có mạng, kể cả sau khi tắt app hoặc khởi động lại
 * máy.
 *
 * Thành phần chính:
 * - doWork: chưa đăng nhập thì không gửi gì; còn lại gọi OutboxRepository.flush, lỗi mạng hoặc HTTP thử lại
 *   được thì trả Result.retry().
 * - schedule: một công việc duy nhất tên "outbox", chỉ chạy khi có mạng, chờ lũy thừa bắt đầu từ 30 giây;
 *   APPEND_OR_REPLACE để mục thêm vào giữa chừng vẫn được gửi.
 * - cancel: huỷ khi đăng xuất.
 *
 * Lưu ý:
 * - @HiltWorker và @AssistedInject: context và params do WorkManager truyền, phụ thuộc còn lại do Hilt cung
 *   cấp. Cần cấu hình trong LokiApp và Manifest.
 * - Result ở đây là của WorkManager, khác kotlin.Result dùng trong safeCall.
 * - Lỗi không thử lại được (Cloudinary/back-end từ chối) đã được flush bỏ qua từng mục, nên không còn mục nào
 *   chặn cả hàng đợi.
 */
package com.example.loki.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.loki.data.local.datastore.SessionStore
import com.example.loki.data.repository.OutboxRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val WORK_NAME = "outbox"

// Sends the outbox once the phone is online. WorkManager keeps it scheduled across app restarts and reboots
@HiltWorker
class OutboxWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val outbox: OutboxRepository,
    private val session: SessionStore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Logged out (e.g. the token expired): nothing may go out. The next login clears the queue
        if (session.token.first() == null) return Result.success()
        return try {
            outbox.flush()
            Result.success()
        } catch (e: IOException) {
            Result.retry() // online but the backend or Cloudinary is unreachable: try again later
        } catch (e: HttpException) {
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<OutboxWorker>()
                .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            // Append: something queued while a run is in progress still gets a run after it
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
