package com.shike.repository;

import com.shike.model.entity.UserItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserItemRepository extends JpaRepository<UserItem, Long> {
    List<UserItem> findByUserId(Long userId);
    Optional<UserItem> findByUserIdAndItemType(Long userId, String itemType);
}
