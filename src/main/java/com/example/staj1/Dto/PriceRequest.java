package com.example.staj1.Dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class PriceRequest {
    @NotNull(message = "Alt sınır boş olamaz.")
    @DecimalMin(value = "0", message = "Alt sınır negatif olamaz.")
    @Digits(integer = 10, fraction = 2, message = "Alt sınır en fazla iki ondalık basamak içermelidir.")
    private BigDecimal minDesi;
    @NotNull(message = "Üst sınır boş olamaz.")
    @Positive(message = "Üst sınır pozitif olmalıdır.")
    @Digits(integer = 10, fraction = 2, message = "Üst sınır en fazla iki ondalık basamak içermelidir.")
    private BigDecimal maxDesi;
    @NotNull(message = "Ücret boş olamaz.")
    @Positive(message = "Ücret pozitif olmalıdır.")
    @Digits(integer = 10, fraction = 2, message = "Ücret en fazla iki ondalık basamak içermelidir.")
    private BigDecimal price;

    public BigDecimal getMinDesi() { return minDesi; }
    public void setMinDesi(BigDecimal minDesi) { this.minDesi = minDesi; }
    public BigDecimal getMaxDesi() { return maxDesi; }
    public void setMaxDesi(BigDecimal maxDesi) { this.maxDesi = maxDesi; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
