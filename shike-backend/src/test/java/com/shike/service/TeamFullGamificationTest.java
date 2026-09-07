package com.shike.service;

import com.shike.model.entity.*;
import com.shike.repository.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@SpringBootTest
@ActiveProfiles("test")
public class TeamFullGamificationTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserItemRepository userItemRepository;

    @Autowired
    private TeamSpyVoteRepository teamSpyVoteRepository;

    @Autowired
    private TeamAuditTaskRepository teamAuditTaskRepository;

    @Test
    @Transactional
    public void testSaboteurAndItemShopAndAiRoast() {
        long ts = System.currentTimeMillis();

        // 1. 创建 3 位玩家
        User u1 = userRepository.save(User.builder().openid("test_spy_1_" + ts).nickname("自律队长").points(1000).build());
        User u2 = userRepository.save(User.builder().openid("test_spy_2_" + ts).nickname("潜伏卧底").points(800).build());
        User u3 = userRepository.save(User.builder().openid("test_spy_3_" + ts).nickname("热血平民").points(600).build());

        // 2. 创建 3 人小队
        Team team = teamRepository.save(Team.builder()
                .teamName("减脂狼人杀突击队")
                .creatorId(u1.getId())
                .inviteCode("SPY" + (ts % 1000))
                .targetDays(7)
                .depositPoints(700)
                .status("ACTIVE")
                .build());

        teamMemberRepository.save(TeamMember.builder().teamId(team.getId()).userId(u1.getId()).build());
        teamMemberRepository.save(TeamMember.builder().teamId(team.getId()).userId(u2.getId()).build());
        teamMemberRepository.save(TeamMember.builder().teamId(team.getId()).userId(u3.getId()).build());

        // 3. 测试【玩法 2：减脂卧底模式】
        Map<String, Object> spyStatus = teamService.getSpyGameStatus(u1.getId(), team.getId());
        Assertions.assertTrue((Boolean) spyStatus.get("enabled"), "3人队伍应该激活卧底模式");
        Assertions.assertNotNull(spyStatus.get("roleName"));

        // 获取真正被分配的卧底 ID
        Team refreshedTeam = teamRepository.findById(team.getId()).orElseThrow();
        Long actualSpyId = refreshedTeam.getSpyUserId();
        Assertions.assertNotNull(actualSpyId, "系统应该自动指派了卧底");

        // 卧底发挑衅
        String tauntMsg = teamService.postSpyTaunt(actualSpyId, team.getId(), "今晚夜宵炸鸡安排上！", null);
        Assertions.assertTrue(tauntMsg.contains("成功"));

        // 平民投票抓卧底
        String voteMsg = teamService.castSpyVote(u1.getId(), u2.getId(), team.getId());
        Assertions.assertTrue(voteMsg.contains("成功"));

        // 4. 测试【玩法 3：对赌战术道具店】
        List<Map<String, Object>> shopItems = teamService.getShopItems(u1.getId());
        Assertions.assertEquals(4, shopItems.size(), "商店应该有4种道具");

        // 购买欺骗餐护盾 (150分)
        Map<String, Object> buyShield = teamService.buyShopItem(u1.getId(), "CHEAT_SHIELD");
        Assertions.assertEquals(1, buyShield.get("owned"));
        Assertions.assertEquals(850, buyShield.get("remainingPoints"));

        // 购买查岗狙击卡 (50分)
        teamService.buyShopItem(u1.getId(), "SNIPER_AUDIT");

        // 使用欺骗餐护盾豁免
        Map<String, Object> useShieldRes = teamService.useCheatShield(u1.getId(), team.getId(), LocalDate.now());
        Assertions.assertTrue(((String) useShieldRes.get("message")).contains("生效"));

        // 发起突击查岗 u1 -> u3
        TeamAuditTask task = teamService.triggerSniperAudit(u1.getId(), u3.getId(), team.getId());
        Assertions.assertNotNull(task.getId());
        Assertions.assertEquals("PENDING", task.getStatus());

        // u3 响应查岗
        Map<String, Object> respAudit = teamService.respondToAudit(u3.getId(), task.getId(), 999L);
        Assertions.assertTrue(((String) respAudit.get("message")).contains("成功"));

        // 5. 测试【玩法 4：AI 营养师毒舌战报】
        TeamAiRoast roast = teamService.generateDailyAiRoast(team.getId(), LocalDate.now());
        Assertions.assertNotNull(roast.getTitle());
        Assertions.assertNotNull(roast.getContent());
        System.out.println(">>> AI 每日毒舌战报生成成功！标题: " + roast.getTitle() + " | 内容: " + roast.getContent());
    }
}
