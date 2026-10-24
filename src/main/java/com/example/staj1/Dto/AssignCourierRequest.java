package com.example.staj1.Dto;

import com.example.staj1.config.StrictIntegerDeserializer;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import tools.jackson.databind.annotation.JsonDeserialize;

public class AssignCourierRequest {
    @NotNull(message = "Kurye seçilmelidir.")
    @Positive(message = "Kurye numarası pozitif olmalıdır.")
    @JsonDeserialize(using = StrictIntegerDeserializer.class)
    private Integer courierId;

    public Integer getCourierId() { return courierId; }
    public void setCourierId(Integer courierId) { this.courierId = courierId; }
}
