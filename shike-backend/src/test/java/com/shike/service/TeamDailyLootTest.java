package com.shike.service;

import com.shike.model.entity.*;
import com.shike.repository.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@SpringBootTest
@ActiveProfiles("test")
public class TeamDailyLootTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DietRecordRepository dietRecordRepository;

    @Autowired
    private TeamLootRecordRepository teamLootRecordRepository;

    @Autowired
    private TeamDailySettlementRepository teamDailySettlementRepository;

    @Test
    @Transactional
    public void testDailySettlementAndLootClaim() {
        // 1. 创建测试用户
        User user1 = userRepository.save(User.builder().openid("test_openid_a_" + System.currentTimeMillis()).nickname("达标玩家A").points(500).build());
        User user2 = userRepository.save(User.builder().openid("test_openid_b_" + System.currentTimeMillis()).nickname("违约玩家B").points(500).build());

        // 2. 创建小队
        Team team = teamRepository.save(Team.builder()
                .teamName("7天自律先锋队")
                .creatorId(user1.getId())
                .inviteCode("TST777")
                .targetDays(7)
                .depositPoints(700)
                .status("ACTIVE")
                .build());

        teamMemberRepository.save(TeamMember.builder().teamId(team.getId()).userId(user1.getId()).build());
        teamMemberRepository.save(TeamMember.builder().teamId(team.getId()).userId(user2.getId()).build());

        // 3. 模拟今日 user1 有达标饮食记录，user2 无记录
        LocalDate today = LocalDate.now();
        dietRecordRepository.save(DietRecord.builder()
                .userId(user1.getId())
                .recordDate(today)
                .totalCalories(BigDecimal.valueOf(1500))
                .mealType("LUNCH")
                .foodItems("[]")
                .build());

        // 4. 执行今日结算
        teamService.settleDailyTeamChallenges(today);

        // 5. 验证结算总表
        List<TeamDailySettlement> settlements = teamDailySettlementRepository.findByTeamIdOrderBySettlementDateDesc(team.getId());
        Assertions.assertFalse(settlements.isEmpty(), "应该生成结算记录");
        TeamDailySettlement s = settlements.get(0);
        Assertions.assertEquals(1, s.getSuccessMembers(), "1人达标");
        Assertions.assertEquals(1, s.getFailedMembers(), "1人违约");
        Assertions.assertTrue(s.getPenaltyPool() > 0, "违约金池大于0");

        // 6. 验证 user1 生成了待开盲盒
        TeamLootRecord loot = teamService.getPendingDailyLoot(user1.getId());
        Assertions.assertNotNull(loot, "达标用户应该拥有待领盲盒");
        Assertions.assertEquals("UNCLAIMED", loot.getStatus());

        // 7. 执行开盲盒
        Map<String, Object> claimRes = teamService.claimDailyLoot(user1.getId(), loot.getId());
        Assertions.assertNotNull(claimRes.get("finalPoints"));
        Assertions.assertNotNull(claimRes.get("multiplier"));
        System.out.println(">>> 本地测试开盲盒成功！抽中倍率: " + claimRes.get("multiplier") + "x, 最终获得积分: " + claimRes.get("finalPoints") + ", 掉落道具: " + claimRes.get("itemReward"));
    }
}
