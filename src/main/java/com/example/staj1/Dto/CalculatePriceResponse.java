package com.example.staj1.Dto;

import java.math.BigDecimal;

public class CalculatePriceResponse {

    private BigDecimal width;
    private BigDecimal length;
    private BigDecimal height;
    private BigDecimal weight;
    private BigDecimal desi;
    private BigDecimal calculatedValue;
    private String calculatedBy;
    private BigDecimal price;

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

    public BigDecimal getDesi() {
        return desi;
    }

    public void setDesi(BigDecimal desi) {
        this.desi = desi;
    }

    public BigDecimal getCalculatedValue() {
        return calculatedValue;
    }

    public void setCalculatedValue(BigDecimal calculatedValue) {
        this.calculatedValue = calculatedValue;
    }

    public String getCalculatedBy() {
        return calculatedBy;
    }

    public void setCalculatedBy(String calculatedBy) {
        this.calculatedBy = calculatedBy;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

}
