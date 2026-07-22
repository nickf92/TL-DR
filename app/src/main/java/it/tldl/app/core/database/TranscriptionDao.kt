package it.tldl.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TranscriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transcription: TranscriptionEntity): Long

    @Query("SELECT * FROM transcriptions ORDER BY timestampMs DESC")
    suspend fun getAll(): List<TranscriptionEntity>

    @Query("SELECT * FROM transcriptions ORDER BY timestampMs DESC")
    fun getAllFlow(): kotlinx.coroutines.flow.Flow<List<TranscriptionEntity>>

    @Query("DELETE FROM transcriptions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transcriptions")
    suspend fun clearAll()
}
