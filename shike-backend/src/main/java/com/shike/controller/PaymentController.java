package com.shike.controller;

import com.shike.common.ResultDTO;
import com.shike.model.dto.CheckoutResponseDTO;
import com.shike.model.dto.CreateCheckoutDTO;
import com.shike.model.dto.OrderStatusDTO;
import com.shike.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create Lemon Squeezy checkout session for user
     */
    @PostMapping("/checkout")
    public ResultDTO<CheckoutResponseDTO> createCheckout(
            @RequestBody @Valid CreateCheckoutDTO dto,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {

        Long userId = dto.getUserId();
        if (userId == null && headerUserId != null && !headerUserId.isBlank()) {
            try {
                userId = Long.parseLong(headerUserId);
            } catch (NumberFormatException ignored) {}
        }

        CheckoutResponseDTO response = paymentService.createCheckoutSession(userId, dto.getPlanType());
        return ResultDTO.success(response);
    }

    /**
     * Lemon Squeezy Webhook Listener
     * Note: Lemon Squeezy requires 200 OK response upon receiving webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Signature", required = false) String signatureHeader) {

        log.info("[PAYMENT_WEBHOOK] Incoming webhook request with X-Signature: {}", signatureHeader);
        boolean success = paymentService.handleWebhook(rawPayload, signatureHeader);

        if (!success) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Signature verification failed");
        }
        return ResponseEntity.ok("Webhook processed successfully");
    }

    /**
     * Query order status and VIP fulfillment
     */
    @GetMapping("/order-status/{orderNo}")
    public ResultDTO<OrderStatusDTO> getOrderStatus(@PathVariable String orderNo) {
        OrderStatusDTO status = paymentService.getOrderStatus(orderNo);
        return ResultDTO.success(status);
    }

    /**
     * Dev / Test Sandbox bypass: simulates instant test payment completion for QA
     */
    @PostMapping("/test-complete/{orderNo}")
    public ResultDTO<OrderStatusDTO> testCompleteOrder(@PathVariable String orderNo) {
        OrderStatusDTO status = paymentService.testCompleteOrder(orderNo);
        return ResultDTO.success(status);
    }
}
