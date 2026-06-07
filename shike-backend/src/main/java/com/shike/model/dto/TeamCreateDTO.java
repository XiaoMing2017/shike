package com.shike.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeamCreateDTO {

    @NotNull(message = "creatorId cannot be null")
    private Long creatorId;

    @NotBlank(message = "teamName cannot be blank")
    private String teamName;

    private Integer targetDays; // default 7
}
