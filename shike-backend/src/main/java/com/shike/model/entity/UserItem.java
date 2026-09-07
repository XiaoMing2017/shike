package com.shike.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_user_item", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "item_type"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "item_type", nullable = false, length = 50)
    private String itemType; // CHEAT_SHIELD, SERUM_REVIVAL, SNIPER_AUDIT, MIRROR_DEFLECT

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
