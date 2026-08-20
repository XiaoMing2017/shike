package com.shike.controller;

import com.shike.common.BizException;
import com.shike.common.ResultDTO;
import com.shike.model.dto.PetCreateDTO;
import com.shike.model.dto.PetDTO;
import com.shike.model.dto.PetInteractDTO;
import com.shike.model.vo.PetInteractVO;
import com.shike.service.PetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/pet")
@RequiredArgsConstructor
@Slf4j
public class PetController {

    private final PetService petService;

    @GetMapping("/my")
    public ResultDTO<PetDTO> getMyPet(@RequestParam(required = false) Long userId) {
        if (userId == null) {
            return ResultDTO.success(null);
        }
        PetDTO pet = petService.getMyPet(userId);
        return ResultDTO.success(pet);
    }

    @PostMapping("/create")
    public ResultDTO<PetDTO> createPet(@RequestBody PetCreateDTO dto) {
        if (dto.getUserId() == null) {
            throw new BizException(400, "用户ID不能为空");
        }
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BizException(400, "宠物名字不能为空");
        }
        PetDTO pet = petService.createPet(dto.getUserId(), dto);
        return ResultDTO.success(pet);
    }

    @PostMapping("/feed")
    public ResultDTO<PetDTO> feedPet(@RequestParam Long userId) {
        if (userId == null) {
            throw new BizException(400, "用户ID不能为空");
        }
        PetDTO pet = petService.feedPet(userId);
        return ResultDTO.success(pet);
    }

    @PostMapping("/generate-avatar")
    public ResultDTO<Map<String, Object>> generateAvatar(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "DRAGON") String petType,
            @RequestParam(required = false) String prompt) {
        Map<String, Object> res = petService.generateAvatar(userId, petType, prompt);
        return ResultDTO.success(res);
    }

    @PostMapping("/checkin")
    public ResultDTO<Map<String, Object>> checkin(@RequestParam Long userId) {
        if (userId == null) {
            throw new BizException(400, "用户ID不能为空");
        }
        Map<String, Object> res = petService.checkin(userId);
        return ResultDTO.success(res);
    }

    @GetMapping("/food-tasks")
    public ResultDTO<Map<String, Object>> getTodayFoodTasks(@RequestParam Long userId) {
        if (userId == null) {
            throw new BizException(400, "用户ID不能为空");
        }
        Map<String, Object> res = petService.getTodayFoodTasks(userId);
        return ResultDTO.success(res);
    }

    /**
     * 搭子 AI 动态拟人互动与树洞对话
     */
    @PostMapping("/interact")
    public ResultDTO<PetInteractVO> interact(@RequestBody PetInteractDTO dto) {
        if (dto.getUserId() == null) {
            throw new BizException(400, "用户ID不能为空");
        }
        PetInteractVO vo = petService.interactWithAi(dto.getUserId(), dto);
        return ResultDTO.success(vo);
    }
}
