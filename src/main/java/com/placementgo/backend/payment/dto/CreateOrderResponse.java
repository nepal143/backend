package com.placementgo.backend.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateOrderResponse {
    private String orderId;
    private Integer amountPaise;
    private String currency;
    private String keyId;
    private String planName;
    private String description;
}
