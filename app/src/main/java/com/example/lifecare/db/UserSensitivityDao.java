package com.example.lifecare.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.lifecare.model.UserSensitivity;

@Dao
public interface UserSensitivityDao {
    //최초저장
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertInitial(UserSensitivity sensitivity);
    //검색
    @Query("SELECT * FROM user_sensitivity WHERE userId = :userId")
    UserSensitivity getByUserId(int userId); //해당 매서드 실행시 상단의 SQL 실행
    //업데이트 -> 추후 사용자 피드백 반영에 사용
    @Update
    void update(UserSensitivity sensitivity);
}