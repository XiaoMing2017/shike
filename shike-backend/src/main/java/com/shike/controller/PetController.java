package com.shike.controller;

import com.shike.common.ResultDTO;
import com.shike.model.dto.PetCreateDTO;
import com.shike.model.dto.PetDTO;
import com.shike.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping({"/pet", "/api/v1/pet"})
@RequiredArgsConstructor
@Slf4j
public class PetController {

    private final PetService petService;

    @GetMapping("/my")
    public ResultDTO<PetDTO> getMyPet(@RequestParam Long userId) {
        return ResultDTO.success(petService.getMyPet(userId));
    }

    @PostMapping("/create")
    public ResultDTO<PetDTO> createPet(@Valid @RequestBody PetCreateDTO dto) {
        return ResultDTO.success(petService.createPet(dto.getUserId(), dto));
    }

    @PostMapping("/feed")
    public ResultDTO<PetDTO> feedPet(@RequestParam Long userId) {
        return ResultDTO.success(petService.feedPet(userId));
    }

    @PostMapping("/claim-reward")
    public ResultDTO<Boolean> claimExerciseReward(@RequestParam Long userId) {
        boolean claimed = petService.awardExerciseFood(userId, LocalDate.now());
        return ResultDTO.success(claimed);
    }

    @PostMapping("/generate-avatar")
    public ResultDTO<Map<String, Object>> generateAvatar(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "DRAGON") String petType,
            @RequestParam(required = false) String promptHint) {
        return ResultDTO.success(petService.generateAvatar(userId, petType, promptHint));
    }
}
