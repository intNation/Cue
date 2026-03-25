package com.cue.data.local.dao

@Dao
interface UserDao {
    @insert
    suspend fun insertUser(user: UserEntity): Long
}