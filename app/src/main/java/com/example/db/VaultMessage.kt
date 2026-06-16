package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Database
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "messages")
data class VaultMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val coverText: String,
    val secretPayload: String, // Encrypted using the password
    val isSent: Boolean, // True if sent by user, false if received
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<VaultMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: VaultMessage)

    @Query("DELETE FROM messages")
    suspend fun clearAll()
}

@Database(entities = [VaultMessage::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}
