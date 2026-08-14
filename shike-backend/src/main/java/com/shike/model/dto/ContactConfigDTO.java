package com.shike.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private List<ContactItem> items; // 动态联系方式列表

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactItem {
        private String id;
        private String label;      // 例如: 客服微信号
        private String value;      // 例如: shike_helper
        private String actionType; // COPY (复制), CALL (拨打), NONE (无动作)
        private String btnText;    // 例如: 复制微信号
    }
}
