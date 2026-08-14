package com.shike.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_contact_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(length = 50)
    private String title;

    @Column(name = "wx_id", length = 64)
    private String wxId;

    @Column(length = 32)
    private String phone;

    @Column(length = 256)
    private String notice;

    @Column(name = "open_type", length = 20)
    private String openType; // MODAL (联系弹窗) 或 CONTACT (微信原生客服)

    @Column(name = "items_json", columnDefinition = "TEXT")
    private String itemsJson; // 动态联系方式列表 JSON 字符串

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
