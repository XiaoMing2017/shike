package com.shike.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusDTO {
    private String orderNo;
    private String status;
    private String planType;
    private String vipType;
    private LocalDateTime vipExpireTime;
    private Boolean aiUnlimited;
    private Boolean isVipActive;
}
