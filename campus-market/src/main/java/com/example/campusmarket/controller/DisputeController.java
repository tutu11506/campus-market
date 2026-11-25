package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.Dispute;
import com.example.campusmarket.service.DisputeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 交易纠纷仲裁：买卖双方提交纠纷与证据，管理员介入并裁决。
 */
@RestController
@RequestMapping("/dispute")
public class DisputeController {

    @Autowired
    private DisputeService disputeService;

    @PostMapping("/create")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        Integer orderId = toInt(body.get("orderId"));
        if (orderId == null) return Result.error("请选择订单");
        String reasonType = (String) body.get("reasonType");
        String description = (String) body.get("description");
        BigDecimal claimAmount = null;
        if (body.get("claimAmount") != null && !"".equals(body.get("claimAmount"))) {
            try {
                claimAmount = new BigDecimal(String.valueOf(body.get("claimAmount")));
            } catch (NumberFormatException e) {
                return Result.error("诉求金额格式不正确");
            }
        }
        return Result.success(disputeService.create(uid, orderId, reasonType, description, claimAmount));
    }

    @PostMapping("/evidence")
    public Result<String> evidence(@RequestBody Map<String, Object> body) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        Integer disputeId = toInt(body.get("disputeId"));
        if (disputeId == null) return Result.error("缺少纠纷ID");
        String type = (String) body.get("type");
        String content = (String) body.get("content");
        String fileUrl = (String) body.get("fileUrl");
        return Result.success(disputeService.addEvidence(uid, disputeId, type, content, fileUrl));
    }

    @GetMapping("/my")
    public Result<PageResult<Dispute>> my(@RequestParam(required = false) String status,
                                          @RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        return Result.success(disputeService.myPage(uid, status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        return Result.success(disputeService.detail(uid, id));
    }

    @GetMapping("/admin/list")
    public Result<PageResult<Dispute>> adminList(@RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(disputeService.adminPage(status, pageNum, pageSize));
    }

    @GetMapping("/admin/{id}")
    public Result<Map<String, Object>> adminDetail(@PathVariable Integer id) {
        return Result.success(disputeService.adminDetail(id));
    }

    @PutMapping("/admin/processing")
    public Result<String> processing(@RequestParam Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        return Result.success(disputeService.startProcessing(uid, id));
    }

    @PutMapping("/admin/resolve")
    public Result<String> resolve(@RequestBody Map<String, Object> body) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("未登录");
        Integer id = toInt(body.get("id"));
        if (id == null) return Result.error("缺少纠纷ID");
        String verdict = (String) body.get("verdict");
        String note = (String) body.get("note");
        if (verdict == null || verdict.isEmpty()) return Result.error("请选择裁决结果");
        return Result.success(disputeService.resolve(uid, id, verdict, note));
    }

    @GetMapping("/admin/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(disputeService.stats());
    }

    private Integer toInt(Object v) {
        if (v == null) return null;
        try {
            return Integer.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
