package com.example.lifecare.location;

public interface OnLocationSearchListener {

    void onSearchSuccess(
            String nx, // 기상청 가로 격자 번호
            String ny, // 기상청 세로 격자 번호
            String areaNo, // 동네 번호
            String locationText //측정소 이름
    );

    void onSearchFailure();
}