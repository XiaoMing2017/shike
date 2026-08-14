package com.shike.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactConfigDTO {
    private Boolean enabled;
    private String title;
    private String wxId;
    private String phone;
    private String notice;
    private String openType; // MODAL 或 CONTACT
}
