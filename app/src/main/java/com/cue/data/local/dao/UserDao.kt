package com.cue.data.local.dao

import UserEntity
import androidx.room.Dao
import androidx.room.Insert

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: UserEntity): Long
}