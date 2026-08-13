package com.shike.repository;

import com.shike.model.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long>, JpaSpecificationExecutor<Feedback> {

    List<Feedback> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByStatus(String status);

    long countByType(String type);

    Page<Feedback> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    List<Feedback> findTop5ByStatusOrderByCreatedAtDesc(String status);

    List<Feedback> findTop5ByOrderByCreatedAtDesc();
}
