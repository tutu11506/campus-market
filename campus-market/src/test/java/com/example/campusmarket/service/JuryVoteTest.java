package com.example.campusmarket.service;

import com.example.campusmarket.common.ForbiddenException;
import com.example.campusmarket.entity.JuryReview;
import com.example.campusmarket.entity.Order;
import com.example.campusmarket.mapper.JuryMapper;
import com.example.campusmarket.mapper.OrderMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 评审团回归：评价提交、重复评价拦截、当事人回避、投票阈值（至少 3 票且支持率严格大于 60%）。
 */
@ExtendWith(MockitoExtension.class)
class JuryVoteTest {

    private static final Integer ORDER_ID = 50;
    private static final Integer REVIEW_ID = 60;
    private static final Integer BUYER_ID = 11;
    private static final Integer SELLER_ID = 12;
    private static final Integer VOTER_ID = 13;

    @Mock
    private JuryMapper juryMapper;
    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private JuryService juryService;

    // ---------------- 提交评价 ----------------

    @Test
    @DisplayName("评价成功：买家评价卖家，角色与被评价人由服务端判定")
    void create_asBuyerTargetsSeller() {
        when(orderMapper.findById(ORDER_ID)).thenReturn(order("completed"));
        when(juryMapper.findByOrderAndReviewer(ORDER_ID, BUYER_ID)).thenReturn(null);
        when(juryMapper.insert(any(JuryReview.class))).thenAnswer(invocation -> {
            JuryReview review = invocation.getArgument(0);
            review.setId(REVIEW_ID);
            return 1;
        });

        Map<String, Object> data = juryService.create(BUYER_ID, ORDER_ID, 4, "描述一致，发货快", null);

        ArgumentCaptor<JuryReview> captor = ArgumentCaptor.forClass(JuryReview.class);
        verify(juryMapper).insert(captor.capture());
        JuryReview saved = captor.getValue();
        assertEquals(SELLER_ID, saved.getRevieweeId());
        assertEquals("buyer", saved.getReviewerRole());
        assertEquals("pending", saved.getStatus());
        assertEquals(REVIEW_ID, data.get("id"));
    }

    @Test
    @DisplayName("评价失败：非交易当事人无权评价")
    void create_rejectsStranger() {
        when(orderMapper.findById(ORDER_ID)).thenReturn(order("completed"));

        assertThrows(ForbiddenException.class,
                () -> juryService.create(VOTER_ID, ORDER_ID, 5, "和我没关系", null));

        verify(juryMapper, never()).insert(any());
    }

    @Test
    @DisplayName("评价失败：订单未完成不能评价")
    void create_rejectsUnfinishedOrder() {
        when(orderMapper.findById(ORDER_ID)).thenReturn(order("dealing"));

        assertThrows(IllegalArgumentException.class,
                () -> juryService.create(BUYER_ID, ORDER_ID, 5, "还没交易完", null));

        verify(juryMapper, never()).insert(any());
    }

    @Test
    @DisplayName("评价失败：同一订单重复评价被拦截")
    void create_rejectsDuplicateReview() {
        when(orderMapper.findById(ORDER_ID)).thenReturn(order("completed"));
        when(juryMapper.findByOrderAndReviewer(ORDER_ID, BUYER_ID)).thenReturn(new JuryReview());

        assertThrows(IllegalArgumentException.class,
                () -> juryService.create(BUYER_ID, ORDER_ID, 5, "再评一次", null));

        verify(juryMapper, never()).insert(any());
    }

    @Test
    @DisplayName("评价失败：星级越界、理由为空或超过 500 字")
    void create_rejectsInvalidRatingAndReason() {
        when(orderMapper.findById(ORDER_ID)).thenReturn(order("completed"));

        assertThrows(IllegalArgumentException.class,
                () -> juryService.create(BUYER_ID, ORDER_ID, 6, "星级越界", null));
        assertThrows(IllegalArgumentException.class,
                () -> juryService.create(BUYER_ID, ORDER_ID, 3, "   ", null));
        assertThrows(IllegalArgumentException.class,
                () -> juryService.create(BUYER_ID, ORDER_ID, 3, "字".repeat(501), null));

        verify(juryMapper, never()).insert(any());
    }

    @Test
    @DisplayName("评价失败：证据图片超过 3 张")
    void create_rejectsTooManyEvidence() {
        when(orderMapper.findById(ORDER_ID)).thenReturn(order("completed"));

        assertThrows(IllegalArgumentException.class,
                () -> juryService.create(BUYER_ID, ORDER_ID, 3, "证据太多",
                        List.of("a.jpg", "b.jpg", "c.jpg", "d.jpg")));

        verify(juryMapper, never()).insert(any());
    }

    // ---------------- 投票阈值 ----------------

    @Test
    @DisplayName("投票：支持率 66.67% 且满 3 票，评价公开展示")
    void vote_threeVotesWithMajority_displays() {
        when(juryMapper.findById(REVIEW_ID)).thenReturn(review("pending"));
        when(juryMapper.countDisplayVotes(REVIEW_ID)).thenReturn(2);
        when(juryMapper.countHideVotes(REVIEW_ID)).thenReturn(1);
        when(juryMapper.countVotes(REVIEW_ID)).thenReturn(3);

        Map<String, Object> data = juryService.vote(VOTER_ID, REVIEW_ID, 1);

        assertEquals("displayed", data.get("status"));
        assertEquals(new BigDecimal("66.67"), data.get("approveRate"));
        verify(juryMapper).updateStats(eq(REVIEW_ID), eq(2), eq(1), eq(3),
                eq(new BigDecimal("66.67")), eq("displayed"));
    }

    @Test
    @DisplayName("投票：支持率 33.33% 未过半，评价不展示")
    void vote_threeVotesWithMinority_hides() {
        when(juryMapper.findById(REVIEW_ID)).thenReturn(review("pending"));
        when(juryMapper.countDisplayVotes(REVIEW_ID)).thenReturn(1);
        when(juryMapper.countHideVotes(REVIEW_ID)).thenReturn(2);
        when(juryMapper.countVotes(REVIEW_ID)).thenReturn(3);

        Map<String, Object> data = juryService.vote(VOTER_ID, REVIEW_ID, 0);

        assertEquals("hidden", data.get("status"));
        verify(juryMapper).updateStats(eq(REVIEW_ID), eq(1), eq(2), eq(3), any(), eq("hidden"));
    }

    @Test
    @DisplayName("投票：支持率正好 60% 不算通过（阈值是严格大于 60）")
    void vote_exactlySixtyPercent_hides() {
        when(juryMapper.findById(REVIEW_ID)).thenReturn(review("pending"));
        when(juryMapper.countDisplayVotes(REVIEW_ID)).thenReturn(3);
        when(juryMapper.countHideVotes(REVIEW_ID)).thenReturn(2);
        when(juryMapper.countVotes(REVIEW_ID)).thenReturn(5);

        Map<String, Object> data = juryService.vote(VOTER_ID, REVIEW_ID, 1);

        assertEquals("hidden", data.get("status"));
        assertTrue(data.get("message").toString().contains("未超过 60%"));
    }

    @Test
    @DisplayName("投票：不足 3 票时保持待评审，并提示还差几票")
    void vote_belowMinimumVotes_staysPending() {
        when(juryMapper.findById(REVIEW_ID)).thenReturn(review("pending"));
        when(juryMapper.countDisplayVotes(REVIEW_ID)).thenReturn(1);
        when(juryMapper.countHideVotes(REVIEW_ID)).thenReturn(1);
        when(juryMapper.countVotes(REVIEW_ID)).thenReturn(2);

        Map<String, Object> data = juryService.vote(VOTER_ID, REVIEW_ID, 1);

        assertEquals("pending", data.get("status"));
        assertTrue(data.get("message").toString().contains("还差 1 票"));
        verify(juryMapper).updateStats(eq(REVIEW_ID), anyInt(), anyInt(), eq(2), any(), eq("pending"));
    }

    @Test
    @DisplayName("投票失败：交易当事人不能投票")
    void vote_rejectsReviewer() {
        when(juryMapper.findById(REVIEW_ID)).thenReturn(review("pending"));

        assertThrows(ForbiddenException.class, () -> juryService.vote(BUYER_ID, REVIEW_ID, 1));

        verify(juryMapper, never()).updateStats(any(), anyInt(), anyInt(), anyInt(), any(), anyString());
    }

    @Test
    @DisplayName("投票失败：评审已结束后不能再投")
    void vote_rejectsFinishedReview() {
        when(juryMapper.findById(REVIEW_ID)).thenReturn(review("displayed"));

        assertThrows(IllegalArgumentException.class, () -> juryService.vote(VOTER_ID, REVIEW_ID, 1));

        verify(juryMapper, never()).updateStats(any(), anyInt(), anyInt(), anyInt(), any(), anyString());
    }

    @Test
    @DisplayName("投票失败：票值只允许 0 或 1")
    void vote_rejectsInvalidVoteValue() {
        assertThrows(IllegalArgumentException.class, () -> juryService.vote(VOTER_ID, REVIEW_ID, 2));

        verify(juryMapper, never()).findById(any());
    }

    // ---------------- 测试数据构造 ----------------

    private Order order(String status) {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setOrderNo("ORD202606010001abcd");
        order.setBuyerId(BUYER_ID);
        order.setSellerId(SELLER_ID);
        order.setPrice(new BigDecimal("88.00"));
        order.setStatus(status);
        return order;
    }

    private JuryReview review(String status) {
        JuryReview review = new JuryReview();
        review.setId(REVIEW_ID);
        review.setOrderId(ORDER_ID);
        review.setReviewerId(BUYER_ID);
        review.setRevieweeId(SELLER_ID);
        review.setStatus(status);
        return review;
    }
}
