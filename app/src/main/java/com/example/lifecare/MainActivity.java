package com.example.lifecare;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lifecare.api.DustInfo;
import com.example.lifecare.model.apiInfo;

import com.example.lifecare.api.weatherInfo;
import com.example.lifecare.model.weatherApiInfo;
import com.example.lifecare.logic.Personalization;
import com.example.lifecare.model.PersonalAdvice;
import com.example.lifecare.location.AirStationSearchTask;
import com.example.lifecare.location.OnLocationSearchListener;
import com.example.lifecare.util.StringConversion;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {


    // 공공데이터포털 미세먼지 API Key
    public static final String API_key = "d8f56429814a9f42a71a1308543224a5d1cc362b2268a5930af0d80917702a2c";

    // 공공데이터포털 자외선 API Key (영문+숫자 64자리)
    public static final String UV_API_key = "e5ff912d567087c0a95c6574dd728ff899c5422d182b1a0cf9374cbc9350720d";

    private EditText locationText;
    private TextView resultText;
    private TextView gradeText;
    private Button button;

    // 결과 저장
    private String dustResult = "";
    private String uvResult = "";

    // 위치 정보 저장
    private String nx;
    private String ny;
    private String areaNo;
    private String stationName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationText = findViewById(R.id.locationText);
        resultText = findViewById(R.id.resultText);
        gradeText = findViewById(R.id.gradeText);
        button = findViewById(R.id.button);

        button.setOnClickListener(view -> {

            String locationName = locationText.getText().toString().trim();

            if (locationName.equals("")) {
                Toast.makeText(this, "사는 지역을 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }

            button.setEnabled(false);

            new AirStationSearchTask(
                    new StringConversion(),
                    new OnLocationSearchListener() {

                        @Override
                        public void onSearchSuccess(
                                String nx,
                                String ny,
                                String areaNo,
                                String locationText
                        ) {
                            Log.d("LOCATION_TEST", "nx = " + nx);
                            Log.d("LOCATION_TEST", "ny = " + ny);

                            new Thread(() -> {

                                try {

                                    weatherInfo wi =
                                            new weatherInfo();

                                    weatherApiInfo weather =
                                            wi.fetchWeatherInfo(nx, ny);

                                    Log.d(
                                            "WEATHER_TEST",
                                            "기온 = " + weather.tmp
                                    );

                                    Log.d(
                                            "WEATHER_TEST",
                                            "습도 = " + weather.reh
                                    );

                                    Log.d(
                                            "WEATHER_TEST",
                                            "하늘 = " + weather.sky
                                    );

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }).start();
                        }

                        @Override
                        public void onSearchFailure() {

                            Log.d("LOCATION_TEST", "실패");

                        }
                    }
            ).execute(locationName);

            dustResult = "미세먼지 로딩 중...";
            uvResult = "자외선 로딩 중...";
            updateUI();

            // 미세먼지 (DustInfo 클래스 사용)
            loadDustInfo(locationName);

            // 자외선
            SimpleDateFormat sdf = new SimpleDateFormat("HHmm", Locale.getDefault());
            int currentTime = Integer.parseInt(sdf.format(new Date()));

            String uvTime = getUvCriteriaTime(currentTime);

            loadUvInfo("1111000000", uvTime);
        });
    }

    // ======================
    // UI 업데이트
    // ======================
    private void updateUI() {
        runOnUiThread(() -> {
            resultText.setText(dustResult + "\n" + uvResult);
        });
    }

    // ======================
    // 미세먼지
    // ======================
    private void loadDustInfo(String locationName) {
        new Thread(() -> {
            try {
                DustInfo dustInfo = new DustInfo();
                apiInfo info = dustInfo.fetchDustInfo(locationName);

                dustResult = "[미세먼지] PM10: " + info.pm10Text + " / PM2.5: " + info.pm25Text;

                runOnUiThread(() -> {
                    if (info.pm10Text.equals("-") && info.pm25Text.equals("-")) {
                        gradeText.setText("측정값 없음");
                    } else {
                        gradeText.setText("PM10: " + info.pm10Grade + ", PM2.5: " + info.pm25Grade);
                    }
                });

                updateUI();

            } catch (Exception e) {
                e.printStackTrace();
                dustResult = "[미세먼지] 오류 발생";
                updateUI();
            }
        }).start();
    }

    // ======================
    // 자외선
    // ======================
    private void loadUvInfo(String areaNo, String time) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String apiUrl = "https://apis.data.go.kr/1360000/LivingWthrIdxServiceV3/getUVIdxV3"
                        + "?serviceKey=" + URLEncoder.encode(UV_API_key, "UTF-8")
                        + "&dataType=JSON"
                        + "&numOfRows=1"
                        + "&pageNo=1"
                        + "&areaNo=" + URLEncoder.encode(areaNo, "UTF-8")
                        + "&time=" + URLEncoder.encode(time, "UTF-8");

                URL url = new URL(apiUrl);
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");

                InputStream inputStream = connection.getInputStream();
                String response = readStream(inputStream);

                JSONObject root = new JSONObject(response);
                JSONObject body = root.getJSONObject("response").getJSONObject("body");
                JSONArray items = body.getJSONArray("items");

                if (items.length() > 0) {
                    JSONObject item = items.getJSONObject(0);
                    String todayUv = item.optString("today", "정보 없음");
                    uvResult = "[자외선] " + todayUv;
                } else {
                    uvResult = "[자외선] 데이터 없음";
                }

                updateUI();

            } catch (Exception e) {
                e.printStackTrace();
                uvResult = "[자외선] 오류 발생";
                updateUI();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    // ======================
    // 자외선 시간 계산
    // ======================
    private String getUvCriteriaTime(int timeInt) {
        Calendar cal = Calendar.getInstance();
        String targetHour;

        if (timeInt < 900) {
            cal.add(Calendar.DATE, -1);
            targetHour = "18";
        } else if (timeInt < 2100) {
            targetHour = "06";
        } else {
            targetHour = "18";
        }

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String dateStr = sdfDate.format(cal.getTime());

        return dateStr + targetHour;
    }

    // ======================
    // 스트림 읽기
    // ======================
    private String readStream(InputStream inputStream) throws Exception {

        if (inputStream == null) return "";

        java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(inputStream, "UTF-8")
        );

        StringBuilder builder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        reader.close();
        return builder.toString();
    }
}