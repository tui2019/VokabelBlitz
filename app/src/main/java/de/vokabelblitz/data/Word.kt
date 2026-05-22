package de.vokabelblitz.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val germanWord: String,
    val englishTranslation: String,
    val usageExample: String,
    val addedAt: Long = System.currentTimeMillis()
)
