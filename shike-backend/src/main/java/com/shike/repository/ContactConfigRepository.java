package com.shike.repository;

import com.shike.model.entity.ContactConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContactConfigRepository extends JpaRepository<ContactConfig, Long> {
    Optional<ContactConfig> findFirstByOrderByIdAsc();
}
