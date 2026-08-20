package com.shike.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_pet", indexes = {
        @Index(name = "idx_user_pet", columnList = "user_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "pet_type", nullable = false, length = 30)
    private String petType; // DRAGON, TOTORO, CAT, DOG, QILIN, CUSTOM

    @Column(name = "avatar_url", nullable = false, length = 512)
    private String avatarUrl;

    @Column(name = "prompt", length = 512)
    private String prompt;

    @Builder.Default
    private Integer level = 1;

    @Builder.Default
    private Integer exp = 0;

    @Builder.Default
    private Integer fullness = 60; // 0 - 100

    @Builder.Default
    private Integer intimacy = 10;

    @Builder.Default
    @Column(name = "food_count")
    private Integer foodCount = 1; // 初始赠送 1 份食物

    @Builder.Default
    @Column(name = "mood", length = 30)
    private String mood = "NORMAL"; // HAPPY, NORMAL, HUNGRY, WANT_EXERCISE

    @Builder.Default
    @Column(name = "streak_days")
    private Integer streakDays = 1;

    @Column(name = "last_feed_date")
    private LocalDate lastFeedDate;

    @Column(name = "last_checkin_date")
    private LocalDate lastCheckinDate;

    @Column(name = "last_exercise_date")
    private LocalDate lastExerciseDate;

    @Column(name = "last_diet_date")
    private LocalDate lastDietDate;

    @Column(name = "last_water_date")
    private LocalDate lastWaterDate;

    @Column(name = "last_weight_date")
    private LocalDate lastWeightDate;

    @Column(name = "last_fullness_calc_time")
    private LocalDateTime lastFullnessCalcTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
