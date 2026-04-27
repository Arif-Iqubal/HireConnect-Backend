package com.hireconnect.subscription.dto.request;

import com.hireconnect.subscription.enums.PaymentMode;
import com.hireconnect.subscription.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscribeRequest {

    @NotNull(message = "Plan is required")
    private SubscriptionPlan plan;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    private Boolean autoRenew = false;

    // Payment gateway token (would be validated by payment gateway in production)
    private String paymentToken;
}
