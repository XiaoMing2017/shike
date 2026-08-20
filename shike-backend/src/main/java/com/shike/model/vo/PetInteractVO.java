package com.shike.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetInteractVO {
    private String dialogue;
    private String mood;
    private String actionAnim;
    private String soundEffect;
    private Integer foodCount;
    private Boolean isHealed;
}
