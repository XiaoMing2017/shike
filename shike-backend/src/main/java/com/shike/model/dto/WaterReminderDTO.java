package com.shike.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterReminderDTO {
    private Boolean needReminder;
    private String reminderMsg;
    private Integer currentAmount;
    private Integer targetAmount;
    private Integer remainingAmount;
    private Integer timeSlot;
}
