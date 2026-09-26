package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.AutoReplyEntity
import com.example.data.local.entity.BotEntity
import com.example.data.local.entity.CommandEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BotDao {
    @Query("SELECT * FROM bots WHERE id = :id LIMIT 1")
    fun getBotById(id: String): Flow<BotEntity?>

    @Query("SELECT * FROM bots LIMIT 1")
    fun getPrimaryBot(): Flow<BotEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(bot: BotEntity)

    @Query("DELETE FROM bots")
    suspend fun clear()
}

@Dao
interface CommandDao {
    @Query("SELECT * FROM commands ORDER BY category ASC, name ASC")
    fun getAllCommands(): Flow<List<CommandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(commands: List<CommandEntity>)

    @Query("UPDATE commands SET enabled = :enabled WHERE id = :id")
    suspend fun updateCommandState(id: String, enabled: Boolean)

    @Query("DELETE FROM commands")
    suspend fun clear()
}

@Dao
interface AutoReplyDao {
    @Query("SELECT * FROM auto_replies ORDER BY id DESC")
    fun getAllRules(): Flow<List<AutoReplyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AutoReplyEntity)

    @Delete
    suspend fun deleteRule(rule: AutoReplyEntity)

    @Query("DELETE FROM auto_replies WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE auto_replies SET enabled = :enabled WHERE id = :id")
    suspend fun updateRuleEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM auto_replies")
    suspend fun clear()
}
