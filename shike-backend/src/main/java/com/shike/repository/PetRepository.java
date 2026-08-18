package com.shike.repository;

import com.shike.model.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {
    Optional<Pet> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
