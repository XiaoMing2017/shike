package com.shike.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String openid;

    @Column(length = 50)
    private String nickname;

    @Column(name = "avatar_url", length = 256)
    private String avatarUrl;

    private Integer gender; // 0-unknown, 1-male, 2-female

    private Integer age;

    @Column(precision = 5, scale = 2)
    private BigDecimal height;

    @Column(precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "activity_level", length = 20)
    private String activityLevel; // SEDENTARY, LIGHT, MODERATE, ACTIVE

    @Column(length = 20)
    private String goal; // LOSE_WEIGHT, MAINTAIN, GAIN_MUSCLE

    @Column(precision = 6, scale = 1)
    private BigDecimal bmr;

    @Column(precision = 6, scale = 1)
    private BigDecimal tdee;

    @Column(name = "target_calories", precision = 6, scale = 1)
    private BigDecimal targetCalories;

    private Integer points;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
