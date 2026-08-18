package com.shike.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetCreateDTO {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "宠物名称不能为空")
    private String name;

    @NotBlank(message = "宠物类型不能为空")
    private String petType;

    private String avatarUrl;
    private String prompt;
}
