package com.shike.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shike.model.dto.AdminFeedbackPageDTO;
import com.shike.model.dto.FeedbackDTO;
import com.shike.model.entity.Feedback;
import com.shike.model.entity.PointsRecord;
import com.shike.model.entity.User;
import com.shike.repository.FeedbackRepository;
import com.shike.repository.PointsRecordRepository;
import com.shike.repository.UserRepository;
import com.shike.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final PointsRecordRepository pointsRecordRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public FeedbackDTO submitFeedback(Long userId, FeedbackDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getContent())) {
            throw new IllegalArgumentException("反馈内容不能为空");
        }

        String imagesJson = null;
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            try {
                imagesJson = objectMapper.writeValueAsString(dto.getImages());
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize feedback images", e);
            }
        }

        Feedback feedback = Feedback.builder()
                .userId(userId)
                .type(StringUtils.hasText(dto.getType()) ? dto.getType().toUpperCase() : "BUG")
                .content(dto.getContent().trim())
                .images(imagesJson)
                .contactInfo(dto.getContactInfo())
                .status("PENDING")
                .pointsRewarded(0)
                .build();

        Feedback saved = feedbackRepository.save(feedback);
        return convertToDTO(saved);
    }

    @Override
    public List<FeedbackDTO> getUserFeedbacks(Long userId) {
        List<Feedback> list = feedbackRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return list.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public AdminFeedbackPageDTO getAdminFeedbacks(String status, String type, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Feedback> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
                predicates.add(cb.equal(root.get("status"), status.toUpperCase()));
            }
            if (StringUtils.hasText(type) && !"ALL".equalsIgnoreCase(type)) {
                predicates.add(cb.equal(root.get("type"), type.toUpperCase()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Feedback> pageResult = feedbackRepository.findAll(spec, pageable);

        List<FeedbackDTO> items = pageResult.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        long pendingCount = feedbackRepository.countByStatus("PENDING");
        long bugCount = feedbackRepository.countByType("BUG");
        long suggestionCount = feedbackRepository.countByType("SUGGESTION");
        long resolvedCount = feedbackRepository.countByStatus("RESOLVED");

        return AdminFeedbackPageDTO.builder()
                .items(items)
                .total(pageResult.getTotalElements())
                .pendingCount(pendingCount)
                .bugCount(bugCount)
                .suggestionCount(suggestionCount)
                .resolvedCount(resolvedCount)
                .build();
    }

    @Override
    @Transactional
    public FeedbackDTO processFeedback(Long feedbackId, String status, String adminReply, Integer rewardPoints) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("反馈记录不存在: " + feedbackId));

        if (StringUtils.hasText(status)) {
            feedback.setStatus(status.toUpperCase());
        }

        if (StringUtils.hasText(adminReply)) {
            feedback.setAdminReply(adminReply.trim());
        }

        // 奖励契约积分处理
        if (rewardPoints != null && rewardPoints > 0) {
            User user = userRepository.findById(feedback.getUserId()).orElse(null);
            if (user != null) {
                int oldPoints = user.getPoints() != null ? user.getPoints() : 0;
                int newPoints = oldPoints + rewardPoints;
                user.setPoints(newPoints);
                userRepository.save(user);

                // 写入积分明细日志
                PointsRecord record = PointsRecord.builder()
                        .userId(user.getId())
                        .type("SYSTEM_GRANT")
                        .points(rewardPoints)
                        .balance(newPoints)
                        .remark("采纳意见/BUG反馈奖励积分 (" + feedback.getType() + " #" + feedback.getId() + ")")
                        .build();
                pointsRecordRepository.save(record);

                feedback.setPointsRewarded((feedback.getPointsRewarded() != null ? feedback.getPointsRewarded() : 0) + rewardPoints);
            }
        }

        Feedback saved = feedbackRepository.save(feedback);
        return convertToDTO(saved);
    }

    @Override
    public Map<String, Object> getFeedbackStatsWidget() {
        Map<String, Object> map = new HashMap<>();
        long pendingCount = feedbackRepository.countByStatus("PENDING");
        List<Feedback> latestList = feedbackRepository.findTop5ByOrderByCreatedAtDesc();

        List<FeedbackDTO> latestDtos = latestList.stream().map(this::convertToDTO).collect(Collectors.toList());

        map.put("pendingCount", pendingCount);
        map.put("latestItems", latestDtos);
        return map;
    }

    private FeedbackDTO convertToDTO(Feedback feedback) {
        List<String> imgList = new ArrayList<>();
        if (StringUtils.hasText(feedback.getImages())) {
            try {
                imgList = objectMapper.readValue(feedback.getImages(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse feedback images JSON", e);
            }
        }

        String nickname = "未知用户";
        String avatar = null;
        User user = userRepository.findById(feedback.getUserId()).orElse(null);
        if (user != null) {
            nickname = StringUtils.hasText(user.getNickname()) ? user.getNickname() : "用户 " + user.getId();
            avatar = user.getAvatar();
        }

        return FeedbackDTO.builder()
                .id(feedback.getId())
                .userId(feedback.getUserId())
                .userNickname(nickname)
                .userAvatar(avatar)
                .type(feedback.getType())
                .content(feedback.getContent())
                .images(imgList)
                .contactInfo(feedback.getContactInfo())
                .status(feedback.getStatus())
                .adminReply(feedback.getAdminReply())
                .pointsRewarded(feedback.getPointsRewarded())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }
}
