package com.shike.service;

import com.shike.model.dto.PetCreateDTO;
import com.shike.model.dto.PetDTO;

import java.time.LocalDate;
import java.util.Map;

public interface PetService {
    PetDTO getMyPet(Long userId);
    PetDTO createPet(Long userId, PetCreateDTO dto);
    PetDTO feedPet(Long userId);
    boolean awardExerciseFood(Long userId, LocalDate date);
    Map<String, Object> generateAvatar(Long userId, String petType, String promptHint);
}
