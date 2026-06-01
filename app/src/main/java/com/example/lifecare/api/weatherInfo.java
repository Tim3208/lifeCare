package com.example.lifecare.api;
import com.example.lifecare.model.weatherApiInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;
import android.util.Log;

import com.example.lifecare.util.StringConversion;

public class weatherInfo {

    String weather_API_key = "https://apihub.kma.go.kr/api/typ01/cgi-bin/url/nph-dfs_shrt_grd?tmfc=2024022505&tmef=2024022506&vars=TMP&authKey=Q-n1Z1EwQcqp9WdRMCHKAw";


    public weatherApiInfo fetchWeatherInfo(String nx, String ny) throws Exception {
        String current_date = currentDate();
        String current_time = criteriaTime();
        String apiUrl = buildAPiUrl_weather(nx, ny, current_date, current_time);
        //API 주소 생성, 사용자애게 입력받은 주소 이용

        Log.d("WEATHER_TEST", "URL = " + apiUrl);

        HttpURLConnection connection = null;
        // 서버와 연결하기 위한 객체입니다

        StringConversion sc = new StringConversion();
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

            Log.d("WEATHER_TEST", "responseCode = " + responseCode);

            InputStream inputStream = null;
            //서버 응답 데이터를 읽을 통로를 선언

            if (responseCode >= 200 && responseCode < 300) {
                //응답 코드가 200번대이면 정상

                inputStream = connection.getInputStream();
                //응답 데이터를 읽어옴

            } else {
                //응답 코드가 200번대가 아니면 오류

                inputStream = connection.getErrorStream();
                //오류 데이터를 읽어옴
            }

            String response = sc.readStream(inputStream);
            //서버에서 받은 데이터를 문자열로 변환

            Log.d("WEATHER_RESPONSE", response);

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

            JSONObject items = body.getJSONObject("items");
            //body 안의 items을 꺼냄 -> 미세먼지 측정값 목록이 들어 있음
            JSONArray itemarray = items.getJSONArray("item");
            //items 에서 itemarray 배열를 꺼냄
            weatherApiInfo wai = new weatherApiInfo();
            //itemarray 안에 들어 있는 정보들을 떠냄
            for (int i = 0; i < itemarray.length(); i++) {
                //itemarray배열의 item을 꺼냄
                JSONObject item = itemarray.getJSONObject(i);

                String category = item.optString("category", "");
                String value = item.optString("fcstValue", "-");

                switch (category) {
                    case "TMP":
                        wai.tmp = value;
                        break;

                    case "TMN":
                        wai.tmn = value;
                        break;

                    case "TMX":
                        wai.tmx = value;
                        break;

                    case "WSD":
                        wai.wsd = value;
                        break;

                    case "POP":
                        wai.pop = value;
                        break;
                    case "PTY":
                        wai.pty = convertPty(value);
                        break;
                    case "SKY":
                        wai.sky = convertSky(value);
                        break;
                    case "REH":
                        wai.reh = value;
                        break;

                }
            }
            return wai;

        } finally {
            if (connection != null) {

                connection.disconnect();
                //서버 연결을 종료
            }
        }
    }
    private String buildAPiUrl_weather (String nx, String ny, String date, String time) throws Exception {
        String baseUrl = "https://apihub.kma.go.kr/api/typ02/openApi/VilageFcstInfoService_2.0/getVilageFcst";
        //요청메시지 앞부분(URL)
        return baseUrl
                + "?authKey=" + java.net.URLEncoder.encode(weather_API_key, "UTF-8")
                + "&pageNo=1"
                + "&numOfRows=1000"
                + "&dataType=JSON"
                + "&base_date=" + date
                + "&base_time=" + time //단기예보 생산 주기는 일 8회 2시부터 3시간 간격
                + "&nx=" + nx
                + "&ny=" + ny;

    }
    //현재 날짜를 가져오는 함수
    private String currentDate() {
        Calendar cal = Calendar.getInstance();

        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm", Locale.KOREA);
        int ctime = Integer.parseInt(timeFormat.format(cal.getTime()));

        //새벽 0210 전에는 전날 2300 발표자료를 써야 함
        if (ctime < 210) {
            cal.add(Calendar.DATE, -1);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
        return dateFormat.format(cal.getTime());
    }
    //현재 시간을 가져오는 함수
    //단기예보 생산 주기는 일 8회 2시부터 3시간 간격이므로 그에 맞게 변환
    private String criteriaTime(){
        SimpleDateFormat sdt = new SimpleDateFormat("HHmm", Locale.KOREA);
        String cutime = sdt.format(new Date());
        int  ctime = Integer.parseInt(cutime);

        if (ctime < 210)
            return "2300";
        else if (ctime < 510)
            return "0200";
        else if (ctime < 810)
            return "0500";
        else if (ctime < 1110)
            return "0800";
        else if (ctime < 1410)
            return "1100";
        else if (ctime < 1710)
            return "1400";
        else if (ctime < 2010)
            return "1700";
        else if (ctime < 2310)
            return "2000";
        else
            return "2300";
    }
    //숫자로 오는 sky값을 변환해주는 함수
    private String convertSky(String sky) {
        switch (sky) {
            case "1":
                return "맑음";
            case "3":
                return "구름많음";
            case "4":
                return "흐림";
            default:
                return sky;
        }
    }
    //숫자로 오는 pty값을 변환해주는 함수
    private String convertPty(String pty) {
        switch (pty) {
            case "0":
                return "없음";
            case "1":
                return "비";
            case "2":
                return "비/눈";
            case "3":
                return "눈";
            case "4":
                return "소나기";
            default:
                return pty;
        }
    }
}

