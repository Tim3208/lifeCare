package com.example.lifecare.api;

import com.example.lifecare.model.uvApiInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.example.lifecare.util.StringConversion;

public class uvInfo {

    String uv_API_key = "e5ff912d567087c0a95c6574dd728ff899c5422d182b1a0cf9374cbc9350720d";

    public uvApiInfo fetchUvInfo(String areaNo) throws Exception {
        String time = criteriaTime();

        return fetchUvInfo(areaNo, time);
    }

    public uvApiInfo fetchUvInfo(String areaNo, String time) throws Exception {
        String apiUrl = buildApiUrl_uv(areaNo, time);

        HttpURLConnection connection = null;

        StringConversion sc = new StringConversion();

        try {
            URL url = new URL(apiUrl);

            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();

            InputStream inputStream;

            if (responseCode >= 200 && responseCode < 300) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }

            String response = sc.readStream(inputStream);

            if (responseCode != 200) {
                throw new Exception("자외선 API HTTP 에러 코드: " + responseCode);
            }

            if (response == null || !response.trim().startsWith("{")) {
                throw new Exception("자외선 API 응답 형식 오류: " + response);
            }

            JSONObject root = new JSONObject(response);

            JSONObject responseObj = root.getJSONObject("response");

            JSONObject header = responseObj.getJSONObject("header");
            String resultCode = header.optString("resultCode");

            if (!"00".equals(resultCode)) {
                throw new Exception("자외선 API 응답 에러 코드: " + resultCode);
            }

            JSONObject body = responseObj.getJSONObject("body");

            JSONArray items = getItemArray(body);

            uvApiInfo uai = new uvApiInfo();

            uai.areaNo = areaNo;
            uai.requestTime = time;
            uai.requestHour = time.substring(8, 10);

            if (items.length() > 0) {
                JSONObject item = items.getJSONObject(0);

                uai.todayUv = emptyToChecking(item.optString("today", "").trim());
                uai.tomorrowUv = emptyToChecking(item.optString("tomorrow", "").trim());
                uai.dayAfterTomorrowUv = emptyToChecking(item.optString("dayaftertomorrow", "").trim());

                uai.hasData = true;
            } else {
                uai.todayUv = "점검 중";
                uai.tomorrowUv = "점검 중";
                uai.dayAfterTomorrowUv = "점검 중";

                uai.hasData = false;
            }

            return uai;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String buildApiUrl_uv(String areaNo, String time) throws Exception {
        String baseUrl = "https://apis.data.go.kr/1360000/LivingWthrIdxServiceV3/getUVIdxV3";

        return baseUrl
                + "?serviceKey=" + URLEncoder.encode(uv_API_key, "UTF-8")
                + "&dataType=JSON"
                + "&numOfRows=1"
                + "&pageNo=1"
                + "&areaNo=" + URLEncoder.encode(areaNo, "UTF-8")
                + "&time=" + URLEncoder.encode(time, "UTF-8");
    }

    private String criteriaTime() {
        Calendar cal = Calendar.getInstance();

        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm", Locale.KOREA);
        int currentTime = Integer.parseInt(timeFormat.format(cal.getTime()));

        String targetHour;

        if (currentTime < 900) {
            cal.add(Calendar.DATE, -1);
            targetHour = "18";
        } else if (currentTime < 2100) {
            targetHour = "06";
        } else {
            targetHour = "18";
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
        String date = dateFormat.format(cal.getTime());

        return date + targetHour;
    }

    private JSONArray getItemArray(JSONObject body) throws Exception {
        Object itemsObj = body.get("items");

        if (itemsObj instanceof JSONArray) {
            return (JSONArray) itemsObj;
        }

        JSONObject items = body.getJSONObject("items");

        return items.getJSONArray("item");
    }

    private String emptyToChecking(String value) {
        if (value == null || value.equals("")) {
            return "점검 중";
        }

        return value;
    }
}