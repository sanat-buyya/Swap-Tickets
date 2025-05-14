package com.example.SwapTicket.model;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.FutureOrPresent;

@Entity
public class PNR {

    @Id
    private String pnrNumber;

    private String trainNumber;
    private String fromStation;
    private String toStation;

    @FutureOrPresent(message = "Date of journey must be today or in the future")
    private LocalDate journeyDate;

    private String ticketImagePath;
    private Integer numberOfTickets;
    private String sellerEmail;
    private String upiId;
    private String sellerMobile;

    @OneToMany(mappedBy = "pnr", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Passenger> passenger; 
    
    public void updateSoldStatus() {
        // Update the sold status of the PNR based on whether all passengers are sold
        this.isSold = passenger.stream().allMatch(Passenger::isSold);
        }

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE", name = "is_sold")
    private boolean isSold;
    
    

    // Getters and setters
    public String getPnrNumber() {
        return pnrNumber;
    }

    public void setPnrNumber(String pnrNumber) {
        this.pnrNumber = pnrNumber;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public void setTrainNumber(String trainNumber) {
        this.trainNumber = trainNumber;
    }

    public String getFromStation() {
        return fromStation;
    }

    public void setFromStation(String fromStation) {
        this.fromStation = fromStation;
    }

    public String getToStation() {
        return toStation;
    }

    public void setToStation(String toStation) {
        this.toStation = toStation;
    }

    public LocalDate getJourneyDate() {
        return journeyDate;
    }

    public void setJourneyDate(LocalDate journeyDate) {
        this.journeyDate = journeyDate;
    }

    public String getTicketImagePath() {
        return ticketImagePath;
    }

    public void setTicketImagePath(String ticketImagePath) {
        this.ticketImagePath = ticketImagePath;
    }

    public Integer getNumberOfTickets() {
        return numberOfTickets;
    }

    public void setNumberOfTickets(Integer numberOfTickets) {
        this.numberOfTickets = numberOfTickets;
    }

    public String getSellerEmail() {
        return sellerEmail;
    }

    public void setSellerEmail(String sellerEmail) {
        this.sellerEmail = sellerEmail;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public String getSellerMobile() {
        return sellerMobile;
    }

    public void setSellerMobile(String sellerMobile) {
        this.sellerMobile = sellerMobile;
    }

    public List<Passenger> getPassenger() {
        return passenger;
    }

    public void setPassenger(List<Passenger> passenger) {
        this.passenger = passenger;
    }

    public boolean isSold() {
        return isSold;
    }

    public void setSold(boolean sold) {
        this.isSold = sold;
    }
}
