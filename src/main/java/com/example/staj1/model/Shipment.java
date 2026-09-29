package com.example.staj1.model;

import jakarta.persistence.*;

import java.math.BigDecimal;


@Entity
@Table(name = "shipments")
public class Shipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private BigDecimal width;
    private BigDecimal length;
    private BigDecimal height;
    private BigDecimal weight;
    private BigDecimal price;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;

    private Integer senderAddressId;
    private Integer receiverAddressId;

    private String senderName;
    private String senderSurname;
    private String senderCity;
    private String senderDistrict;
    private String senderAddressText;


    private String receiverName;
    private String receiverSurname;
    private String receiverCity;
    private String receiverDistrict;
    private String receiverAddressText;

    @Column(unique = true, nullable = false, length = 12)
    private String barcode;

    private boolean deleted = false;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public BigDecimal getWidth() {
        return width;
    }

    public void setWidth(BigDecimal width) {
        this.width = width;
    }

    public BigDecimal getLength() {
        return length;
    }

    public void setLength(BigDecimal length) {
        this.length = length;
    }

    public BigDecimal getHeight() {
        return height;
    }

    public void setHeight(BigDecimal height) {
        this.height = height;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public Integer getSenderAddressId() {
        return senderAddressId;
    }

    public void setSenderAddressId(Integer senderAddressId) {
        this.senderAddressId = senderAddressId;
    }

    public Integer getReceiverAddressId() {
        return receiverAddressId;
    }

    public void setReceiverAddressId(Integer receiverAddressId) {
        this.receiverAddressId = receiverAddressId;
    }

    public String getSenderCity() {
        return senderCity;
    }

    public void setSenderCity(String senderCity) {
        this.senderCity = senderCity;
    }

    public String getSenderDistrict() {
        return senderDistrict;
    }

    public void setSenderDistrict(String senderDistrict) {
        this.senderDistrict = senderDistrict;
    }


    public String getReceiverCity() {
        return receiverCity;
    }

    public void setReceiverCity(String receiverCity) {
        this.receiverCity = receiverCity;
    }

    public String getReceiverDistrict() {
        return receiverDistrict;
    }

    public void setReceiverDistrict(String receiverDistrict) {
        this.receiverDistrict = receiverDistrict;
    }

    public String getReceiverAddressText() {
        return receiverAddressText;
    }

    public void setReceiverAddressText(String receiverAddressText) {
        this.receiverAddressText = receiverAddressText;
    }

    public String getSenderAddressText() {
        return senderAddressText;
    }

    public void setSenderAddressText(String senderAddressText) {
        this.senderAddressText = senderAddressText;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderSurname() {
        return senderSurname;
    }

    public void setSenderSurname(String senderSurname) {
        this.senderSurname = senderSurname;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverSurname() {
        return receiverSurname;
    }

    public void setReceiverSurname(String receiverSurname) {
        this.receiverSurname = receiverSurname;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
