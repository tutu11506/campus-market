package com.example.campusmarket.controller;

import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.CampusTask;
import com.example.campusmarket.mapper.CampusTaskMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 校园任务大厅：同学发布悬赏任务，其他同学接单完成。
 *
 * 竞争壁垒：
 *   闲鱼只能"卖东西"，卖不了"帮忙"。
 *   但校园里最高频的需求其实是跑腿取快递、搬宿舍、辅导作业这类"任务"，
 *   这些需求天然需要"同校 + 面交 + 熟人社会"的信任基础，只有校园平台能做。
 */
@RestController
@RequestMapping("/task")
public class CampusTaskController {

    @Autowired
    private CampusTaskMapper taskMapper;

    /** 发布任务 */
    @PostMapping("/publish")
    public Result<Map<String, Object>> publish(@RequestBody PublishRequest req) {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录后再发布任务");
        }

        String title = req.getTitle() == null ? "" : req.getTitle().trim();
        if (title.length() < 2 || title.length() > 100) {
            return Result.error("任务标题需要 2-100 个字");
        }
        String description = req.getDescription() == null ? "" : req.getDescription().trim();
        if (description.length() > 800) {
            return Result.error("任务描述不能超过 800 个字");
        }

        BigDecimal reward = req.getReward() == null ? BigDecimal.ZERO : req.getReward();
        if (reward.compareTo(BigDecimal.ZERO) < 0) {
            return Result.error("悬赏金额不能为负数");
        }
        if (reward.compareTo(new BigDecimal("9999")) > 0) {
            return Result.error("悬赏金额不能超过 9999 元");
        }

        Date deadline = null;
        if (req.getDeadline() != null && !req.getDeadline().trim().isEmpty()) {
            deadline = parseDeadline(req.getDeadline());
            if (deadline == null) {
                return Result.error("期望完成时间格式不正确");
            }
            if (deadline.before(new Date())) {
                return Result.error("期望完成时间需要晚于当前时间");
            }
        }

        CampusTask task = new CampusTask();
        task.setTitle(title);
        task.setDescription(description);
        task.setReward(reward);
        task.setCategory(req.getCategory() == null || req.getCategory().trim().isEmpty()
                ? "校园跑腿" : req.getCategory().trim());
        task.setLocation(req.getLocation() == null ? "" : req.getLocation().trim());
        task.setDeadline(deadline);
        task.setPublisherId(userId);
        taskMapper.insert(task);

        return Result.success(taskMapper.findDetail(task.getId()));
    }

    /** 任务大厅列表（公开） */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize) {
        int safePage = Math.max(pageNum, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 50);
        String statusFilter = ("all".equals(status) || status == null || status.isEmpty()) ? null : status;

        List<Map<String, Object>> rows = taskMapper.findPage(statusFilter, keyword, (safePage - 1) * safeSize, safeSize);
        int total = taskMapper.countPage(statusFilter, keyword);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", rows);
        data.put("total", total);
        data.put("pageNum", safePage);
        data.put("pageSize", safeSize);
        data.put("hasMore", (long) safePage * safeSize < total);
        return Result.success(data);
    }

    /** 任务详情（公开） */
    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Integer id) {
        Map<String, Object> task = taskMapper.findDetail(id);
        if (task == null) {
            return Result.error("任务不存在或已被删除");
        }
        return Result.success(task);
    }

    /** 接单 */
    @PostMapping("/{id}/accept")
    public Result<Map<String, Object>> accept(@PathVariable Integer id) {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录后再接单");
        }
        Map<String, Object> basic = taskMapper.findBasic(id);
        if (basic == null) {
            return Result.error("任务不存在或已被删除");
        }
        String status = String.valueOf(basic.get("status"));
        if ("open".equals(status) == false) {
            return Result.error(statusMessage(status));
        }
        int publisherId = toInt(basic.get("publisherId"));
        if (publisherId == userId) {
            return Result.error("不能接自己发布的任务");
        }
        if (taskMapper.accept(id, userId) == 0) {
            return Result.error("手慢了，任务已被其他同学接走");
        }
        return Result.success(taskMapper.findDetail(id));
    }

    /** 发布人确认完成 */
    @PostMapping("/{id}/finish")
    public Result<Map<String, Object>> finish(@PathVariable Integer id) {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        Map<String, Object> basic = taskMapper.findBasic(id);
        if (basic == null) {
            return Result.error("任务不存在或已被删除");
        }
        if (toInt(basic.get("publisherId")) != userId) {
            return Result.error("只有发布人可以确认完成");
        }
        if (!"accepted".equals(String.valueOf(basic.get("status")))) {
            return Result.error("只有进行中的任务才能确认完成");
        }
        if (taskMapper.finish(id, userId) == 0) {
            return Result.error("任务状态已变化，请刷新后重试");
        }
        return Result.success(taskMapper.findDetail(id));
    }

    /** 发布人取消（仅待接单） */
    @PostMapping("/{id}/cancel")
    public Result<Map<String, Object>> cancel(@PathVariable Integer id) {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        Map<String, Object> basic = taskMapper.findBasic(id);
        if (basic == null) {
            return Result.error("任务不存在或已被删除");
        }
        if (toInt(basic.get("publisherId")) != userId) {
            return Result.error("只有发布人可以取消任务");
        }
        if (!"open".equals(String.valueOf(basic.get("status")))) {
            return Result.error("任务已被接单或已结束，无法取消");
        }
        if (taskMapper.cancel(id, userId) == 0) {
            return Result.error("任务状态已变化，请刷新后重试");
        }
        return Result.success(taskMapper.findDetail(id));
    }

    /** 我发布的任务 */
    @GetMapping("/my/published")
    public Result<List<Map<String, Object>>> myPublished() {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        return Result.success(taskMapper.findByPublisher(userId));
    }

    /** 我接的任务 */
    @GetMapping("/my/accepted")
    public Result<List<Map<String, Object>>> myAccepted() {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("请先登录");
        }
        return Result.success(taskMapper.findByAccepter(userId));
    }

    /** 状态对应的提示文案 */
    private String statusMessage(String status) {
        if ("accepted".equals(status)) return "任务已被接走啦";
        if ("completed".equals(status)) return "任务已完成";
        if ("cancelled".equals(status)) return "任务已取消";
        return "任务状态异常";
    }

    /** 解析期望完成时间，支持 yyyy-MM-dd HH:mm / yyyy-MM-dd HH:mm:ss / ISO 格式 */
    private Date parseDeadline(String raw) {
        String v = raw.trim().replace('T', ' ');
        if (v.length() == 16) {
            v = v + ":00";
        }
        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"};
        for (String p : patterns) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(p);
                f.setLenient(false);
                return f.parse(v);
            } catch (Exception ignored) {
                // 尝试下一个格式
            }
        }
        return null;
    }

    private int toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 发布任务请求体 */
    @Data
    public static class PublishRequest {
        private String title;
        private String description;
        private BigDecimal reward;
        private String category;
        private String location;
        private String deadline;
    }
}
