package com.example.SwapTicket.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String trainNumber;
    private String trainName;
    private String fromStation;
    private String toStation;

    private LocalDate dateOfJourney; // You can also use LocalDate if needed
    private String classType;
    private String pnrNumber;

    @ElementCollection
    private List<String> passengerNames; // multiple passengers

    private double price;

    private String ticketImagePath; // path to the uploaded ticket image

    private boolean isSold = false;
    
    private Integer numberOfTickets;
    private Integer age;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

	public Long getId() {
		return id;
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
		return isSold;
	}

	public void setSold(boolean isSold) {
		this.isSold = isSold;
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

	@Override
	public String toString() {
		return "Ticket [id=" + id + ", trainNumber=" + trainNumber + ", trainName=" + trainName + ", fromStation="
				+ fromStation + ", toStation=" + toStation + ", dateOfJourney=" + dateOfJourney + ", classType="
				+ classType + ", pnrNumber=" + pnrNumber + ", passengerNames=" + passengerNames + ", price=" + price
				+ ", ticketImagePath=" + ticketImagePath + ", isSold=" + isSold + ", numberOfTickets=" + numberOfTickets
				+ ", age=" + age + ", user=" + user + "]";
	}
    
    
}