package com.example.lifecare.api;
import com.example.lifecare.model.apiInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL; //API 주소를 객체로 만듬
import java.net.URLEncoder; //한글 지역명을 URL에 맞게 변환
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;

public class DustInfo {
    public static final String Dust_API_key = "d8f56429814a9f42a71a1308543224a5d1cc362b2268a5930af0d80917702a2c";

    // 중요!! API는 측정소명 기준으로 동작 ex)강남구, 가평 -> 측정소명의 기준이 모두 다름 (읍, 면, 동, 그냥 이름등 다양)
    // 추후 지역명을 정리하는 기능 개발 필요
    public apiInfo fetchDustInfo(String locationText) throws Exception {

        String apiUrl = buildApiUrl(locationText);
        //API 주소 생성, 사용자애게 입력받은 주소 이용

        HttpURLConnection connection = null;
        // 서버와 연결하기 위한 객체입니다
        try {
            //인터넷 연결과 데이터 읽기 작업을 try 안에서 실행.

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

                + "?serviceKey=" + Dust_API_key
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

    private String readStream(InputStream inputStream) throws Exception {

        if (inputStream == null) {
            return "";
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, "UTF-8")
        );

        StringBuilder builder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        reader.close();

        return builder.toString();
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
}
