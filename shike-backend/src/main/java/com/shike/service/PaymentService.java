package com.shike.service;

import com.shike.model.dto.CheckoutResponseDTO;
import com.shike.model.dto.OrderStatusDTO;

public interface PaymentService {
    CheckoutResponseDTO createCheckoutSession(Long userId, String planType);
    boolean handleWebhook(String rawPayload, String signatureHeader);
    OrderStatusDTO getOrderStatus(String orderNo);
    OrderStatusDTO testCompleteOrder(String orderNo);
    OrderStatusDTO testRefundOrder(String orderNo);
}
