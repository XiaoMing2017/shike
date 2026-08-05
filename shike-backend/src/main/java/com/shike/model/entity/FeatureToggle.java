package com.shike.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_feature_toggle")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureToggle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feature_key", nullable = false, unique = true, length = 50)
    private String featureKey;

    @Column(name = "feature_name", nullable = false, length = 100)
    private String featureName;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "env_mode", length = 30)
    private String envMode; // PROD_AND_TEST, TEST_ONLY, DISABLED

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
