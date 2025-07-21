 package com.example.SwapTicket.model;

public class TrainResult {
    private String trainNo;
    private String trainName;
    private String sourceStation;
    private String destinationStation;
    private String departureTime; // from source
    private String arrivalTime;   // at destination

    public TrainResult(String trainNo, String trainName, String sourceStation, String destinationStation, String departureTime, String arrivalTime) {
        this.trainNo = trainNo;
        this.trainName = trainName;
        this.sourceStation = sourceStation;
        this.destinationStation = destinationStation;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
    }

    // Getters
    public String getTrainNo() { return trainNo; }
    public String getTrainName() { return trainName; }
    public String getSourceStation() { return sourceStation; }
    public String getDestinationStation() { return destinationStation; }
    public String getDepartureTime() { return departureTime; }
    public String getArrivalTime() { return arrivalTime; }
}