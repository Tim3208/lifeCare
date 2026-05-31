package com.example.lifecare.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.lifecare.model.UserProfile;
import com.example.lifecare.model.UserSensitivity;

@Database(
        entities = {
                UserProfile.class,
                UserSensitivity.class,
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserProfileDao userProfileDao();

    public abstract UserSensitivityDao userSensitivityDao();

}