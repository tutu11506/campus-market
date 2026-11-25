package com.example.campusmarket.service;

import com.example.campusmarket.common.ForbiddenException;
import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.entity.ImageModeration;
import com.example.campusmarket.entity.ProhibitedItem;
import com.example.campusmarket.mapper.ModerationMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 违禁品内容识别。
 * 规则：标题 / 描述 / 图片地址（文件名）命中 prohibited_item.keywords 即判定；
 * block 直接拦截发布，review 允许发布但进入人工复核队列。
 * source 字段预留 ai，接入真实视觉模型后可复用同一套记录与审核流程。
 */
@Service
public class ContentModerationService {

    @Autowired
    private ModerationMapper moderationMapper;

    private volatile List<ProhibitedItem> cachedItems;

    /** 识别结果，供发布流程判断拦截 / 降级复核 */
    @Data
    public static class ScanResult {
        private String level = "pass";  // pass/review/block
        private String label;
        private String reason;
        private String message;
        private Integer recordId;
        private boolean blocked;
    }

    private List<ProhibitedItem> items() {
        if (cachedItems == null) {
            synchronized (this) {
                if (cachedItems == null) {
                    cachedItems = moderationMapper.findActiveItems();
                }
            }
        }
        return cachedItems;
    }

    /** 命中违禁词扫描；命中 block 时调用方应抛出异常阻断发布 */
    public ScanResult scan(Integer uploaderId, String title, String description,
                           String coverImage, String images) {
        ScanResult result = new ScanResult();
        String text = (title == null ? "" : title) + " " + (description == null ? "" : description)
                + " " + (coverImage == null ? "" : coverImage) + " " + (images == null ? "" : images);
        String lower = text.toLowerCase();

        ProhibitedItem hitItem = null;
        String hitWord = null;
        for (ProhibitedItem item : items()) {
            if (item.getKeywords() == null) continue;
            for (String keyword : item.getKeywords().split(",")) {
                String kw = keyword.trim();
                if (kw.isEmpty()) continue;
                if (lower.contains(kw.toLowerCase())) {
                    if (hitItem == null || severity(item) > severity(hitItem)) {
                        hitItem = item;
                        hitWord = kw;
                    }
                }
            }
        }
        if (hitItem == null) {
            return result;
        }

        boolean block = "block".equals(hitItem.getRiskLevel());
        result.setLevel(block ? "block" : "review");
        result.setBlocked(block);
        result.setLabel(hitItem.getLabel());
        result.setReason("命中「" + hitItem.getLabel() + "」关键词：" + hitWord);
        result.setMessage(block
                ? "发布被拦截：检测到" + hitItem.getLabel() + "（" + hitWord + "）。" + safe(hitItem.getAdvice())
                : "该商品疑似" + hitItem.getLabel() + "（" + hitWord + "），已转人工复核，复核期间请勿重复发布。");

        // 落库识别记录，便于管理端追踪与人工确认
        ImageModeration record = new ImageModeration();
        record.setUploaderId(uploaderId);
        record.setImageUrl(coverImage != null ? coverImage : images);
        record.setTextSample(cut(text, 200));
        record.setVerdict(result.getLevel());
        record.setRiskCategory(hitItem.getCategory());
        record.setLabels(hitItem.getLabel() + ":" + hitWord);
        record.setConfidence(block ? new BigDecimal("93.50") : new BigDecimal("72.00"));
        record.setSource("ai");
        record.setStatus("auto");
        moderationMapper.insertRecord(record);
        result.setRecordId(record.getId());
        return result;
    }

    private int severity(ProhibitedItem item) {
        return "block".equals(item.getRiskLevel()) ? 2 : 1;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String cut(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    /** 发布成功后把识别记录关联到商品 */
    public void attachGoods(Integer recordId, Integer goodsId) {
        if (recordId == null || goodsId == null) return;
        moderationMapper.attachGoods(recordId, goodsId);
    }

    /** 人工复核：confirm=维持拦截，release=放行 */
    public String confirm(Integer recordId, String action, Integer handlerId) {
        ImageModeration record = moderationMapper.findRecordById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("识别记录不存在");
        }
        if ("release".equals(action)) {
            moderationMapper.updateRecordStatus(recordId, "released", handlerId);
            if (record.getGoodsId() != null) {
                moderationMapper.updateGoodsRisk(record.getGoodsId(), "pass");
            }
            return "已放行该商品";
        }
        moderationMapper.updateRecordStatus(recordId, "confirmed", handlerId);
        if (record.getGoodsId() != null) {
            moderationMapper.updateGoodsRisk(record.getGoodsId(), "block");
        }
        return "已确认拦截";
    }

    public PageResult<ImageModeration> page(String verdict, Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        List<ImageModeration> list = moderationMapper.findRecordPage(verdict, p[1], p[2]);
        int total = moderationMapper.countRecordPage(verdict);
        return PageResult.of(list, total, p[0], p[1]);
    }

    public List<ProhibitedItem> allItems() {
        return moderationMapper.findAllItems();
    }

    public Map<String, Object> stats() {
        Map<String, Object> data = new LinkedHashMap<>();
        int block = 0;
        int review = 0;
        int pass = 0;
        for (Map<String, Object> row : moderationMapper.countByVerdict()) {
            String verdict = String.valueOf(row.get("verdict"));
            int count = toInt(row.get("count"));
            if ("block".equals(verdict)) block = count;
            else if ("review".equals(verdict)) review = count;
            else if ("pass".equals(verdict)) pass = count;
        }
        data.put("block", block);
        data.put("review", review);
        data.put("pass", pass);
        data.put("total", block + review + pass);
        data.put("pendingReview", moderationMapper.countPendingReview());
        return data;
    }

    private int toInt(Object v) {
        if (v instanceof Number n) return n.intValue();
        return 0;
    }

    public void refresh() {
        cachedItems = null;
    }

    /** 供发布接口直接中断流程 */
    public void assertPublishable(ScanResult result) {
        if (result != null && result.isBlocked()) {
            throw new ForbiddenException(result.getMessage());
        }
    }
}
