package com.example.lifecare.model;


import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;


//외래키를 사용해 연동 완료
@Entity(
        tableName = "user_sensitivity",
        foreignKeys = @ForeignKey(
                entity = UserProfile.class,
                parentColumns = "id",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index("userId")
        })
public class UserSensitivity {

    //UserProfile의 id와 같은 값으로 사용
    @PrimaryKey
    public int userId;

    //1 ~ 10 사이 값으로 관리
    //1 = 둔감, 5 = 평균, 10 = 매우 민감

    public double temperatureSensitivity;  //온도 민감
    public double respiratorySensitivity; //호흡기 민감도
    public double skinSensitivity;        //피부 민감도
}