package de.vokabelblitz.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words ORDER BY addedAt ASC")
    fun getAllWords(): Flow<List<Word>>

    @Query("SELECT COUNT(*) FROM words")
    fun getWordCount(): Flow<Int>



    @Query("SELECT * FROM words ORDER BY RANDOM()")
    suspend fun getAllWordsShuffled(): List<Word>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(word: Word)

    @Delete
    suspend fun delete(word: Word)


}
