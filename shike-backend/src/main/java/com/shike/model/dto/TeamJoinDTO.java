package com.shike.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeamJoinDTO {

    @NotNull(message = "userId cannot be null")
    private Long userId;

    @NotBlank(message = "inviteCode cannot be blank")
    private String inviteCode;
}
