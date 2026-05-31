package com.example.lifecare.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.lifecare.model.UserProfile;

@Dao
public interface UserProfileDao {
    @Insert
    void insert(UserProfile profile);

    @Update
    void update(UserProfile profile);

    @Query("SELECT * FROM user_profile LIMIT 1")
    UserProfile getCurrentProfile();
}