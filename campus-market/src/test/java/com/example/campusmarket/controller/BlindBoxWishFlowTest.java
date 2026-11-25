package com.example.campusmarket.controller;

import com.example.campusmarket.common.Result;
import com.example.campusmarket.mapper.BlindBoxMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 盲盒许愿流转回归：open 待认领 → matched 已打包待确认 → completed 已完成 / cancelled 已取消。
 * 重点验证条件更新（认领失败返回 0 视为被别人抢走）与状态文案、按钮判定。
 */
@ExtendWith(MockitoExtension.class)
class BlindBoxWishFlowTest {

    private static final Integer BUYER_ID = 8;
    private static final Integer SELLER_ID = 9;
    private static final Integer WISH_ID = 40;

    @Mock
    private BlindBoxMapper blindBoxMapper;

    @InjectMocks
    private BlindBoxController blindBoxController;

    @BeforeEach
    void loginAsBuyer() {
        login(BUYER_ID);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------------- 许愿 ----------------

    @Test
    @DisplayName("许愿成功：写入许愿并按预算展示最新一条")
    void wish_persistsAndReturnsLatest() {
        when(blindBoxMapper.findLatestByBuyer(BUYER_ID)).thenReturn(wish("open", BUYER_ID));

        Result<Map<String, Object>> result = blindBoxController.wish(wishBody("书籍", 50.0, "想要考研资料"));

        assertEquals(200, result.getCode());
        verify(blindBoxMapper).insertWish(BUYER_ID, "书籍", 50.0, "想要考研资料");
        // 自己发布的许愿不允许自己认领
        assertFalse(Boolean.TRUE.equals(result.getData().get("canClaim")));
        assertTrue(Boolean.TRUE.equals(result.getData().get("canCancel")));
    }

    @Test
    @DisplayName("许愿失败：未登录直接返回错误，不写库")
    void wish_rejectsAnonymous() {
        SecurityContextHolder.clearContext();

        Result<Map<String, Object>> result = blindBoxController.wish(wishBody("书籍", 50.0, "想要考研资料"));

        assertEquals(400, result.getCode());
        verify(blindBoxMapper, never()).insertWish(any(), anyString(), anyDouble(), anyString());
    }

    @Test
    @DisplayName("许愿失败：品类不在允许列表内")
    void wish_rejectsUnknownCategory() {
        assertThrows(IllegalArgumentException.class,
                () -> blindBoxController.wish(wishBody("汽车", 50.0, "随便")));

        verify(blindBoxMapper, never()).insertWish(any(), anyString(), anyDouble(), anyString());
    }

    @Test
    @DisplayName("许愿失败：预算低于 5 元或高于 200 元")
    void wish_rejectsBudgetOutOfRange() {
        assertThrows(IllegalArgumentException.class,
                () -> blindBoxController.wish(wishBody("书籍", 4.9, "太便宜")));
        assertThrows(IllegalArgumentException.class,
                () -> blindBoxController.wish(wishBody("书籍", 200.1, "太贵")));

        verify(blindBoxMapper, never()).insertWish(any(), anyString(), anyDouble(), anyString());
    }

    @Test
    @DisplayName("许愿失败：说明超过 100 字")
    void wish_rejectsTooLongNote() {
        assertThrows(IllegalArgumentException.class,
                () -> blindBoxController.wish(wishBody("书籍", 50.0, "字".repeat(101))));

        verify(blindBoxMapper, never()).insertWish(any(), anyString(), anyDouble(), anyString());
    }

    // ---------------- 认领并打包 ----------------

    @Test
    @DisplayName("认领成功：条件更新成功，许愿进入已打包待确认")
    void claim_succeedsAndMarksMatched() {
        login(SELLER_ID);
        when(blindBoxMapper.findById(WISH_ID)).thenReturn(wish("open", BUYER_ID), wish("matched", BUYER_ID));
        when(blindBoxMapper.claim(WISH_ID, SELLER_ID, "一箱旧书", "随机 5 本", 30.0)).thenReturn(1);

        Result<Map<String, Object>> result = blindBoxController.claim(WISH_ID, claimBody("一箱旧书", "随机 5 本", 30.0));

        assertEquals(200, result.getCode());
        verify(blindBoxMapper).claim(WISH_ID, SELLER_ID, "一箱旧书", "随机 5 本", 30.0);
        assertEquals("已打包待确认", result.getData().get("statusLabel"));
    }

    @Test
    @DisplayName("认领失败：不能认领自己发布的许愿")
    void claim_rejectsOwnWish() {
        when(blindBoxMapper.findById(WISH_ID)).thenReturn(wish("open", BUYER_ID));

        assertThrows(IllegalArgumentException.class,
                () -> blindBoxController.claim(WISH_ID, claimBody("一箱旧书", "随机 5 本", 30.0)));

        verify(blindBoxMapper, never()).claim(any(), any(), anyString(), anyString(), anyDouble());
    }

    @Test
    @DisplayName("认领失败：条件更新返回 0（已被他人抢先认领）")
    void claim_rejectsConcurrentClaim() {
        login(SELLER_ID);
        when(blindBoxMapper.findById(WISH_ID)).thenReturn(wish("open", BUYER_ID));
        when(blindBoxMapper.claim(WISH_ID, SELLER_ID, "一箱旧书", "随机 5 本", 30.0)).thenReturn(0);

        assertThrows(IllegalStateException.class,
                () -> blindBoxController.claim(WISH_ID, claimBody("一箱旧书", "随机 5 本", 30.0)));

        // 失败时不得回读并展示成功数据
        verify(blindBoxMapper).claim(any(), any(), anyString(), anyString(), anyDouble());
    }

    @Test
    @DisplayName("认领失败：报价高于买家预算")
    void claim_rejectsPriceOverBudget() {
        login(SELLER_ID);
        Map<String, Object> wish = wish("open", BUYER_ID);
        wish.put("budget", 20.0);
        when(blindBoxMapper.findById(WISH_ID)).thenReturn(wish);

        assertThrows(IllegalArgumentException.class,
                () -> blindBoxController.claim(WISH_ID, claimBody("一箱旧书", "随机 5 本", 30.0)));

        verify(blindBoxMapper, never()).claim(any(), any(), anyString(), anyString(), anyDouble());
    }

    @Test
    @DisplayName("认领失败：盲盒标题为空或超过 40 字")
    void claim_rejectsInvalidTitle() {
        login(SELLER_ID);
        when(blindBoxMapper.findById(WISH_ID)).thenReturn(wish("open", BUYER_ID));

        assertThrows(IllegalArgumentException.class,
                () -> blindBoxController.claim(WISH_ID, claimBody("", "", 30.0)));
        assertThrows(IllegalArgumentException.class,
                () -> blindBoxController.claim(WISH_ID, claimBody("标".repeat(41), "", 30.0)));

        verify(blindBoxMapper, never()).claim(any(), any(), anyString(), anyString(), anyDouble());
    }

    // ---------------- 确认收货与取消 ----------------

    @Test
    @DisplayName("确认收货：条件更新失败（状态不是待确认）时报错")
    void confirm_rejectsWhenStatusChanged() {
        when(blindBoxMapper.confirm(WISH_ID, BUYER_ID)).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> blindBoxController.confirm(WISH_ID));
    }

    @Test
    @DisplayName("取消许愿：条件更新失败（已被认领/已完成）时报错")
    void cancel_rejectsWhenStatusChanged() {
        when(blindBoxMapper.cancel(WISH_ID, BUYER_ID)).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> blindBoxController.cancel(WISH_ID));
    }

    @Test
    @DisplayName("取消许愿：自己待认领的许愿可以取消")
    void cancel_succeedsForOwnOpenWish() {
        when(blindBoxMapper.cancel(WISH_ID, BUYER_ID)).thenReturn(1);
        when(blindBoxMapper.findById(WISH_ID)).thenReturn(wish("cancelled", BUYER_ID));

        Result<Map<String, Object>> result = blindBoxController.cancel(WISH_ID);

        assertEquals(200, result.getCode());
        assertEquals("已取消", result.getData().get("statusLabel"));
        assertFalse(Boolean.TRUE.equals(result.getData().get("canClaim")));
    }

    // ---------------- 列表状态装饰 ----------------

    @Test
    @DisplayName("列表：别人的待认领许愿可认领，自己的待认领许愿可取消")
    void list_decoratesActionFlags() {
        Map<String, Object> others = wish("open", SELLER_ID);
        Map<String, Object> mine = wish("open", BUYER_ID);
        List<Map<String, Object>> list = new ArrayList<>(List.of(others, mine));
        when(blindBoxMapper.findOpen(BUYER_ID)).thenReturn(list);
        when(blindBoxMapper.countByScope(BUYER_ID)).thenReturn(counts(1, 1, 0));

        Result<Map<String, Object>> result = blindBoxController.list("open");

        assertEquals(200, result.getCode());
        assertTrue(Boolean.TRUE.equals(others.get("canClaim")));
        assertFalse(Boolean.TRUE.equals(mine.get("canClaim")));
        assertTrue(Boolean.TRUE.equals(mine.get("canCancel")));
        Map<String, Object> data = result.getData();
        @SuppressWarnings("unchecked")
        Map<String, Object> countResult = (Map<String, Object>) data.get("counts");
        assertEquals(1, countResult.get("open"));
        assertEquals(1, countResult.get("mine"));
    }

    // ---------------- 测试数据构造 ----------------

    private void login(Integer userId) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(String.valueOf(userId), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Map<String, Object> wishBody(String category, Double budget, String note) {
        Map<String, Object> body = new HashMap<>();
        body.put("category", category);
        body.put("budget", budget);
        body.put("wishNote", note);
        return body;
    }

    private Map<String, Object> claimBody(String title, String desc, Double price) {
        Map<String, Object> body = new HashMap<>();
        body.put("boxTitle", title);
        body.put("boxDesc", desc);
        body.put("price", price);
        return body;
    }

    private Map<String, Object> wish(String status, Integer buyerId) {
        Map<String, Object> wish = new HashMap<>();
        wish.put("id", WISH_ID);
        wish.put("buyerId", buyerId);
        wish.put("category", "书籍");
        wish.put("budget", 50.0);
        wish.put("price", 30.0);
        wish.put("wishNote", "想要考研资料");
        wish.put("status", status);
        return wish;
    }

    private Map<String, Object> counts(int open, int mine, int claimed) {
        Map<String, Object> counts = new HashMap<>();
        counts.put("openCount", open);
        counts.put("mineCount", mine);
        counts.put("claimedCount", claimed);
        return counts;
    }
}
