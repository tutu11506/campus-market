package com.example.campusmarket.controller;

import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.mapper.BlindBoxMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 闲置盲盒许愿：买家按品类许愿，卖家打包随机发货，
 * 用一点小惊喜帮闲置更快流转。
 */
@RestController
@RequestMapping("/blindbox")
public class BlindBoxController {

    private static final List<String> CATEGORIES = List.of("文具", "书籍", "数码配件", "生活杂物", "随机");

    private static final Map<String, String> STATUS_LABELS = Map.of(
            "open", "待认领",
            "matched", "已打包待确认",
            "completed", "已完成",
            "cancelled", "已取消"
    );

    @Autowired
    private BlindBoxMapper blindBoxMapper;

    /**
     * scope: open 待认领 / mine 我的许愿 / claimed 我接的单
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam(required = false) String scope) {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return Result.error("未登录");

        String s = scope == null ? "open" : scope.trim();
        if (!List.of("open", "mine", "claimed").contains(s)) {
            throw new IllegalArgumentException("不支持的范围：" + s);
        }

        List<Map<String, Object>> list = switch (s) {
            case "mine" -> blindBoxMapper.findMine(userId);
            case "claimed" -> blindBoxMapper.findClaimed(userId);
            default -> blindBoxMapper.findOpen(userId);
        };
        list.forEach(item -> decorate(item, userId));

        Map<String, Object> countRow = blindBoxMapper.countByScope(userId);
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("open", toInt(countRow == null ? null : countRow.get("openCount")));
        counts.put("mine", toInt(countRow == null ? null : countRow.get("mineCount")));
        counts.put("claimed", toInt(countRow == null ? null : countRow.get("claimedCount")));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scope", s);
        data.put("list", list);
        data.put("counts", counts);
        data.put("categories", CATEGORIES);
        data.put("note", "卖家随机打包，内容以实物为准；同一个许愿只会被一位卖家认领");
        data.put("hasData", !list.isEmpty());
        return Result.success(data);
    }

    @PostMapping("/wish")
    public Result<Map<String, Object>> wish(@RequestBody Map<String, Object> body) {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return Result.error("未登录");

        if (body == null) throw new IllegalArgumentException("请填写许愿内容");
        String category = str(body.get("category"));
        if (!CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("请选择许愿品类");
        }
        Double budget = toDouble(body.get("budget"));
        if (budget == null || budget < 5 || budget > 200) {
            throw new IllegalArgumentException("预算需在 5 ~ 200 元之间");
        }
        String wishNote = str(body.get("wishNote"));
        if (wishNote.length() > 100) {
            throw new IllegalArgumentException("许愿说明不能超过 100 字");
        }

        blindBoxMapper.insertWish(userId, category, budget, wishNote);
        Map<String, Object> latest = blindBoxMapper.findLatestByBuyer(userId);
        if (latest != null) decorate(latest, userId);
        return Result.success(latest);
    }

    @PostMapping("/{id}/claim")
    public Result<Map<String, Object>> claim(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return Result.error("未登录");

        Map<String, Object> wish = blindBoxMapper.findById(id);
        if (wish == null) throw new IllegalArgumentException("许愿不存在");
        if (userId.equals(toInt(wish.get("buyerId")))) {
            throw new IllegalArgumentException("不能认领自己的许愿");
        }

        if (body == null) throw new IllegalArgumentException("请填写盲盒信息");
        String boxTitle = str(body.get("boxTitle"));
        if (boxTitle.isEmpty() || boxTitle.length() > 40) {
            throw new IllegalArgumentException("盲盒标题需填写，且不超过 40 字");
        }
        String boxDesc = str(body.get("boxDesc"));
        if (boxDesc.length() > 200) {
            throw new IllegalArgumentException("盲盒描述不能超过 200 字");
        }
        Double price = toDouble(body.get("price"));
        Double budget = toDouble(wish.get("budget"));
        if (price == null || price <= 0) {
            throw new IllegalArgumentException("请填写合理的价格");
        }
        if (budget != null && price > budget) {
            throw new IllegalArgumentException("价格不能高于买家预算 ¥" + budget);
        }

        int updated = blindBoxMapper.claim(id, userId, boxTitle, boxDesc, price);
        if (updated == 0) {
            throw new IllegalStateException("手慢了，这个许愿已被其他同学认领");
        }
        Map<String, Object> result = blindBoxMapper.findById(id);
        if (result != null) decorate(result, userId);
        return Result.success(result);
    }

    @PostMapping("/{id}/confirm")
    public Result<Map<String, Object>> confirm(@PathVariable Integer id) {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return Result.error("未登录");

        int updated = blindBoxMapper.confirm(id, userId);
        if (updated == 0) throw new IllegalStateException("当前状态无法确认收货");
        Map<String, Object> result = blindBoxMapper.findById(id);
        if (result != null) decorate(result, userId);
        return Result.success(result);
    }

    @PostMapping("/{id}/cancel")
    public Result<Map<String, Object>> cancel(@PathVariable Integer id) {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return Result.error("未登录");

        int updated = blindBoxMapper.cancel(id, userId);
        if (updated == 0) throw new IllegalStateException("只有待认领的许愿可以取消");
        Map<String, Object> result = blindBoxMapper.findById(id);
        if (result != null) decorate(result, userId);
        return Result.success(result);
    }

    /** 补充状态文案与前端按钮判定 */
    private void decorate(Map<String, Object> item, Integer userId) {
        if (item == null) return;
        String status = str(item.get("status"));
        Integer buyerId = toInt(item.get("buyerId"));
        boolean mine = userId != null && userId.equals(buyerId);
        item.put("statusLabel", STATUS_LABELS.getOrDefault(status, status));
        item.put("mine", mine);
        item.put("canClaim", "open".equals(status) && !mine);
        item.put("canConfirm", "matched".equals(status) && mine);
        item.put("canCancel", "open".equals(status) && mine);
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
