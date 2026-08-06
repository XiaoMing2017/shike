package com.shike.repository;

import com.shike.model.entity.AnnouncementConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnnouncementConfigRepository extends JpaRepository<AnnouncementConfig, Long> {
    Optional<AnnouncementConfig> findFirstByOrderByIdAsc();
}
