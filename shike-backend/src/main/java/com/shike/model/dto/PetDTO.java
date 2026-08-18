package com.shike.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetDTO {
    private Long id;
    private Long userId;
    private String name;
    private String petType;
    private String avatarUrl;
    private String prompt;
    private Integer level;
    private Integer exp;
    private Integer maxExp;
    private Integer fullness;
    private Integer intimacy;
    private Integer foodCount;
    private String mood;
    private String moodText;
    private String dialogue;
    private Integer streakDays;
    private Boolean fedToday;
    private Boolean exercisedToday;
    private LocalDate lastFeedDate;
    private LocalDate lastExerciseDate;
}
