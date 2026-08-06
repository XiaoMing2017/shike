package com.shike.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementConfigDTO {

    private Boolean enabled;
    private String badgeText;
    private String title;
    private String subtitle;
    private List<Item> items;
    private String buttonText;
    private String buttonAction; // e.g. "AI_PLAN", "HEALTH_DIAGNOSIS", "PHOTO_MEAL", "NONE"

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private String icon;
        private String title;
        private String desc;
    }
}
