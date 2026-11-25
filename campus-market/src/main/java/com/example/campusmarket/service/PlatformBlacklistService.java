package com.example.campusmarket.service;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.entity.Message;
import com.example.campusmarket.entity.PlatformBlacklist;
import com.example.campusmarket.entity.User;
import com.example.campusmarket.mapper.MessageMapper;
import com.example.campusmarket.mapper.PlatformBlacklistMapper;
import com.example.campusmarket.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台共享黑名单：管理员标记校内失信（多次放鸽子、骗东西等），
 * 全平台在商品详情 / 聊天等场景提醒其他同学谨慎交易。
 */
@Service
public class PlatformBlacklistService {

    private static final int SYSTEM_SENDER_ID = 1;
    private static final List<String> LEVELS = List.of("watch", "warning", "serious");

    @Autowired
    private PlatformBlacklistMapper blacklistMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private MessageMapper messageMapper;

    @Transactional
    public String mark(Integer adminId, Integer userId, String level, String reason, String evidence) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (blacklistMapper.countActiveByUser(userId) > 0) {
            throw new IllegalArgumentException("该用户已在平台黑名单中，请先解除再重新标记");
        }
        if (!LEVELS.contains(level)) {
            throw new IllegalArgumentException("非法失信等级");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("请填写失信原因");
        }

        PlatformBlacklist item = new PlatformBlacklist();
        item.setUserId(userId);
        item.setLevel(level);
        item.setReason(cut(reason.trim(), 300));
        item.setEvidence(cut(evidence, 500));
        item.setReportCount(blacklistMapper.countUserReports(userId));
        item.setCreatedBy(adminId);
        blacklistMapper.insert(item);

        String levelText = levelText(level);
        notify(userId, "【信用提醒】你的账号已被平台标记为" + levelText + "，原因：" + cut(reason.trim(), 100)
                + "。如有异议请联系管理员申诉。");
        return "已加入平台黑名单";
    }

    @Transactional
    public String revoke(Integer adminId, Integer id) {
        PlatformBlacklist item = blacklistMapper.findById(id);
        if (item == null) {
            throw new IllegalArgumentException("黑名单记录不存在");
        }
        if (!"active".equals(item.getStatus())) {
            throw new IllegalArgumentException("该记录已解除");
        }
        blacklistMapper.revoke(id, adminId);
        notify(item.getUserId(), "【信用提醒】你在平台上的失信标记已被解除，请继续保持良好交易记录。");
        return "已解除失信标记";
    }

    /** 公开查询：给全平台做风险提醒 */
    public Map<String, Object> check(Integer userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("blocked", false);
        data.put("level", null);
        data.put("reason", null);
        data.put("message", null);
        if (userId == null) return data;

        PlatformBlacklist item = blacklistMapper.findActiveByUser(userId);
        if (item == null) return data;

        data.put("blocked", true);
        data.put("level", item.getLevel());
        data.put("reason", item.getReason());
        data.put("reportCount", item.getReportCount());
        data.put("createTime", item.getCreateTime());
        String prefix = switch (item.getLevel()) {
            case "serious" -> "该用户已被平台标记为严重失信";
            case "warning" -> "该用户曾被平台警告";
            default -> "该用户处于平台关注名单";
        };
        data.put("message", prefix + "（" + item.getReason() + "），交易前请核实并保留证据。");
        return data;
    }

    public PageResult<PlatformBlacklist> page(String status, Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        List<PlatformBlacklist> list = blacklistMapper.findPage(status, p[1], p[2]);
        int total = blacklistMapper.countPage(status);
        return PageResult.of(list, total, p[0], p[1]);
    }

    public Map<String, Object> stats() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("active", blacklistMapper.countActive());
        data.put("total", blacklistMapper.countAll());
        return data;
    }

    private String levelText(String level) {
        return switch (level) {
            case "serious" -> "严重失信";
            case "warning" -> "失信警告";
            default -> "重点关注";
        };
    }

    private void notify(Integer receiverId, String content) {
        if (receiverId == null) return;
        Message msg = new Message();
        msg.setSenderId(SYSTEM_SENDER_ID);
        msg.setReceiverId(receiverId);
        msg.setContent(cut(content, 480));
        msg.setMsgType("text");
        msg.setAutoReply(0);
        messageMapper.insert(msg);
    }

    private String cut(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
