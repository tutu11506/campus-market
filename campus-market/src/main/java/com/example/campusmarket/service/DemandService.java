package com.example.campusmarket.service;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.entity.DemandPost;
import com.example.campusmarket.entity.DemandQuote;
import com.example.campusmarket.mapper.DemandMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 求购专区：学生发求购，卖家接单报价，系统按关键词匹配在售商品。
 */
@Service
public class DemandService {

    private static final Set<String> CATEGORIES = Set.of(
            "textbook", "vehicle", "furniture", "digital", "dorm", "other");

    @Autowired
    private DemandMapper demandMapper;

    public PageResult<DemandPost> list(String category, String keyword, Integer mine,
                                       int pageNum, int pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 30);
        List<DemandPost> list = demandMapper.findPosts(category, keyword, mine, p[2], p[1]);
        fillMatchCount(list);
        int total = demandMapper.countPosts(category, keyword, mine);
        return PageResult.of(list, total, p[0], p[1]);
    }

    /** 求购详情：求购信息 + 报价列表 + 系统匹配商品 + 我的报价 */
    public Map<String, Object> detail(Integer id, Integer uid) {
        DemandPost post = demandMapper.findById(id);
        if (post == null) {
            throw new IllegalArgumentException("求购不存在或已删除");
        }
        post.setMatchCount(demandMapper.countMatchGoods(matchKeyword(post)));
        List<DemandQuote> quotes = demandMapper.findQuotes(id);
        List<Map<String, Object>> matches = demandMapper.matchGoods(matchKeyword(post), 6);
        DemandQuote myQuote = uid == null ? null : demandMapper.findMyQuote(id, uid);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("demand", post);
        data.put("quotes", quotes);
        data.put("matches", matches);
        data.put("myQuote", myQuote);
        return data;
    }

    public DemandPost create(Integer uid, DemandPost form) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        String title = form.getTitle() == null ? "" : form.getTitle().trim();
        if (title.length() < 2 || title.length() > 100) {
            throw new IllegalArgumentException("标题需要 2-100 个字");
        }
        String keyword = form.getKeyword() == null ? "" : form.getKeyword().trim();
        if (keyword.length() < 2 || keyword.length() > 50) {
            throw new IllegalArgumentException("请填写想收的关键词（2-50 字），用于匹配在售商品");
        }
        String category = form.getCategory() == null ? "other" : form.getCategory();
        if (!CATEGORIES.contains(category)) {
            category = "other";
        }
        String description = form.getDescription() == null ? null : form.getDescription().trim();
        if (description != null && description.length() > 1000) {
            throw new IllegalArgumentException("描述最多 1000 字");
        }
        BigDecimal min = form.getBudgetMin();
        BigDecimal max = form.getBudgetMax();
        if (min != null && min.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("预算不能为负数");
        }
        if (max != null && max.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("预算不能为负数");
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("最低预算不能高于最高预算");
        }
        String major = trimToNull(form.getTargetMajor(), 50, "定向专业最多 50 字");
        String grade = trimToNull(form.getTargetGrade(), 20, "定向年级最多 20 字");

        DemandPost post = new DemandPost();
        post.setUserId(uid);
        post.setTitle(title);
        post.setCategory(category);
        post.setKeyword(keyword);
        post.setDescription(description);
        post.setBudgetMin(min);
        post.setBudgetMax(max);
        post.setTargetMajor(major);
        post.setTargetGrade(grade);
        demandMapper.insertPost(post);
        DemandPost created = demandMapper.findById(post.getId());
        created.setMatchCount(demandMapper.countMatchGoods(keyword));
        return created;
    }

    /** 卖家报价 */
    public DemandQuote quote(Integer uid, Integer demandId, BigDecimal price, String message) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        DemandPost post = demandMapper.findById(demandId);
        if (post == null) {
            throw new IllegalArgumentException("求购不存在或已删除");
        }
        if (uid.equals(post.getUserId())) {
            throw new IllegalArgumentException("不能给自己的求购报价");
        }
        if (!"open".equals(post.getStatus())) {
            throw new IllegalArgumentException("该求购已结束，无法报价");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("请填写有效报价");
        }
        String msg = message == null ? null : message.trim();
        if (msg != null && msg.length() > 500) {
            throw new IllegalArgumentException("留言最多 500 字");
        }
        if (demandMapper.findMyQuote(demandId, uid) != null) {
            throw new IllegalArgumentException("你已经报过价，等待对方选择即可");
        }
        DemandQuote quote = new DemandQuote();
        quote.setDemandId(demandId);
        quote.setSellerId(uid);
        quote.setPrice(price);
        quote.setMessage(msg);
        demandMapper.insertQuote(quote);
        demandMapper.addQuoteCount(demandId);
        return demandMapper.findMyQuote(demandId, uid);
    }

    /** 求购发布者选定报价 */
    public String accept(Integer uid, Integer quoteId) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        DemandQuote quote = demandMapper.findQuoteById(quoteId);
        if (quote == null) {
            throw new IllegalArgumentException("报价不存在");
        }
        DemandPost post = demandMapper.findById(quote.getDemandId());
        if (post == null) {
            throw new IllegalArgumentException("求购不存在或已删除");
        }
        if (!uid.equals(post.getUserId())) {
            throw new IllegalArgumentException("只有求购发布者可以选择报价");
        }
        if (!"open".equals(post.getStatus())) {
            throw new IllegalArgumentException("该求购已结束");
        }
        demandMapper.updateQuoteStatus(quoteId, "accepted");
        demandMapper.rejectOthers(post.getId(), quoteId);
        demandMapper.updateStatus(post.getId(), "matched", quote.getPrice());
        return "已选定该报价，求购成交";
    }

    /** 关闭求购 */
    public String close(Integer uid, Integer id) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        DemandPost post = demandMapper.findById(id);
        if (post == null) {
            throw new IllegalArgumentException("求购不存在或已删除");
        }
        if (!uid.equals(post.getUserId())) {
            throw new IllegalArgumentException("只有发布者可以关闭求购");
        }
        demandMapper.updateStatus(id, "closed", post.getDealPrice());
        return "已关闭求购";
    }

    /** 列表页批量回填匹配件数：整页关键词只查一次，替代逐条统计 */
    private void fillMatchCount(List<DemandPost> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<String> keywords = list.stream()
                .map(this::matchKeyword)
                .distinct()
                .toList();
        Map<String, Integer> counts = new HashMap<>();
        Map<String, Object> row = demandMapper.countMatchGoodsBatch(keywords);
        for (int i = 0; i < keywords.size(); i++) {
            Object cnt = row == null ? null : row.get("c" + i);
            counts.put(keywords.get(i), cnt instanceof Number n ? n.intValue() : 0);
        }
        for (DemandPost d : list) {
            d.setMatchCount(counts.getOrDefault(matchKeyword(d), 0));
        }
    }

    private String matchKeyword(DemandPost post) {
        String keyword = post.getKeyword();
        if (keyword == null || keyword.trim().isEmpty()) {
            keyword = post.getTitle();
        }
        return keyword.trim();
    }

    private String trimToNull(String value, int max, String message) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return null;
        }
        if (v.length() > max) {
            throw new IllegalArgumentException(message);
        }
        return v;
    }
}
