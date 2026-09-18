package com.shike.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_payment_order")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 64)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_email", length = 120)
    private String userEmail;

    @Column(name = "plan_type", nullable = false, length = 20)
    private String planType; // WEEKLY, YEARLY

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal amount;

    @Column(length = 10)
    private String currency; // USD

    @Column(length = 20)
    private String status; // PENDING, PAID, CANCELLED, REFUNDED

    @Column(length = 20)
    private String provider; // LEMON_SQUEEZY

    @Column(name = "ls_order_id", length = 64)
    private String lsOrderId;

    @Column(name = "ls_subscription_id", length = 64)
    private String lsSubscriptionId;

    @Column(name = "checkout_url", columnDefinition = "TEXT")
    private String checkoutUrl;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
