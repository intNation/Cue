package com.cue.data.local.dao
import com.cue.data.local.entity.UserEntity
import androidx.room.Dao
import androidx.room.Insert

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: UserEntity): Long
}