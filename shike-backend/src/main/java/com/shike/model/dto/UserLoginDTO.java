package com.shike.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserLoginDTO {
    @NotBlank(message = "openid cannot be blank")
    private String openid;
}
