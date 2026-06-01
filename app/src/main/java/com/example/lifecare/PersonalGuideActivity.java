package com.example.lifecare;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.lifecare.logic.Personalization;
import com.example.lifecare.model.PersonalAdvice;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class PersonalGuideActivity extends AppCompatActivity {

    private TextView guideTitleText;
    private TextView totalRiskScoreText;
    private TextView temperatureGuideText;
    private TextView skinGuideText;
    private TextView respiratoryGuideText;
    private TextView finalGuideText;

    private final Personalization ps = new Personalization();
    private PersonalAdvice pa;

    public void loadPersonal() {
        new Thread(() -> {
            try {
                PersonalAdvice result = ps.calculate("노원구", 1);

                runOnUiThread(() -> {
                    pa = result;
                    showAdvice(pa);
                });
            } catch (Exception ex) {
                runOnUiThread(() -> showErrorState(ex));
            }
        }).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_guide);
        setTitle("맞춤 가이드");

        guideTitleText = findViewById(R.id.guideTitleText);
        totalRiskScoreText = findViewById(R.id.overallRiskText);
        temperatureGuideText = findViewById(R.id.temperatureGuideText);
        skinGuideText = findViewById(R.id.skinGuideText);
        respiratoryGuideText = findViewById(R.id.respiratoryGuideText);
        finalGuideText = findViewById(R.id.finalGuideText);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setSelectedItemId(R.id.nav_guide);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                Intent intent = new Intent(PersonalGuideActivity.this, MainActivity.class);
                startActivity(intent);
                return true;

            } else if (id == R.id.nav_health) {
                Intent intent = new Intent(PersonalGuideActivity.this, UserInfoActivity.class);
                startActivity(intent);
                return true;

            } else if (id == R.id.nav_guide) {
                return true;

            } else if (id == R.id.nav_more) {
                return true;
            }

            return false;
        });

        //테스트용
        showLoadingState();

        loadPersonal();
    }

    private void showAdvice(PersonalAdvice advice) {
        if (advice == null) {
            showEmptyState();
            return;
        }

        guideTitleText.setText("오늘의 맞춤 가이드");
        totalRiskScoreText.setText("종합 위험도: " + advice.temperatureRiskGrade
                + " / 피부 " + advice.skinRiskGrade
                + " / 호흡기 " + advice.respiratoryRiskGrade);
        temperatureGuideText.setText(buildTemperatureGuide(advice));
        skinGuideText.setText(buildSkinGuide(advice));
        respiratoryGuideText.setText(buildRespiratoryGuide(advice));
        finalGuideText.setText(buildFinalGuide(advice));
    }

    private void showLoadingState() {
        guideTitleText.setText("맞춤 가이드 불러오는 중");
        totalRiskScoreText.setText("종합 위험도: 조회 중");
        temperatureGuideText.setText("온도 가이드를 불러오는 중입니다.");
        skinGuideText.setText("피부 가이드를 불러오는 중입니다.");
        respiratoryGuideText.setText("호흡기 가이드를 불러오는 중입니다.");
        finalGuideText.setText("잠시 후 맞춤 안내가 표시됩니다.");
    }

    private void showEmptyState() {
        guideTitleText.setText("맞춤 가이드 없음");
        totalRiskScoreText.setText("종합 위험도: 조회 전");
        temperatureGuideText.setText("온도 가이드가 아직 없습니다.");
        skinGuideText.setText("피부 가이드가 아직 없습니다.");
        respiratoryGuideText.setText("호흡기 가이드가 아직 없습니다.");
        finalGuideText.setText("메인 화면에서 조회버튼을 눌려주세요!");
    }

    private void showErrorState(Exception ex) {
        guideTitleText.setText("맞춤 가이드 조회 실패");
        totalRiskScoreText.setText("종합 위험도: 조회 실패");
        temperatureGuideText.setText("온도 가이드를 만들지 못했습니다.");
        skinGuideText.setText("피부 가이드를 만들지 못했습니다.");
        respiratoryGuideText.setText("호흡기 가이드를 만들지 못했습니다.");
        finalGuideText.setText("오류가 발생했습니다.");
    }

    private String buildTemperatureGuide(PersonalAdvice advice) {
        String grade = advice.temperatureRiskGrade;

        if ("매우추움".equals(grade)) {
            return "오늘의 체감 날씨는 매우 추움입니다. 두꺼운 외투, 목도리, 장갑처럼 보온성이 높은 옷차림을 추천드립니다. 외출 시간이 길다면 핫팩이나 따뜻한 음료도 챙기는 것이 좋습니다.";
        }

        if ("추움".equals(grade)) {
            return "오늘의 체감 날씨는 추움입니다. 가벼운 옷차림보다는 겉옷을 챙기고, 아침·저녁 외출에는 체온이 떨어지지 않도록 내복 착용을 추천드립니다.";
        }

        if ("더움".equals(grade)) {
            return "오늘의 체감 날씨는 더움입니다. 통풍이 잘 되는 옷을 입고, 물을 자주 마시며, 한낮 장시간 야외 활동은 줄이는 것을 추천드립니다.";
        }

        if ("매우더움".equals(grade)) {
            return "오늘의 체감 날씨는 매우 더움입니다. 땀이 잘 마르는 옷, 모자, 충분한 물을 준비하세요. 가능한 그늘이나 실내에서 쉬는 시간을 자주 갖는 것이 좋습니다.";
        }

        if ("보통".equals(grade)) {
            return "오늘의 체감 날씨는 보통입니다. 큰 온도 부담은 낮지만, 일교차가 있을 수 있으니 얇은 겉옷을 하나 챙기면 좋습니다.";
        }

        return "온도 위험도 등급이 없어 정확한 옷차림 추천을 만들지 못했습니다.";
    }

    private String buildSkinGuide(PersonalAdvice advice) {
        String grade = advice.skinRiskGrade;

        if ("필수".equals(grade)) {
            return "피부 관리 위험도는 필수 단계입니다. 외출 전 자외선 차단제를 충분히 바르고, 모자나 양산을 함께 사용하는 것을 추천드립니다. 장시간 야외 활동은 가능한 줄이세요.";
        }

        if ("권장".equals(grade)) {
            return "피부 관리 위험도는 권장 단계입니다. 자외선 차단제를 바르고, 햇빛이 강한 시간대에는 모자나 긴 소매를 활용하는 것이 좋습니다.";
        }

        if ("보통".equals(grade)) {
            return "피부 관리 위험도는 보통입니다. 짧은 외출은 큰 부담이 적지만, 야외에 오래 머문다면 자외선 차단제를 사용하는 것이 좋습니다.";
        }

        if ("안전".equals(grade)) {
            return "피부 관리 위험도는 안전 단계입니다. 다만 피부가 예민한 편이라면 기본적인 보습과 자외선 차단 습관은 유지하는 것이 좋습니다.";
        }

        return "피부 위험도 등급이 없어 피부 관리 추천을 만들지 못했습니다.";
    }

    private String buildRespiratoryGuide(PersonalAdvice advice) {
        String grade = advice.respiratoryRiskGrade;

        if ("필수".equals(grade)) {
            return "호흡기 관리 위험도는 필수 단계입니다. 외출 시 마스크 착용을 강하게 추천드리며, 창문 환기는 짧게 하고 실내 공기질 관리에 신경 써 주세요.";
        }

        if ("권장".equals(grade)) {
            return "호흡기 관리 위험도는 권장 단계입니다. 오래 걷거나 운동할 계획이 있다면 미세먼지 노출을 줄이고, 필요할 때 마스크를 착용하는 것이 좋습니다.";
        }

        if ("보통".equals(grade)) {
            return "호흡기 관리 위험도는 보통입니다. 일반적인 활동은 가능하지만, 비염·천식 등 민감 증상이 있다면 외출 후 세안과 물 섭취를 챙겨 주세요.";
        }

        if ("안전".equals(grade)) {
            return "호흡기 관리 위험도는 안전 단계입니다. 외출 부담은 비교적 낮지만, 건조함을 느낀다면 물을 자주 마시고 실내 습도를 적절히 유지하세요.";
        }

        return "호흡기 위험도 등급이 없어 호흡기 관리 추천을 만들지 못했습니다.";
    }

    private String buildFinalGuide(PersonalAdvice advice) {
        return "오늘은 온도 " + advice.temperatureRiskGrade + ", 피부 " + advice.skinRiskGrade+ ", 호흡기 " + advice.respiratoryRiskGrade + " 등급 기준으로 생활 가이드를 확인하면 됩니다.";
    }

}