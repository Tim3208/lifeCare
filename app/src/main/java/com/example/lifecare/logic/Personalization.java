package com.example.lifecare.logic;

import com.example.lifecare.db.AppDatabase;
import com.example.lifecare.model.PersonalAdvice;
import com.example.lifecare.model.UserSensitivity;
import com.example.lifecare.model.weatherApiInfo;
import com.example.lifecare.model.uvApiInfo;
import com.example.lifecare.model.UserProfile;
import com.example.lifecare.model.apiInfo;
import com.example.lifecare.api.weatherInfo;
import com.example.lifecare.api.DustInfo;
import com.example.lifecare.api.uvInfo;

public class Personalization {

    private AppDatabase db;

    SensitivityInitializer si = new SensitivityInitializer();

    weatherInfo wi = new weatherInfo();
    DustInfo di = new DustInfo();

    uvInfo ui = new uvInfo();




    public PersonalAdvice testCalculate() {

        UserSensitivity sensitivity = new UserSensitivity();

        sensitivity.temperatureSensitivity = 5;
        sensitivity.skinSensitivity = 5;
        sensitivity.respiratorySensitivity = 5;

        weatherApiInfo weather = new weatherApiInfo();
        weather.tmp = "25";
        weather.reh = "50";
        weather.wsd = "2";

        uvApiInfo uv = new uvApiInfo();
        uv.todayUv = "4";

        apiInfo dust = new apiInfo();
        dust.pm10Text = "30";
        dust.pm25Text = "15";

        String temperatureGrade =
                calcualte_temperature(
                        weather,
                        sensitivity.temperatureSensitivity
                );

        String skinGrade =
                calculate_skin(
                        uv,
                        sensitivity.skinSensitivity
                );

        String respiratoryGrade =
                calculate_respiratory(
                        dust,
                        weather,
                        sensitivity.respiratorySensitivity
                );

        return new PersonalAdvice(
                temperatureGrade,
                skinGrade,
                respiratoryGrade
        );
    }





    //지역명을 입력값으로 받아 PersonalAdvice 객체를 반환 -> 지역에 따른 온도등급, 호흡기/피부 위험도
    //아마 최졷결과화면에서 지역명을 입력받는 식으로 개발할듯
    public PersonalAdvice calculate(String livingRegion, int user_id) throws Exception{
        //지역명 변환함수? 가 완성되면 이곳에서 사용해 함수별로 필요한 값을 얻음

        //임시
        String nx = "0";
        String ny = "0";
        String areaNo = "0";
        String locationText = "0";
        //db에서 사용자 민감도를 가져옴
        UserSensitivity sensitivity = db.userSensitivityDao().getByUserId(user_id);

        if (sensitivity == null) {//민감도가 없으면 초기 민감도 생성
            UserProfile profile = db.userProfileDao().getCurrentProfile();
            sensitivity = SensitivityInitializer.createInitialSensitivity(profile);

            db.userSensitivityDao().insertInitial(sensitivity);
        }
        weatherApiInfo weather = wi.fetchWeatherInfo(nx, ny);
        uvApiInfo uv = ui.fetchUvInfo(areaNo);
        apiInfo dust = di.fetchDustInfo(locationText);

        String temperatureGrade = calcualte_temperature(weather, sensitivity.temperatureSensitivity);

        String skinGrade = calculate_skin(uv, sensitivity.skinSensitivity);

        String respiratoryGrade = calculate_respiratory(dust, weather, sensitivity.respiratorySensitivity);

        return new PersonalAdvice(temperatureGrade, skinGrade, respiratoryGrade);


    }
    //온도 위험도 계산 -> 매우추움/추움/보통/더움/매우더움 반환 -> 개인화 체감온도 등급
    private String calcualte_temperature(weatherApiInfo weather, double sensitivity){
        double tmp = toDouble(weather.tmp);
        double reh = toDouble(weather.reh);
        double wsd = toDouble(weather.wsd);

        double rt = tmp; //현재온도에 습도/풍속/민감도를 고려해 개인화 체감온도 생성
        //습도 반영
        if (reh >= 0) {
            if (tmp >= 26) { //더울때 습도가 높으면 체감온도 향상
                if (reh >= 85) {
                    rt += 3.0;
                } else if (reh >= 75) {
                    rt += 2.0;
                } else if (reh >= 65) {
                    rt += 1.0;
                }
            } else if (tmp <= 15) {//추울때 건조하면 체감온도 하락
                if (reh < 30) {
                    rt -= 1.0;
                } else if (reh < 40) {
                    rt -= 0.5;
                }
            }
        }
        //풍속 반영
        if (wsd >= 0) {
            if (tmp <= 15) { //추울때 바람이 강하면 더 추워짐
                if (wsd >= 7) {
                    rt -= 3.0;
                } else if (wsd >= 4) {
                    rt -= 1.5;
                } else if (wsd >= 2) {
                    rt -= 0.5;
                }
            } else if (tmp >= 26) { //더울때 바람이 강하면 덜 더워짐
                if (wsd >= 7) {
                    rt -= 1.0;
                } else if (wsd >= 4) {
                    rt -= 0.5;
                }
            }
        }
        //온도 민감도 반영
        rt += (sensitivity - 5.0) * 0.8;

        if (rt <= 5) {
            return "매우추움";
        } else if (rt < 17) {
            return "추움";
        } else if (rt < 27) {
            return "보통";
        } else if (rt < 32) {
            return "더움";
        } else {
            return "매우더움";
        }
    }
    //피부(자외선) 위험도 계산
    private String calculate_skin(uvApiInfo uv, double sensitivity){
        double uvRisk = toDouble(uv.todayUv);

        double personalRisk = applyRiskSensitivity(uvRisk, sensitivity);

        return toRiskGrade(personalRisk);

    }
    //호흡기 위험도 계산
    private String calculate_respiratory(apiInfo dust, weatherApiInfo weather, double respiratorySensitivity){

        double pm10Risk = 0;
        double pm25Risk = 0;

        double pm10Value = toDouble(dust.pm10Text);
        double pm25Value = toDouble(dust.pm25Text);

        pm10Risk = calculatePm10Risk(pm10Value);
        pm25Risk = calculatePm25Risk(pm25Value);

        double dustRisk = Math.max(pm10Risk, pm25Risk);

        double humidityRisk = calculateRespiratoryHumidityRisk(weather);

        double environmentRisk = dustRisk + humidityRisk;

        double personalRisk = applyRiskSensitivity(environmentRisk, respiratorySensitivity);

        return toRiskGrade(personalRisk);
    }
    //미세먼지 수치 -> 위험도 변환
    private double calculatePm10Risk(double pm10Value) {
        if (pm10Value < 0) {
            return 0;
        }
        if (pm10Value <= 30) { //낮음
            return 1.5;
        } else if (pm10Value <= 80) {//보통
            return 4.0;
        } else if (pm10Value <= 150) {//높음
            return 7.0;
        } else { //매우 높음
            return 9.0;
        }
    }
    //초미세먼지 수치 -> 위험도 변환
    private double calculatePm25Risk(double pm25Value) {
        if (pm25Value < 0) {
            return 0;
        }

        if (pm25Value <= 15) { //낮음
            return 1.5;
        } else if (pm25Value <= 35) {//보통
            return 4.0;
        } else if (pm25Value <= 75) {//높음
            return 7.0;
        } else { //매우 높음
            return 9.0;
        }
    }
    //습도 -> 위험도 변환
    private double calculateRespiratoryHumidityRisk(weatherApiInfo weather) {

        double reh = toDouble(weather.reh);

        if (reh < 0) {
            return 0;
        }

        //너무 건조한 경우
        if (reh < 25) {
            return 2.5;
        } else if (reh < 35) {
            return 2.0;
        } else if (reh < 45) {
            return 1.0;
        }

        //너무 습한 경우
        if (reh >= 90) {
            return 1.5;
        } else if (reh >= 80) {
            return 1.0;
        }

        return 0;
    }


    //문자열을 숫자(double)로 변환
    private double toDouble(String value) {

        //"25℃", "60%", "3.5m/s" 같은 문자열에서도 숫자만 추출
        String num = value.trim().replaceAll("[^0-9.\\-]", "");

        return Double.parseDouble(num);
    }
    //환경 수치와 민감도를 더함
    private double applyRiskSensitivity(double environmentRisk, double sensitivity) {

        double multiplier = 0.5 + sensitivity / 10.0;

        return environmentRisk * multiplier;
    }
    //위험도를 등급으로 변환
    private String toRiskGrade(double score) {
        if (score < 2.5) {
            return "안전";
        } else if (score < 5.0) {
            return "보통";
        } else if (score < 7.5) {
            return "권장";
        } else {
            return "필수";
        }
    }

}
