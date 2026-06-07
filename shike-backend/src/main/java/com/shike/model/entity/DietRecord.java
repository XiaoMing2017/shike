package com.shike.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_diet_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DietRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "meal_type", nullable = false, length = 10)
    private String mealType; // BREAKFAST, LUNCH, DINNER, SNACK

    @Column(name = "image_url", length = 256)
    private String imageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "food_items", nullable = false)
    private String foodItems; // [{"name":"苹果", "weight":150}]

    @Column(name = "total_calories", nullable = false, precision = 6, scale = 1)
    private BigDecimal totalCalories;

    @Column(name = "total_protein", precision = 5, scale = 1)
    private BigDecimal totalProtein;

    @Column(name = "total_fat", precision = 5, scale = 1)
    private BigDecimal totalFat;

    @Column(name = "total_carbs", precision = 5, scale = 1)
    private BigDecimal totalCarbs;

    @Column(name = "oil_level", length = 10)
    private String oilLevel; // LIGHT, MODERATE, HEAVY

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
