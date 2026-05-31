package com.example.lifecare;

import androidx.appcompat.app.AppCompatActivity;

import androidx.room.Room;

import com.example.lifecare.db.AppDatabase;
import com.example.lifecare.model.UserProfile;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.view.View;
import android.widget.Toast;
import android.util.Log;

import com.example.lifecare.model.UserSensitivity;
import com.example.lifecare.logic.SensitivityInitializer;




public class UserInfoActivity extends AppCompatActivity {
    private EditText userName, age;
    private CheckBox chkNose, chkSkin, chkThroat, chkSmoke;
    private Button btnSave;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        setTitle("사용자 정보");

        userName = findViewById(R.id.userName);
        age = findViewById(R.id.age);
        chkNose = findViewById(R.id.nose);
        chkSkin = findViewById(R.id.skin);
        chkThroat = findViewById(R.id.throat);
        chkSmoke = findViewById(R.id.smoke);
        btnSave = findViewById(R.id.btnSave);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveUserData();
            }
        });

        db = Room.databaseBuilder(
                        getApplicationContext(),
                        AppDatabase.class,
                        "lifecare-db"
                )
                .allowMainThreadQueries()
                .build();
    }

    private void saveUserData(){
        String name = userName.getText().toString().trim();
        String ageStr = age.getText().toString().trim();

        if(name.isEmpty()){
            Toast.makeText(this, "이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
            userName.requestFocus();
            return;
        }

        if (ageStr.isEmpty()) {
            Toast.makeText(this, "나이를 입력해주세요.", Toast.LENGTH_SHORT).show();
            age.requestFocus();
            return;
        }

        boolean hasNoseIssue = chkNose.isChecked();   // 비염
        boolean hasSkinIssue = chkSkin.isChecked();   // 아토피
        boolean hasThroatIssue = chkThroat.isChecked(); // 천식
        boolean isSmoker = chkSmoke.isChecked();      // 흡연 여부

        UserProfile profile = new UserProfile();

        profile.name = name;
        profile.age = ageStr;

        profile.hasNoseIssue = hasNoseIssue;
        profile.hasSkinIssue = hasSkinIssue;
        profile.hasThroatIssue = hasThroatIssue;
        profile.isSmoker = isSmoker;

        db.userProfileDao().insert(profile);

        UserSensitivity sensitivity = SensitivityInitializer.createInitialSensitivity(profile);
        Log.d(
                "TEST",
                "호흡기 민감도 = " +
                        sensitivity.respiratorySensitivity
        );
        db.userSensitivityDao().insertInitial(sensitivity);

        String message = name + "님의 정보가 저장되었습니다.";
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        /*
         * Intent를 통한 데이터 전달
         * Intent intent = new Intent(UserInfoActivity.this, ResultActivity.class);
         * intent.putExtra("NAME", name);
         * intent.putExtra("AGE", Integer.parseInt(ageStr));
         * intent.putExtra("NOSE", hasNoseIssue);
         * intent.putExtra("SKIN", hasSkinIssue);
         * intent.putExtra("THROAT", hasThroatIssue);
         * intent.putExtra("SMOKE", isSmoker);
         * startActivity(intent);
         */
    }
}