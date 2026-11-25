package com.example.campusmarket.service;

import com.example.campusmarket.common.ForbiddenException;
import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.entity.JuryReview;
import com.example.campusmarket.entity.Order;
import com.example.campusmarket.mapper.JuryMapper;
import com.example.campusmarket.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 评审团：订单完成后买卖双方互评（打分 + 理由 + 证据），
 * 由其他用户投票判断真实性，支持率超过 60%（至少 3 票）才公开展示。
 */
@Service
public class JuryService {

    /** 进入公投结果所需的最少票数 */
    private static final int MIN_VOTES = 3;
    /** 公开展示所需的最低支持率（百分比，需严格大于） */
    private static final BigDecimal APPROVE_THRESHOLD = new BigDecimal("60");
    /** 评价证据图片上限 */
    private static final int MAX_EVIDENCE = 3;

    @Autowired
    private JuryMapper juryMapper;
    @Autowired
    private OrderMapper orderMapper;

    public PageResult<JuryReview> page(Integer uid, String filter, Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        String f = (filter == null || filter.isEmpty()) ? "displayed" : filter;
        if (!"pending".equals(f) && !"displayed".equals(f) && !"mine".equals(f)) {
            f = "displayed";
        }
        List<JuryReview> list = juryMapper.findPage(f, uid, p[1], p[2]);
        int total = juryMapper.countPage(f, uid);
        return PageResult.of(list, total, p[0], p[1]);
    }

    public List<Map<String, Object>> reviewableOrders(Integer uid) {
        return juryMapper.findReviewableOrders(uid);
    }

    @Transactional
    public Map<String, Object> create(Integer uid, Integer orderId, Integer rating,
                                      String reason, List<String> evidence) {
        if (orderId == null) {
            throw new IllegalArgumentException("请选择要评价的订单");
        }
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        boolean isBuyer = uid.equals(order.getBuyerId());
        boolean isSeller = uid.equals(order.getSellerId());
        if (!isBuyer && !isSeller) {
            throw new ForbiddenException("无权评价该订单");
        }
        if (!"completed".equals(order.getStatus())) {
            throw new IllegalArgumentException("订单完成后才能互相评价");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("请给出 1-5 星的评价");
        }
        String text = reason == null ? "" : reason.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("请填写评价理由");
        }
        if (text.length() > 500) {
            throw new IllegalArgumentException("评价理由最多 500 字");
        }
        if (juryMapper.findByOrderAndReviewer(orderId, uid) != null) {
            throw new IllegalArgumentException("你已经评价过该订单");
        }

        StringBuilder ev = new StringBuilder();
        if (evidence != null) {
            int count = 0;
            for (String url : evidence) {
                if (url == null || url.trim().isEmpty()) continue;
                if (++count > MAX_EVIDENCE) {
                    throw new IllegalArgumentException("证据图片最多 " + MAX_EVIDENCE + " 张");
                }
                if (ev.length() > 0) ev.append(',');
                ev.append(url.trim());
            }
        }
        String evidenceStr = ev.length() == 0 ? null : ev.toString();
        if (evidenceStr != null && evidenceStr.length() > 1000) {
            throw new IllegalArgumentException("证据内容过长，请减少图片数量");
        }

        JuryReview review = new JuryReview();
        review.setOrderId(orderId);
        review.setReviewerId(uid);
        review.setRevieweeId(isBuyer ? order.getSellerId() : order.getBuyerId());
        review.setReviewerRole(isBuyer ? "buyer" : "seller");
        review.setRating(rating);
        review.setReason(text);
        review.setEvidence(evidenceStr);
        review.setStatus("pending");
        juryMapper.insert(review);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", review.getId());
        data.put("status", "pending");
        data.put("message", "评价已提交，等待其他用户投票评审（满 " + MIN_VOTES + " 票且支持率超过 60% 公开展示）");
        return data;
    }

    @Transactional
    public Map<String, Object> vote(Integer uid, Integer reviewId, Integer vote) {
        if (reviewId == null) {
            throw new IllegalArgumentException("缺少评价ID");
        }
        if (vote == null || (vote != 0 && vote != 1)) {
            throw new IllegalArgumentException("请选择支持或反对展示");
        }
        JuryReview review = juryMapper.findById(reviewId);
        if (review == null) {
            throw new IllegalArgumentException("评价不存在");
        }
        if (!"pending".equals(review.getStatus())) {
            throw new IllegalArgumentException("该评价评审已结束，无法继续投票");
        }
        if (uid.equals(review.getReviewerId()) || uid.equals(review.getRevieweeId())) {
            throw new ForbiddenException("交易当事人不能参与本次投票");
        }

        juryMapper.upsertVote(reviewId, uid, vote);

        int displayVotes = juryMapper.countDisplayVotes(reviewId);
        int hideVotes = juryMapper.countHideVotes(reviewId);
        int totalVotes = juryMapper.countVotes(reviewId);
        BigDecimal rate = totalVotes == 0
                ? BigDecimal.ZERO
                : new BigDecimal(displayVotes).multiply(new BigDecimal(100))
                    .divide(new BigDecimal(totalVotes), 2, RoundingMode.HALF_UP);

        String status = "pending";
        if (totalVotes >= MIN_VOTES) {
            status = rate.compareTo(APPROVE_THRESHOLD) > 0 ? "displayed" : "hidden";
        }
        juryMapper.updateStats(reviewId, displayVotes, hideVotes, totalVotes, rate, status);

        String message;
        if ("displayed".equals(status)) {
            message = "投票成功，支持率 " + rate.stripTrailingZeros().toPlainString() + "%，该评价已公开展示";
        } else if ("hidden".equals(status)) {
            message = "投票成功，支持率 " + rate.stripTrailingZeros().toPlainString()
                    + "%，未超过 60%，该评价不予展示";
        } else {
            message = "投票成功，当前支持率 " + rate.stripTrailingZeros().toPlainString()
                    + "%，还差 " + (MIN_VOTES - totalVotes) + " 票出结果";
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("displayVotes", displayVotes);
        data.put("hideVotes", hideVotes);
        data.put("totalVotes", totalVotes);
        data.put("approveRate", rate);
        data.put("status", status);
        data.put("message", message);
        return data;
    }

    public Map<String, Object> summary(Integer uid) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pending", juryMapper.countPending());
        data.put("displayed", juryMapper.countDisplayed());
        data.put("mine", juryMapper.countMine(uid));
        data.put("canReview", juryMapper.countReviewableOrders(uid));
        data.put("minVotes", MIN_VOTES);
        data.put("threshold", 60);
        return data;
    }
}
