package com.example.campusmarket.controller;

import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.mapper.AlumniMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 校友标签：给同学打上「考研党 / 新生 / 毕业清仓 / 摄影爱好者」等标签，
 * 按标签筛选同学与他们的在售闲置，快速找到同需求、同频道的人。
 */
@RestController
@RequestMapping("/alumni")
public class AlumniTagController {

    /** 预设标签 + 一句使用场景说明（顺序即页面展示顺序） */
    private static final Map<String, String> TAG_TIPS = new LinkedHashMap<>();

    static {
        TAG_TIPS.put("考研党", "自习搭子、资料互换");
        TAG_TIPS.put("新生", "刚到学校，置办生活用品");
        TAG_TIPS.put("毕业清仓", "学长学姐急出闲置");
        TAG_TIPS.put("摄影爱好者", "器材交流、约拍互助");
        TAG_TIPS.put("健身搭子", "操场夜跑、健身房结伴");
        TAG_TIPS.put("编程开发", "项目组队、技术互助");
        TAG_TIPS.put("社团达人", "招新、活动合作");
        TAG_TIPS.put("考公党", "备考资料、经验交流");
        TAG_TIPS.put("乐器玩家", "二手乐器、排练搭子");
        TAG_TIPS.put("留学生", "生活互助、语言交换");
    }

    private static final int MAX_TAGS = 5;

    @Autowired
    private AlumniMapper alumniMapper;

    /**
     * 全部预设标签 + 每个标签的同学数 / 在售闲置数 + 我的标签。
     */
    @GetMapping("/tags")
    public Result<Map<String, Object>> tags() {
        Map<String, Integer> userCounts = toCountMap(alumniMapper.countUsersByTag(), "userCount");
        Map<String, Integer> goodsCounts = toCountMap(alumniMapper.countGoodsByTag(), "goodsCount");

        List<Map<String, Object>> tags = new ArrayList<>();
        for (Map.Entry<String, String> e : TAG_TIPS.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("tag", e.getKey());
            item.put("tip", e.getValue());
            item.put("userCount", userCounts.getOrDefault(e.getKey(), 0));
            item.put("goodsCount", goodsCounts.getOrDefault(e.getKey(), 0));
            tags.add(item);
        }

        Integer userId = SecurityUtils.getCurrentUserId();
        List<String> myTags = userId == null ? List.of() : alumniMapper.findMyTags(userId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tags", tags);
        data.put("myTags", myTags);
        data.put("maxTags", MAX_TAGS);
        data.put("hasData", !tags.isEmpty());
        return Result.success(data);
    }

    /**
     * 某个标签下的同学 + 在售闲置。
     */
    @GetMapping("/users")
    public Result<Map<String, Object>> users(@RequestParam String tag) {
        String name = tag == null ? "" : tag.trim();
        if (!TAG_TIPS.containsKey(name)) {
            throw new IllegalArgumentException("不支持的标签：" + name);
        }

        Map<String, Integer> userCounts = toCountMap(alumniMapper.countUsersByTag(), "userCount");
        Map<String, Integer> goodsCounts = toCountMap(alumniMapper.countGoodsByTag(), "goodsCount");

        List<Map<String, Object>> users = new ArrayList<>();
        for (Map<String, Object> row : alumniMapper.findUsersByTag(name, 24)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("username", row.get("username"));
            item.put("avatar", row.get("avatar"));
            item.put("studentNo", row.get("studentNo"));
            item.put("credit", row.get("credit"));
            item.put("onSaleCount", toInt(row.get("onSaleCount")));
            item.put("tags", splitTags(row.get("tagStr")));
            users.add(item);
        }

        List<Map<String, Object>> goods = new ArrayList<>();
        for (Map<String, Object> row : alumniMapper.findGoodsByTag(name, 24)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("title", row.get("title"));
            item.put("price", row.get("price"));
            item.put("coverImage", row.get("coverImage"));
            item.put("viewCount", toInt(row.get("viewCount")));
            item.put("sellerId", row.get("sellerId"));
            item.put("sellerName", row.get("sellerName"));
            item.put("sellerTags", splitTags(row.get("sellerTags")));
            goods.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tag", name);
        data.put("tip", TAG_TIPS.get(name));
        data.put("userCount", userCounts.getOrDefault(name, 0));
        data.put("goodsCount", goodsCounts.getOrDefault(name, 0));
        data.put("users", users);
        data.put("goods", goods);
        data.put("hasData", !users.isEmpty());
        return Result.success(data);
    }

    /**
     * 保存我的标签：只允许预设标签，最多 5 个，重复提交直接覆盖。
     */
    @PutMapping("/my-tags")
    public Result<List<String>> saveMyTags(@RequestBody Map<String, Object> body) {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return Result.error("未登录");

        Set<String> saved = new LinkedHashSet<>();
        Object raw = body == null ? null : body.get("tags");
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                String tag = o == null ? "" : String.valueOf(o).trim();
                if (tag.isEmpty()) continue;
                if (!TAG_TIPS.containsKey(tag)) {
                    throw new IllegalArgumentException("不支持的标签：" + tag);
                }
                saved.add(tag);
            }
        }
        if (saved.size() > MAX_TAGS) {
            throw new IllegalArgumentException("最多选择 " + MAX_TAGS + " 个标签");
        }

        alumniMapper.deleteByUser(userId);
        for (String tag : saved) {
            alumniMapper.insertTag(userId, tag);
        }
        return Result.success(new ArrayList<>(saved));
    }

    /** 把 GROUP_CONCAT 的逗号串拆成标签列表 */
    private List<String> splitTags(Object value) {
        if (value == null) return List.of();
        return Arrays.stream(String.valueOf(value).split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private Map<String, Integer> toCountMap(List<Map<String, Object>> rows, String key) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String tag = row.get("tag") == null ? "" : String.valueOf(row.get("tag"));
            if (!tag.isEmpty()) map.put(tag, toInt(row.get(key)));
        }
        return map;
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
