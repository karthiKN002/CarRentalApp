package com.example.gearup.models;

import com.example.gearup.states.contract.ContractState;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class Contract {
    private String id;
    private String userId;
    private String managerId; // Added
    private String carId;
    private String carName;
    private Timestamp createdAt;
    private Timestamp startDate;
    private Timestamp endDate;
    private Timestamp updatedAt;
    private double totalPayment;
    private ContractState state;
    private String eventId;
    private boolean rated;
    private String pickupLocation;

    public Contract() {
        this.state = ContractState.ACTIVE;
        this.rated = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    @PropertyName("managerId")
    public String getManagerId() { return managerId; }
    @PropertyName("managerId")
    public void setManagerId(String managerId) { this.managerId = managerId; }
    @PropertyName("carId")
    public String getCarId() { return carId; }
    @PropertyName("carId")
    public void setCarId(String carId) { this.carId = carId; }
    @PropertyName("carName")
    public String getCarName() { return carName; }
    @PropertyName("carName")
    public void setCarName(String carName) { this.carName = carName; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getStartDate() { return startDate; }
    public void setStartDate(Timestamp startDate) { this.startDate = startDate; }
    public Timestamp getEndDate() { return endDate; }
    public void setEndDate(Timestamp endDate) { this.endDate = endDate; }
    @PropertyName("updateDate")
    public Timestamp getUpdatedAt() { return updatedAt; }
    @PropertyName("updateDate")
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public double getTotalPayment() { return totalPayment; }
    public void setTotalPayment(double totalPayment) { this.totalPayment = totalPayment; }
    @PropertyName("status")
    public ContractState getState() { return state; }
    @PropertyName("status")
    public void setState(ContractState state) { this.state = state; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public boolean isRated() { return rated; }
    public void setRated(boolean rated) { this.rated = rated; }
    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }
}