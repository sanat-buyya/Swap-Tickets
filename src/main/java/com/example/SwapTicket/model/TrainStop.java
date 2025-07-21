package com.example.SwapTicket.model;

public class TrainStop {
    private String trainNo;
    private String trainName;
    private String stationCode;
    private String arrivalTime;
    private String departureTime;

    public TrainStop(String trainNo, String trainName, String stationCode, String arrivalTime, String departureTime) {
        this.trainNo = trainNo;
        this.trainName = trainName;
        this.stationCode = stationCode;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
    }

    // Getters
    public String getTrainNo() { return trainNo; }
    public String getTrainName() { return trainName; }
    public String getStationCode() { return stationCode; }
    public String getArrivalTime() { return arrivalTime; }
    public String getDepartureTime() { return departureTime; }
}  