package com.example.lifecare.logic;

import com.example.lifecare.model.UserProfile;
import com.example.lifecare.model.UserSensitivity;

public class SensitivityInitializer {

//    UserProfile profile = db.userProfileDao().getCurrentProfile();


    public static UserSensitivity createInitialSensitivity(UserProfile profile) {
        UserSensitivity sensitivity = new UserSensitivity();

        sensitivity.userId = 1;

        double temperatureSensitivity = 5;
        double skinSensitivity = 5;
        double respiratorySensitivity = 5;

        //비염 -> 호흡기 민감도 증가
        if (profile.hasNoseIssue) {
            respiratorySensitivity += 2.0;
        }

        //아토피 -> 피부 민감도 증가
        if (profile.hasSkinIssue) {
            skinSensitivity += 2.0;
        }

        //천식 -> 호흡기 민감도 증가
        if (profile.hasThroatIssue) {
            respiratorySensitivity += 3.0;
        }

        //흡연 -> 호흡기 민감도 증가

        if (profile.isSmoker) {
            respiratorySensitivity += 1.5;
        }

        //나이 보정 어린이/고령자는 호흡기와 자외선 쪽을 약간 높임
        int age = Integer.parseInt(profile.age);

        if (age > 0 && age <= 12) {
            respiratorySensitivity += 0.8;
            skinSensitivity += 0.5;
        } else if (age >= 65) {
            respiratorySensitivity += 0.8;
            temperatureSensitivity -= 0.3;
        }

        sensitivity.temperatureSensitivity = temperatureSensitivity;

        sensitivity.skinSensitivity = skinSensitivity;

        sensitivity.respiratorySensitivity = respiratorySensitivity;

        return sensitivity;
    }
}
