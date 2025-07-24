package com.example.gearup.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

public class Message {
    private String id;
    private String senderId;
    private String receiverId;
    private String message;
    private Timestamp timestamp;
    private String carId;
    private String contractId;
    private String messageType;
    private boolean isRead;
    private boolean confirmed;
    private String pickupLocation;

    public Message() {}

    public Message(String senderId, String receiverId, String message, Timestamp timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = false;
        this.messageType = "REGULAR";
        this.confirmed = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Timestamp getTimestamp() { return timestamp; }

    @PropertyName("timestamp")
    public void setTimestamp(Object timestamp) {
        if (timestamp instanceof Long) {
            this.timestamp = new Timestamp(((Long) timestamp) / 1000, 0);
        } else if (timestamp instanceof Timestamp) {
            this.timestamp = (Timestamp) timestamp;
        }
    }

    public String getCarId() { return carId; }
    public void setCarId(String carId) { this.carId = carId; }

    public String getContractId() { return contractId; }
    public void setContractId(String contractId) { this.contractId = contractId; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean isRead) { this.isRead = isRead; }

    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }

    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }
}