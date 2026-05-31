package com.example.lifecare.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profile")
public class UserProfile {
    @PrimaryKey(autoGenerate = true)
    public int id; // 기본키 (자동 증가)

    public String name;
    public String age;
    public boolean hasNoseIssue;   // 비염
    public boolean hasSkinIssue;   // 아토피
    public boolean hasThroatIssue; // 천식
    public boolean isSmoker;       // 흡연 여부
}