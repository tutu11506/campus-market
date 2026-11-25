package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.service.RiskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/risk")
public class RiskController {

    @Autowired
    private RiskService riskService;

    @PostMapping("/report")
    public Result<String> report(@RequestBody Map<String, Object> body) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        String targetType = (String) body.get("targetType");
        Integer targetId = (Integer) body.get("targetId");
        String reason = (String) body.get("reason");
        String detail = (String) body.get("detail");
        if (targetType == null || targetId == null) return Result.error("缺少举报目标");
        String msg = riskService.report(uid, targetType, targetId, reason, detail);
        return Result.success(msg);
    }

    @PostMapping("/block")
    public Result<String> block(@RequestParam Integer blockedUserId) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        String msg = riskService.blockUser(uid, blockedUserId);
        return Result.success(msg);
    }

    @PostMapping("/unblock")
    public Result<String> unblock(@RequestParam Integer blockedUserId) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        String msg = riskService.unblockUser(uid, blockedUserId);
        return Result.success(msg);
    }

    @GetMapping("/credit")
    public Result<Map<String, Object>> credit() {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        Map<String, Object> data = new HashMap<>();
        int c = riskService.getCredit(uid);
        data.put("credit", c);
        data.put("muted", c < 60);
        return Result.success(data);
    }

    @GetMapping("/admin/report/list")
    public Result<PageResult<Map<String, Object>>> adminReportList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        int total = riskService.countReports();
        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> list = riskService.listReports(pageSize, offset);
        return Result.success(PageResult.of(list, total, pageNum, pageSize));
    }

    @PutMapping("/admin/report/handle")
    public Result<String> adminHandleReport(
            @RequestParam Integer reportId,
            @RequestParam String status) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        riskService.handleReport(reportId, status, uid);
        return Result.success("处置完成");
    }

    @PutMapping("/admin/credit/deduct")
    public Result<Map<String, Object>> adminDeductCredit(
            @RequestParam Integer userId,
            @RequestParam(defaultValue = "10") Integer delta) {
        int c = riskService.deductCredit(userId, delta);
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("credit", c);
        return Result.success(data);
    }

    @PostMapping("/admin/refresh-words")
    public Result<String> adminRefreshWords() {
        riskService.refreshWords();
        return Result.success("敏感词缓存已刷新");
    }
}