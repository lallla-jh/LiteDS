package me.magnum.melonds.common.workers

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.runBlocking
import me.magnum.melonds.common.cheats.CheatDatabaseParser
import me.magnum.melonds.common.cheats.CheatDatabaseParserListener
import me.magnum.melonds.common.cheats.ProgressTrackerInputStream
import me.magnum.melonds.common.cheats.XmlCheatDatabaseParser
import me.magnum.melonds.domain.model.CheatDatabase
import me.magnum.melonds.domain.model.Game
import me.magnum.melonds.domain.repositories.CheatsRepository
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltWorker
class CheatImportWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val cheatsRepository: CheatsRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_URI = "uri"
        const val KEY_PROGRESS_RELATIVE = "progress_relative"
        const val KEY_PROGRESS_ITEM = "progress_item"
    }

    override suspend fun doWork(): Result {
        val uri = inputData.getString(KEY_URI)?.toUri() ?: return Result.failure()

        return try {
            val databaseDocument = DocumentFile.fromSingleUri(applicationContext, uri)
            if (databaseDocument?.isFile != true) {
                return Result.failure()
            }

            val totalFileSize = applicationContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                val length = it.length
                if (length == AssetFileDescriptor.UNKNOWN_LENGTH) null else length
            }

            val databaseExtension = databaseDocument.name?.substringAfterLast('.')
            val parser = when (databaseExtension) {
                "xml" -> XmlCheatDatabaseParser()
                else -> return Result.failure()
            }

            parseXmlDocument(uri, parser, totalFileSize)
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private suspend fun parseXmlDocument(uri: Uri, parser: CheatDatabaseParser, totalFileSize: Long?) = suspendCoroutine { continuation ->
        applicationContext.contentResolver.openInputStream(uri)?.use {
            val progressTrackerStream = ProgressTrackerInputStream(it)
            parser.parseCheatDatabase(progressTrackerStream, object : CheatDatabaseParserListener {
                override fun onDatabaseParseStart(databaseName: String): CheatDatabase = runBlocking {
                    cheatsRepository.deleteCheatDatabaseIfExists(databaseName)
                    cheatsRepository.addCheatDatabase(databaseName)
                }

                override fun onGameParseStart(gameName: String) {
                    val readProgress = if (totalFileSize != null) {
                        (progressTrackerStream.totalReadBytes.toDouble() / totalFileSize * 100).toInt()
                    } else {
                        0
                    }
                    setProgressAsync(workDataOf(
                        KEY_PROGRESS_RELATIVE to readProgress / 100f,
                        KEY_PROGRESS_ITEM to gameName
                    ))
                }

                override fun onGameParsed(game: Game): Unit = runBlocking {
                    cheatsRepository.addGameCheats(game)
                }

                override fun onParseComplete() {
                    continuation.resume(Result.success())
                }
            })
        }
    }
}
