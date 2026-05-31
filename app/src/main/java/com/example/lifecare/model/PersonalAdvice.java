package com.example.lifecare.model;

public class PersonalAdvice {
    public String temperatureRiskGrade;  //온도 위험도 등급
    public String respiratoryRiskGrade;  //호흡기 위험도 등급
    public String skinRiskGrade;         //피부 위험도 등급

    public PersonalAdvice( //생성자
                           String temperatureRiskGrade,
                           String skinRiskGrade,
                           String respiratoryRiskGrade
    ) {
        this.temperatureRiskGrade = temperatureRiskGrade;
        this.skinRiskGrade = skinRiskGrade;
        this.respiratoryRiskGrade = respiratoryRiskGrade;
    }

}
