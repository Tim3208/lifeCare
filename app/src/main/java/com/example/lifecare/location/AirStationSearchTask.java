package com.example.lifecare.location;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import com.example.lifecare.util.StringConversion;

//사용자가 입력한 동네 이름으로 API에서 정확한 주소와 기상청 좌표를 찾아오는 역할을 하는 독립된 클래스

// 1. 외부에서 사용할 수 있도록 public class로 선언
public class AirStationSearchTask extends AsyncTask<String, Void, Bundle> {

    private static final String UV_API_key = "e5ff912d567087c0a95c6574dd728ff899c5422d182b1a0cf9374cbc9350720d";

    private final OnLocationSearchListener listener;
    private final StringConversion stringConversion;

    // 2. 메인 액티비티의 리스너와 헬퍼 클래스를 전달받는 생성자 추가
    public AirStationSearchTask(StringConversion stringConversion, OnLocationSearchListener listener) {
        this.stringConversion = stringConversion;
        this.listener = listener;
    }

    @Override //독립된 백그라운드 공간에서 실행
    protected Bundle doInBackground(String... params) {
        String userInputAddr = params[0];
        HttpURLConnection conn = null;
        try {
            String encodedAddr = URLEncoder.encode(userInputAddr, "UTF-8");

            String urlStr = "https://apis.data.go.kr/B552584/MsrstnInfoInqireSvc/getMsrstnList"
                    + "?serviceKey=" + URLEncoder.encode(UV_API_key, "UTF-8")
                    + "&returnType=json"
                    + "&numOfRows=1"
                    + "&pageNo=1"
                    + "&addr=" + encodedAddr;

            URL url = new URL(urlStr); //인터넷 연결신호 보내기
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode >= 200 && responseCode <= 300) ? conn.getInputStream() : conn.getErrorStream();
            String responseStr = stringConversion.readStream(is); //JSON으로 변환

            JSONObject jsonObject = new JSONObject(responseStr); //JSON 파싱
            JSONObject response = jsonObject.optJSONObject("response");
            if (response != null) {
                JSONObject body = response.optJSONObject("body");
                if (body != null) {
                    JSONArray items = body.optJSONArray("items");
                    if (items != null && items.length() > 0) {
                        JSONObject targetItem = items.getJSONObject(0);

                        // 메인 스레드로 한 번에 뭉쳐서 보내기 위해 Bundle 이용
                        Bundle resultBundle = new Bundle();
                        // 공식 미세먼지 측정소 이름(예:노원구)을 파싱해서 저장
                        resultBundle.putString("locationTextValue", targetItem.optString("stationName", "0"));

                        String fullAddr = targetItem.optString("addr", "");
                        resultBundle.putString("areaNo", parseAreaNo(fullAddr));
                        // 측정소의 실제 지도 위도/경도(dmX, dmY) 글자 꺼내기
                        String dmX = targetItem.optString("dmX", "0");
                        String dmY = targetItem.optString("dmY", "0");

                        // 좌표가 0이 아니고 정상적인 위치라면 기상청 바둑판 격자 좌표로 변환하기
                        if (!dmX.equals("0") && !dmY.equals("0")) {
                            double lat = Double.parseDouble(dmY);
                            double lon = Double.parseDouble(dmX);
                            LatLonToGrid grid = convertGRID_GPS(lat, lon);
                            // 계산되어 나온 기상청 가로 칸(nx), 세로 칸(ny) 번호를 저장
                            resultBundle.putString("nx", String.valueOf((int) grid.nx));
                            resultBundle.putString("ny", String.valueOf((int) grid.ny));
                        }
                        return resultBundle;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Bundle result) {
        // 3. 기존의 메인 액티비티 직접 조작 대신 인터페이스(창구)를 통해 데이터 패스
        if (result != null && listener != null) {
            listener.onSearchSuccess(
                    result.getString("nx", "0"),
                    result.getString("ny", "0"),
                    result.getString("areaNo", "0"),
                    result.getString("locationTextValue", "0")
            );
        } else if (listener != null) {
            listener.onSearchFailure();
        }
    }

    // 실제 주소 글자를 보고 기상청 자외선 지수 API가 요구하는 10자리 행정구역 코드로 바꿔줍니다.
    private String parseAreaNo(String addr) {
        if (addr.contains("서울")) return "1100000000";
        if (addr.contains("부산")) return "2600000000";
        if (addr.contains("대구")) return "2700000000";
        if (addr.contains("인천")) return "2800000000";
        if (addr.contains("광주")) return "2900000000";
        if (addr.contains("대전")) return "3000000000";
        if (addr.contains("울산")) return "3100000000";
        if (addr.contains("세종")) return "3600000000";
        if (addr.contains("경기")) return "4100000000";
        if (addr.contains("강원")) return "5100000000";
        if (addr.contains("충북") || addr.contains("충청북도")) return "4300000000";
        if (addr.contains("충남") || addr.contains("충청남도")) return "4400000000";
        if (addr.contains("전북") || addr.contains("전라북도")) return "4500000000";
        if (addr.contains("전남") || addr.contains("전라남도")) return "4600000000";
        if (addr.contains("경북") || addr.contains("경상북도")) return "4700000000";
        if (addr.contains("경남") || addr.contains("경상남도")) return "4800000000";
        if (addr.contains("제주")) return "5000000000";
        return "1111000000";
    }

    private class LatLonToGrid { double nx, ny; } // 결과 데이터를 묶어두기 위한 구조체

    /*기상청 격자 좌표 변환기]
     둥근 지구 모양의 위도, 경도 좌표를 대한민국 기상청 고유의 평면 바둑판 지도의
     가로칸 번호(nx)와 세로칸 번호(ny)로 바꾸어 주는 삼각함수 수학 공식 기계입니다.
     */
    private LatLonToGrid convertGRID_GPS(double lat, double lon) {
        double RE = 6371.00877; double GRID = 5.0; double SLAT1 = 30.0; double SLAT2 = 60.0;
        double OLON = 126.0; double OLAT = 38.0; double XO = 43; double YO = 136;
        double DEGRAD = Math.PI / 180.0;
        double re = RE / GRID; double slat1 = SLAT1 * DEGRAD; double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD; double olat = OLAT * DEGRAD;
        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);
        LatLonToGrid result = new LatLonToGrid();
        double ra = Math.tan(Math.PI * 0.25 + (lat) * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);
        double theta = lon * DEGRAD - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;
        result.nx = Math.floor(ra * Math.sin(theta) + XO + 0.5);
        result.ny = Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);
        return result;
    }
}