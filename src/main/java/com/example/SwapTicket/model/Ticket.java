package com.example.SwapTicket.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;

import java.time.LocalDate;
import java.util.List;

@Entity
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String trainNumber;
    private String trainName;
    private String fromStation;
    private String toStation;
    @FutureOrPresent(message = "Date of journey must be today or in the future")
    private LocalDate dateOfJourney;
    private String classType;
    private String pnrNumber;

    @ElementCollection
    private List<String> passengerNames;

    private double price;
    private String ticketImagePath;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE") 
    private boolean sold = false;

    private Integer numberOfTickets;
    private Integer age;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    private String sellerEmail;
    
    private String gender;
    // --- Getters and Setters ---
    
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean sellerPaid = false;

    // … getters / setters …

    public boolean isSellerPaid() {
        return sellerPaid;
    }

    public void setSellerPaid(boolean sellerPaid) {
        this.sellerPaid = sellerPaid;
    }

    
    public Long getId() {
        return id;
    }

    public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getSellerEmail() {
		return sellerEmail;
	}

	public void setSellerEmail(String sellerEmail) {
		this.sellerEmail = sellerEmail;
	}

	public void setId(Long id) {
        this.id = id;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public void setTrainNumber(String trainNumber) {
        this.trainNumber = trainNumber;
    }

    public String getTrainName() {
        return trainName;
    }

    public void setTrainName(String trainName) {
        this.trainName = trainName;
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

    public LocalDate getDateOfJourney() {
        return dateOfJourney;
    }

    public void setDateOfJourney(LocalDate dateOfJourney) {
        this.dateOfJourney = dateOfJourney;
    }

    public String getClassType() {
        return classType;
    }

    public void setClassType(String classType) {
        this.classType = classType;
    }

    public String getPnrNumber() {
        return pnrNumber;
    }

    public void setPnrNumber(String pnrNumber) {
        this.pnrNumber = pnrNumber;
    }

    public List<String> getPassengerNames() {
        return passengerNames;
    }

    public void setPassengerNames(List<String> passengerNames) {
        this.passengerNames = passengerNames;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getTicketImagePath() {
        return ticketImagePath;
    }

    public void setTicketImagePath(String ticketImagePath) {
        this.ticketImagePath = ticketImagePath;
    }

    public boolean isSold() {
        return sold;
    }

    public void setSold(boolean sold) {
        this.sold = sold;
    }

    public Integer getNumberOfTickets() {
        return numberOfTickets;
    }

    public void setNumberOfTickets(Integer numberOfTickets) {
        this.numberOfTickets = numberOfTickets;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
    @ManyToOne
    @JoinColumn(name = "buyer_id")
    private User buyer;
    
    public User getBuyer() {
        return buyer;
    }

    public void setBuyer(User buyer) {
        this.buyer = buyer;
    }
    
    private String originalDocumentPath;

    public String getOriginalDocumentPath() {
        return originalDocumentPath;
    }

    public void setOriginalDocumentPath(String originalDocumentPath) {
        this.originalDocumentPath = originalDocumentPath;
    }
    
    private String seatNumber;

 // Getter and Setter
    public String getSeatNumber() {
     return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
     this.seatNumber = seatNumber;
    }
    private String buyerEmail;

 // + getter and setter
    public String getBuyerEmail() {
     return buyerEmail;
    }

    public void setBuyerEmail(String buyerEmail) {
     this.buyerEmail = buyerEmail;
    }



	@Override
	public String toString() {
		return "Ticket [id=" + id + ", trainNumber=" + trainNumber + ", trainName=" + trainName + ", fromStation="
				+ fromStation + ", toStation=" + toStation + ", dateOfJourney=" + dateOfJourney + ", classType="
				+ classType + ", pnrNumber=" + pnrNumber + ", passengerNames=" + passengerNames + ", price=" + price
				+ ", ticketImagePath=" + ticketImagePath + ", sold=" + sold + ", numberOfTickets=" + numberOfTickets
				+ ", age=" + age + ", user=" + user + ", sellerEmail=" + sellerEmail + ", buyer=" + buyer + "]";
	}

}
