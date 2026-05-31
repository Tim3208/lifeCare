package com.example.lifecare.model;

public class uvApiInfo {
    public String areaNo;          // 지역 코드
    public String requestTime;     // 요청 시간
    public String requestHour;     // 요청 시각 (06, 18 등)

    public String todayUv;         // 오늘 자외선
    public String tomorrowUv;      // 내일
    public String dayAfterTomorrowUv; // 모레

    public boolean hasData;        // 데이터 존재 여부
}
