package com.shike.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCheckoutDTO {
    @NotBlank(message = "planType is required (weekly or yearly)")
    private String planType; // weekly or yearly
    private Long userId;
}
