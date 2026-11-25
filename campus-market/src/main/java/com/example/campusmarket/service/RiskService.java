package com.example.campusmarket.service;

import com.example.campusmarket.common.ForbiddenException;
import com.example.campusmarket.mapper.RiskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RiskService {
    @Autowired
    private RiskMapper riskMapper;

    private volatile List<String> cachedWords = null;

    private List<String> getSensitiveWords() {
        if (cachedWords == null) {
            synchronized (this) {
                if (cachedWords == null) {
                    cachedWords = riskMapper.findAllSensitiveWords();
                }
            }
        }
        return cachedWords;
    }

    public String checkSensitive(String... texts) {
        List<String> words = getSensitiveWords();
        if (words == null || words.isEmpty()) return "";
        for (String text : texts) {
            if (text == null) continue;
            for (String word : words) {
                if (word == null || word.isEmpty()) continue;
                if (text.contains(word)) return word;
            }
        }
        return "";
    }

    public void assertNoSensitive(String... texts) {
        String hit = checkSensitive(texts);
        if (!hit.isEmpty()) {
            throw new ForbiddenException("内容包含违规词[" + hit + "],请修改后重试");
        }
    }

    public boolean isBlocked(int a, int b) {
        return riskMapper.isBlocked(a, b) > 0 || riskMapper.isBlocked(b, a) > 0;
    }

    public String blockUser(int a, int b) {
        if (a == b) return "不能拉黑自己";
        riskMapper.blockUser(a, b);
        return "拉黑成功";
    }

    public String unblockUser(int a, int b) {
        riskMapper.unblockUser(a, b);
        return "已解除拉黑";
    }

    public String report(int reporterId, String targetType, int targetId, String reason, String detail) {
        riskMapper.insertReport(reporterId, targetType, targetId, reason, detail);
        return "举报成功";
    }

    public java.util.List<java.util.Map<String, Object>> listReports(int limit, int offset) {
        return riskMapper.findReports(limit, offset);
    }

    public int countReports() {
        return riskMapper.countReports();
    }

    public void handleReport(int reportId, String status, int handlerId) {
        riskMapper.handleReport(reportId, status, handlerId);
    }

    public int deductCredit(int userId, int delta) {
        riskMapper.deductCredit(userId, delta);
        Integer c = riskMapper.findCredit(userId);
        return c == null ? 0 : c;
    }

    public int getCredit(int userId) {
        Integer c = riskMapper.findCredit(userId);
        return c == null ? 100 : c;
    }

    public boolean isMuted(int userId) {
        return getCredit(userId) < 60;
    }

    public void refreshWords() { cachedWords = null; }
}