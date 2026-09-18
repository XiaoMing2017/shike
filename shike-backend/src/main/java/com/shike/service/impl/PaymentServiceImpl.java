package com.shike.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shike.common.BizException;
import com.shike.model.dto.CheckoutResponseDTO;
import com.shike.model.dto.OrderStatusDTO;
import com.shike.model.entity.PaymentOrder;
import com.shike.model.entity.User;
import com.shike.repository.PaymentOrderRepository;
import com.shike.repository.UserRepository;
import com.shike.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${lemonsqueezy.api-key:}")
    private String apiKey;

    @Value("${lemonsqueezy.store-id:}")
    private String storeId;

    @Value("${lemonsqueezy.webhook-secret:}")
    private String webhookSecret;

    @Value("${lemonsqueezy.variant-weekly-id:}")
    private String variantWeeklyId;

    @Value("${lemonsqueezy.variant-yearly-id:}")
    private String variantYearlyId;

    @Value("${lemonsqueezy.redirect-url:https://shike-two.vercel.app/profile?payment=success}")
    private String redirectUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @Transactional
    public CheckoutResponseDTO createCheckoutSession(Long userId, String planType) {
        if (userId == null) {
            throw new BizException("User ID is required to create a checkout session");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException("User not found with ID: " + userId));

        String normPlan = (planType != null && planType.equalsIgnoreCase("yearly")) ? "YEARLY" : "WEEKLY";
        BigDecimal amount = normPlan.equals("YEARLY") ? new BigDecimal("39.99") : new BigDecimal("4.99");
        String currency = "USD";

        String orderNo = "LS_ORD_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        String checkoutUrl = null;

        // If Lemon Squeezy API key and store ID are configured, request Lemon Squeezy API
        if (apiKey != null && !apiKey.isBlank() && storeId != null && !storeId.isBlank()) {
            try {
                String variantId = normPlan.equals("YEARLY") ? variantYearlyId : variantWeeklyId;
                if (variantId != null && !variantId.isBlank()) {
                    checkoutUrl = requestLemonSqueezyCheckout(user, orderNo, normPlan, variantId);
                }
            } catch (Exception e) {
                log.error("[LEMON_SQUEEZY] Error creating remote checkout via API: {}", e.getMessage(), e);
            }
        }

        // Sandbox / Test fallback checkout URL if API not configured or during local QA
        if (checkoutUrl == null || checkoutUrl.isBlank()) {
            checkoutUrl = "https://shike-two.vercel.app/profile?simulated_checkout=1&order_no=" + orderNo + "&plan=" + normPlan.toLowerCase();
        }

        PaymentOrder order = PaymentOrder.builder()
                .orderNo(orderNo)
                .userId(user.getId())
                .userEmail(user.getEmail())
                .planType(normPlan)
                .amount(amount)
                .currency(currency)
                .status("PENDING")
                .provider("LEMON_SQUEEZY")
                .checkoutUrl(checkoutUrl)
                .build();

        paymentOrderRepository.save(order);
        log.info("[PAYMENT] Created order: {} for user: {} (plan: {}, amount: {})", orderNo, user.getId(), normPlan, amount);

        return CheckoutResponseDTO.builder()
                .orderNo(orderNo)
                .checkoutUrl(checkoutUrl)
                .planType(normPlan)
                .amount(amount)
                .currency(currency)
                .build();
    }

    private String requestLemonSqueezyCheckout(User user, String orderNo, String planType, String variantId) throws Exception {
        String url = "https://api.lemonsqueezy.com/v1/checkouts";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.api+json");
        headers.set("Content-Type", "application/vnd.api+json");
        headers.set("Authorization", "Bearer " + apiKey.trim());

        Map<String, Object> customData = new HashMap<>();
        customData.put("user_id", String.valueOf(user.getId()));
        customData.put("order_no", orderNo);
        customData.put("plan_type", planType);

        Map<String, Object> checkoutData = new HashMap<>();
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            checkoutData.put("email", user.getEmail().trim());
        }
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            checkoutData.put("name", user.getNickname());
        }
        checkoutData.put("custom", customData);

        Map<String, Object> productOptions = new HashMap<>();
        productOptions.put("redirect_url", redirectUrl);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("checkout_data", checkoutData);
        attributes.put("product_options", productOptions);

        Map<String, Object> storeData = new HashMap<>();
        storeData.put("type", "stores");
        storeData.put("id", storeId.trim());

        Map<String, Object> variantData = new HashMap<>();
        variantData.put("type", "variants");
        variantData.put("id", variantId.trim());

        Map<String, Object> relationships = new HashMap<>();
        relationships.put("store", Collections.singletonMap("data", storeData));
        relationships.put("variant", Collections.singletonMap("data", variantData));

        Map<String, Object> data = new HashMap<>();
        data.put("type", "checkouts");
        data.put("attributes", attributes);
        data.put("relationships", relationships);

        Map<String, Object> payload = Collections.singletonMap("data", data);

        String jsonPayload = objectMapper.writeValueAsString(payload);
        HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode urlNode = root.path("data").path("attributes").path("url");
            if (!urlNode.isMissingNode() && !urlNode.isNull()) {
                return urlNode.asText();
            }
        }
        return null;
    }

    @Override
    public boolean handleWebhook(String rawPayload, String signatureHeader) {
        if (!verifySignature(rawPayload, signatureHeader)) {
            log.warn("[LEMON_SQUEEZY_WEBHOOK] Signature verification failed!");
            return false;
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String eventName = root.path("meta").path("event_name").asText();
            log.info("[LEMON_SQUEEZY_WEBHOOK] Received event: {}", eventName);

            if ("order_created".equalsIgnoreCase(eventName) || "subscription_created".equalsIgnoreCase(eventName)) {
                processOrderPaidEvent(root);
            } else {
                log.info("[LEMON_SQUEEZY_WEBHOOK] Ignored unhandled event: {}", eventName);
            }
            return true;
        } catch (Exception e) {
            log.error("[LEMON_SQUEEZY_WEBHOOK] Failed to process webhook payload: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean verifySignature(String rawPayload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("[LEMON_SQUEEZY] Webhook secret not configured. Skipping HMAC check in dev mode.");
            return true;
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            log.error("[LEMON_SQUEEZY] Missing X-Signature header.");
            return false;
        }
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(webhookSecret.trim().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(keySpec);
            byte[] hash = hmac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(hash);
            return expected.equalsIgnoreCase(signatureHeader.trim());
        } catch (Exception e) {
            log.error("[LEMON_SQUEEZY] HMAC computation error: {}", e.getMessage());
            return false;
        }
    }

    @Transactional
    public void processOrderPaidEvent(JsonNode root) {
        JsonNode customNode = root.path("meta").path("custom_data");
        String orderNo = customNode.path("order_no").asText(null);
        String userIdStr = customNode.path("user_id").asText(null);
        String planType = customNode.path("plan_type").asText("YEARLY");

        String lsOrderId = root.path("data").path("id").asText(null);
        String lsSubscriptionId = root.path("data").path("attributes").path("subscription_id").asText(null);

        PaymentOrder order = null;
        if (orderNo != null && !orderNo.isBlank()) {
            order = paymentOrderRepository.findByOrderNo(orderNo).orElse(null);
        }

        Long userId = null;
        if (userIdStr != null && !userIdStr.isBlank()) {
            try {
                userId = Long.parseLong(userIdStr);
            } catch (Exception ignored) {}
        }

        if (order == null && userId != null) {
            // Find recent pending order for this user
            List<PaymentOrder> orders = paymentOrderRepository.findByUserIdOrderByCreatedAtDesc(userId);
            if (!orders.isEmpty() && "PENDING".equals(orders.get(0).getStatus())) {
                order = orders.get(0);
            }
        }

        if (order != null) {
            fulfillOrder(order, lsOrderId, lsSubscriptionId, planType);
        } else if (userId != null) {
            // Direct user VIP upgrade even if order record missing
            fulfillUserVip(userId, planType);
        } else {
            log.warn("[LEMON_SQUEEZY_WEBHOOK] Could not match orderNo or userId from webhook payload");
        }
    }

    @Override
    @Transactional
    public OrderStatusDTO testCompleteOrder(String orderNo) {
        PaymentOrder order = paymentOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BizException("Order not found: " + orderNo));

        if (!"PAID".equals(order.getStatus())) {
            fulfillOrder(order, "TEST_LS_" + System.currentTimeMillis(), null, order.getPlanType());
        }

        return getOrderStatus(orderNo);
    }

    private void fulfillOrder(PaymentOrder order, String lsOrderId, String lsSubscriptionId, String planType) {
        order.setStatus("PAID");
        order.setLsOrderId(lsOrderId);
        order.setLsSubscriptionId(lsSubscriptionId);
        order.setPaidAt(LocalDateTime.now());
        paymentOrderRepository.save(order);

        fulfillUserVip(order.getUserId(), planType != null ? planType : order.getPlanType());
        log.info("[PAYMENT] Successfully fulfilled order: {} for user: {}", order.getOrderNo(), order.getUserId());
    }

    private void fulfillUserVip(Long userId, String planType) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("[PAYMENT] User {} not found for VIP fulfillment", userId);
            return;
        }

        user.setVipType("PRO");
        user.setAiUnlimited(true);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = (user.getVipExpireTime() != null && user.getVipExpireTime().isAfter(now))
                ? user.getVipExpireTime()
                : now;

        if ("WEEKLY".equalsIgnoreCase(planType)) {
            user.setVipExpireTime(base.plusWeeks(1));
        } else {
            user.setVipExpireTime(base.plusYears(1));
        }

        userRepository.save(user);
        log.info("[PAYMENT] User {} upgraded to PRO! Expire at: {}", user.getId(), user.getVipExpireTime());
    }

    @Override
    public OrderStatusDTO getOrderStatus(String orderNo) {
        PaymentOrder order = paymentOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new BizException("Order not found: " + orderNo));

        User user = userRepository.findById(order.getUserId()).orElse(null);

        boolean isVipActive = false;
        String vipType = "NORMAL";
        LocalDateTime expireTime = null;
        Boolean aiUnlimited = false;

        if (user != null) {
            vipType = user.getVipType();
            expireTime = user.getVipExpireTime();
            aiUnlimited = user.getAiUnlimited();
            if (Boolean.TRUE.equals(aiUnlimited)) {
                isVipActive = true;
            } else if ("PRO".equalsIgnoreCase(vipType) || "VIP".equalsIgnoreCase(vipType)) {
                isVipActive = expireTime == null || expireTime.isAfter(LocalDateTime.now());
            }
        }

        return OrderStatusDTO.builder()
                .orderNo(order.getOrderNo())
                .status(order.getStatus())
                .planType(order.getPlanType())
                .vipType(vipType)
                .vipExpireTime(expireTime)
                .aiUnlimited(aiUnlimited)
                .isVipActive(isVipActive)
                .build();
    }
}
