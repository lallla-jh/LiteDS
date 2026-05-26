package me.magnum.melonds.common.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.magnum.melonds.domain.repositories.RetroAchievementsRepository

@HiltWorker
class RetroAchievementsSubmissionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val retroAchievementsRepository: RetroAchievementsRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val submissionResult = withContext(Dispatchers.IO) {
            retroAchievementsRepository.submitPendingAchievements()
        }
        return if (submissionResult.isSuccess) Result.success() else Result.retry()
    }
}
