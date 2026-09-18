package com.shike.repository;

import com.shike.model.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByOrderNo(String orderNo);
    Optional<PaymentOrder> findByLsOrderId(String lsOrderId);
    Optional<PaymentOrder> findByLsSubscriptionId(String lsSubscriptionId);
    List<PaymentOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
}
