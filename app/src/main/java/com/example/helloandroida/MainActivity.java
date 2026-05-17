package com.example.helloandroida;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.content.Intent;
import android.widget.Button;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader; //API 응답 데이터를 한 줄씩 읽을 때 사용
import java.io.InputStreamReader; //바이트 데이터를 글자로 바꿔줌.
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL; //API 주소를 객체로 만듬
import java.net.URLEncoder; //한글 지역명을 URL에 맞게 변환

import com.example.helloandroida.model.apiInfo;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    public static final String API_key = "d8f56429814a9f42a71a1308543224a5d1cc362b2268a5930af0d80917702a2c";
    private EditText locationText;
    private TextView resultText;
    private Button button;
    private TextView gradeText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /** 이거 안쓰면 앱 이상해질 수 있음. 안드로이드 기본 초기화 작업 (직관적인 의미는, 부모(super)의 onCreate도 실행하겠다는 의미) */
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        /**
         * activity_main.xml 화면을 연결하라는 의미 (xml은 화면 생김새, java는 동작처리)
         * R은 안드로이드가 자동 생성하는 리소스 주소표
         * */
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                //대시보드 버튼을 눌렀을 때 실행할 코드 작성
                return true;
            } else if (id == R.id.nav_health) {
                //건강 정보 버튼을 눌렀을 때 실행할 코드 작성
                return true;
            } else if (id == R.id.nav_guide) {
                //맞춤 가이드 버튼을 눌렀을 때, UserInfoActivity로 이동
                Intent intent = new Intent(MainActivity.this, UserInfoActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_more) {
                //더보기 버튼을 눌렀을 때 실행할 코드 작성
                return true;
            }

            return false;
        });


        locationText = findViewById(R.id.locationText);
        resultText = findViewById(R.id.resultText);
        button = findViewById(R.id.button);
        gradeText = findViewById(R.id.gradeText);

        button.setOnClickListener(new View.OnClickListener() {
            // 버튼을 눌렀을 때 실행될 동작을 설정

            @Override
            public void onClick(View view) {

                loadDustInfo();
                // 미세먼지 정보를 불러오는 함수를 실행합니다.
            }
        });
        /**
         * 사용자 정보 스크린 연결 버튼
         */
//        Button btnOpenUserInfo = findViewById(R.id.btnOpenUserInfo);
//        btnOpenUserInfo.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // UserInfoActivity로 이동하는 코드
//                Intent intent = new Intent(MainActivity.this, UserInfoActivity.class);
//                startActivity(intent);
//            }
//        });
    }

    //API를 통해 정보를 불러오는 함수
    public void loadDustInfo(){
        String locationName = locationText.getText().toString().trim();
        //입력칸이 비어 있으면 메시지 띄우고 함수 종료
        if (locationName.equals("")){
            Toast.makeText(this, "사는 지역을 입력하세요", Toast.LENGTH_SHORT).show();
            return;
        }

        button.setEnabled(false);//버튼 비활성화

        new Thread(new Runnable() { //인터넷 호출을 메인에서 하면 오류 발생 반드시 스레드 사용!!!!!
            @Override
            public void run() {
                try {
                    apiInfo info = fetchApiInfo(locationName);

                    runOnUiThread(new Runnable() { //UI는 백그라운드 스레드에서 건드리면 안됨 오류 발생!!!
                        @Override
                        public void run() {

                            resultText.setText("PM10: " + info.pm10Text + "/ PM2.5: " + info.pm25Text);
                            if (info.pm10Text.equals("-") && info.pm25Text.equals("-")) {
                                gradeText.setText("오늘은 미세먼지와 초미세먼지의 측정값이 없습니다.");
                            }else if (info.pm10Text.equals("-")) {
                                gradeText.setText("오늘의 미세먼지 수치는 측정값이 없으며" + "초미세먼지 수치는 " + info.pm25Grade + "입니다.");
                            }else if (info.pm25Text.equals("-")) {
                                gradeText.setText("오늘의 미세먼지 수치는 " + info.pm10Grade + "이고 초미세먼지는 측정값이 없습니다.");
                            }else {
                                gradeText.setText("오늘의 미세먼지 수치는 " + info.pm10Grade + ", 초미세먼지 수치는 " + info.pm25Grade + "입니다.");
                            }
                            button.setEnabled(true);
                        }
                    });

                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "조회 실패", Toast.LENGTH_SHORT).show();
                }
            }
        }).start();
    }

    private apiInfo fetchApiInfo(String locationText) throws Exception{

        String apiUrl = buildApiUrl(locationText);
        //API 주소 생성, 사용자애게 입력받은 주소 이용

        HttpURLConnection connection = null;
        // 서버와 연결하기 위한 객체입니다
        try{
            //인터넷 연결과 데이터 읽기 작업을 try 안에서 실행합니다.

            URL url = new URL(apiUrl);
            //문자열 API 주소를 URL 객체로 변환

            connection = (HttpURLConnection) url.openConnection();
            //URL에 접속할 수 있는 연결 객체를 만듬

            connection.setRequestMethod("GET");
            //GET 방식으로 API를 호출


            connection.setConnectTimeout(10000);
            //서버 연결을 최대 10초까지 기다림

            connection.setReadTimeout(10000);
            //서버 응답을 읽는 시간을 최대 10초까지 기다림

            int responseCode = connection.getResponseCode();
            //서버 응답 코드를 가져옴.

            InputStream inputStream = null;
            //서버 응답 데이터를 읽을 통로를 선언

            if (responseCode >= 200 && responseCode < 300) {
                //응답 코드가 200번대이면 정상

                inputStream = connection.getInputStream();
                //응답 데이터를 읽어옴

            }

            String response = readStream(inputStream);
            //서버에서 받은 데이터를 문자열로 변환

            if (responseCode != 200) {
                //응답 코드가 정확히 200이 아니면 오류

                throw new Exception();
                //예외를 발생시킴
            }

            JSONObject root = new JSONObject(response);
            //API 응답 문자열을 JSON 객체로 변환

            JSONObject responseObj = root.getJSONObject("response");
            //JSON에서 response 부분을 꺼냄

            JSONObject header = responseObj.getJSONObject("header");
            //response 안에 있는 header 부분을 꺼냄 -> 결과 코드와 결과 메시지가 들어있음

            String resultCode = header.optString("resultCode");
            //resultCode는 API 호출 성공 여부를 알려줌 -> 00이면 정상

            if (!"00".equals(resultCode)) {
                //00이 아닌경우

                throw new Exception();
            }

            JSONObject body = responseObj.getJSONObject("body");
            //response 안의 body 부분을 꺼냄 -> 실제 데이터가 들어있음

            JSONArray items = body.getJSONArray("items");
            //body 안의 items 배열을 꺼냄 -> 미세먼지 측정값 목록이 들어 있음

            JSONObject item = items.getJSONObject(0);
            // items 배열에서 데이터를 꺼냅니다. ->배열에 가장 최근 측정값 하나만 들어있음 따라서 첫 번째 데이터만 사용

            String pm10Text = item.optString("pm10Value", "-");
            //PM10 값을 꺼냄 -> 일반 미세먼지 수치, 값이 없을 경우 에러대신 - 삽입

            String pm25Text = item.optString("pm25Value", "-");
            //PM2.5 값을 꺼냄. -> 초미세먼지 수치
            int pm10, pm25;
            if (pm10Text.equals("-"))
                pm10 = -1;
            else
                pm10 = Integer.parseInt(pm10Text);
            //pm10 값을 숫자로 변환, - 일 경우 -1 삽입.

            if (pm25Text.equals("-"))
                pm25 = -1;
            else
                pm25 = Integer.parseInt(pm25Text);
            //pm25 값을 숫자로 변환, - 일 경우 -1 삽입.

            String pm10Grade = getPm10Grade(pm10);
            //pm10 수치 등급 계산 함수

            String pm25Grade = getPm25Grade(pm25);
            //pm25 수치 등급 계산 함수


            apiInfo info = new apiInfo();
            //화면에 표시할 정보를 담을 apiInfo 객체 생성

            info.locationName = locationText;

            info.pm10Text = pm10Text;

            info.pm25Text = pm25Text;

            info.pm10Grade = pm10Grade;

            info.pm25Grade = pm25Grade;

            return info;
            //완성된 apiInfo 객체를 반환
        } finally {
            if (connection != null) {

                connection.disconnect();
                //서버 연결을 종료
            }
        }
    }

    // API 요청 주소를 만드는 함수(한국 환경공단, 에어코리아 자료 참고)
    private String buildApiUrl(String locationName) throws Exception {

        String baseUrl = "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty";
        // 요청 메시지 앞부분
        return baseUrl

                + "?serviceKey=" + API_key
                //발급받은 API 키

                + "&returnType=json"
                //응답 형식은 json.

                + "&numOfRows=1"
                //한 페이지 결과수 -> 가장 최근 측정값 하나만 받음

                + "&pageNo=1"
                //1페이지 데이터를 요청.

                + "&stationName=" + URLEncoder.encode(locationName, "UTF-8")
                //측정소명을 URL에 삽입. -> 한글은 URL에서 깨질 수 있으므로 UTF-8로 인코딩

                + "&dataTerm=DAILY"
                //하루 기준 데이터를 요청

                + "&ver=1.3";
        //API 버전을 지정.
    }

    private String getPm10Grade(int pm10) {
        //pm10 수치에 따라 등급을 계산하는 함수 -> 추후 저장된 사용자 데이터를 끌어와 개인화

        if (pm10 < 0) return "측정값 없음";
        //-1이면 측정값이 없는 경우

        if (pm10 <= 30) return "좋음";

        if (pm10 <= 80) return "보통";

        if (pm10 <= 150) return "나쁨";

        return "매우나쁨";
    }

    private String getPm25Grade(int pm25) {
        //pm25 수치에 따라 등급을 계산하는 함수

        if (pm25 < 0) return "측정값 없음";

        if (pm25 <= 15) return "좋음";

        if (pm25 <= 35) return "보통";

        if (pm25 <= 75) return "나쁨";

        return "매우나쁨";
    }

    private String readStream(InputStream inputStream) throws Exception {
        //InputStream으로 들어온 서버 응답을 문자열로 바꾸는 함수

        if (inputStream == null) {
            //읽을 데이터가 없는 경우.

            return "";
            //빈 문자열을 반환
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        //BufferedReader: 문자 데이터를 효율적으로 읽기 위한 클래스

        StringBuilder builder = new StringBuilder();
        //StringBuilder: 문자열을 이어붙일 때 사용하는 클래스

        String line;
        //한 줄 씩 읽은 내용을 저장하는 변수

        while (true) {
            line = reader.readLine(); //서버 응답을 한 줄씩 읽음
            if(line == null)
                break;
            //더 이상 읽을 줄이 없으면 종료

            builder.append(line);
            //읽은 한 줄을 builder에 추가.
        }

        reader.close();

        return builder.toString();
        //응답 문자열을 반환
    }
}