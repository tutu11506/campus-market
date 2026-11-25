package com.example.campusmarket.service;

import com.example.campusmarket.common.ForbiddenException;
import com.example.campusmarket.entity.Goods;
import com.example.campusmarket.entity.Order;
import com.example.campusmarket.entity.Refund;
import com.example.campusmarket.mapper.GoodsMapper;
import com.example.campusmarket.mapper.OrderMapper;
import com.example.campusmarket.mapper.RefundMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 退货状态机回归：pending 待审核 → approved 已同意（订单已退货 + 商品重新上架 + 托管退款）
 * 或 rejected 已拒绝（订单仍已完成，仅记录退货被拒）。
 * Mapper 全部 Mock，不依赖真实数据库。
 */
@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    private static final Integer ORDER_ID = 70;
    private static final Integer REFUND_ID = 80;
    private static final Integer GOODS_ID = 23;
    private static final Integer BUYER_ID = 21;
    private static final Integer SELLER_ID = 22;
    private static final Integer STRANGER_ID = 24;

    private static final String PENDING = "pending";
    private static final String APPROVED = "approved";
    private static final String REJECTED = "rejected";

    @Mock
    private RefundMapper refundMapper;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private GoodsMapper goodsMapper;

    @InjectMocks
    private RefundService refundService;

    // ---------------- 买家申请 ----------------

    @Test
    @DisplayName("申请成功：金额/卖家/商品以服务端订单为准，订单标记退货审核中")
    void apply_fillsServerSideFieldsAndMarksReturning() {
        when(orderMapper.findById(ORDER_ID)).thenReturn(order("completed"));
        when(refundMapper.findPendingByOrderId(ORDER_ID)).thenReturn(null);

        Refund form = new Refund();
        form.setOrderId(ORDER_ID);
        form.setReason("  商品有划痕  ");
        form.setDescription("描".repeat(600));
        form.setEvidenceUrls("e".repeat(2100));

        String message = refundService.apply(form, BUYER_ID);

        assertTrue(message.contains("已提交"));
        ArgumentCaptor<Refund> captor = ArgumentCaptor.forClass(Refund.class);
        verify(refundMapper).insert(captor.capture());
        Refund saved = captor.getValue();
        assertTrue(saved.getRefundNo().startsWith("RF"));
        // 前端传入的金额、卖家、商品一律不采信，全部由服务端补齐
        assertEquals(BUYER_ID, saved.getBuyerId());
        assertEquals(SELLER_ID, saved.getSellerId());
        assertEquals(GOODS_ID, saved.getGoodsId());
        assertEquals(new BigDecimal("88.00"), saved.getAmount());
        assertEquals(PENDING, saved.getStatus());
        // 超长文本截断，避免写爆字段
        assertEquals(500, saved.getDescription().length());
        assertEquals(2000, saved.getEvidenceUrls().length());
        verify(orderMapper).updateRefundStatus(ORDER_ID, "returning");
    }

    @Test
    @DisplayName("申请失败：非买家无权申请，不写退货单也不改订单")
    void apply_rejectsNonBuyer() {
        when(orderMapper.findById(ORDER_ID)).thenReturn(order("completed"));

        assertThrows(ForbiddenException.class,
                () -> refundService.apply(form("质量问题"), STRANGER_ID));

        verify(refundMapper, never()).insert(any());
        verify(orderMapper, never()).updateRefundStatus(any(), anyString());
    }

    @Test
    @DisplayName("申请失败：订单未完成不能退货")
    void apply_rejectsUnfinishedOrder() {
        when(orderMapper.findById(ORDER_ID)).thenReturn(order("dealing"));

        String message = refundService.apply(form("质量问题"), BUYER_ID);

        assertTrue(message.contains("仅已完成的订单"));
        verify(refundMapper, never()).insert(any());
        verify(orderMapper, never()).updateRefundStatus(any(), anyString());
    }

    @Test
    @DisplayName("申请失败：已退货订单不能重复申请")
    void apply_rejectsReturnedOrder() {
        when(orderMapper.findById(ORDER_ID)).thenReturn(order("returned"));

        String message = refundService.apply(form("质量问题"), BUYER_ID);

        assertTrue(message.contains("已退货"));
        verify(refundMapper, never()).insert(any());
    }

    @Test
    @DisplayName("申请失败：存在待审核申请时拦截重复提交")
    void apply_rejectsDuplicatePending() {
        when(orderMapper.findById(ORDER_ID)).thenReturn(order("completed"));
        when(refundMapper.findPendingByOrderId(ORDER_ID)).thenReturn(new Refund());

        String message = refundService.apply(form("质量问题"), BUYER_ID);

        assertTrue(message.contains("待审核"));
        verify(refundMapper, never()).insert(any());
        verify(orderMapper, never()).updateRefundStatus(any(), anyString());
    }

    @Test
    @DisplayName("申请失败：未填写退货原因时直接拒绝，不查订单")
    void apply_rejectsBlankReason() {
        String message = refundService.apply(form("   "), BUYER_ID);

        assertTrue(message.contains("退货原因"));
        verify(orderMapper, never()).findById(any());
        verify(refundMapper, never()).insert(any());
    }

    // ---------------- 卖家同意 ----------------

    @Test
    @DisplayName("同意退货：订单置为已退货、托管退款、商品重新上架")
    void approve_marksReturnedAndRelistsGoods() {
        when(refundMapper.findById(REFUND_ID)).thenReturn(refund(PENDING));
        when(goodsMapper.findById(GOODS_ID)).thenReturn(goods("sold", 0));
        when(goodsMapper.updateStatus(GOODS_ID, "on_sale")).thenReturn(1);

        String message = refundService.approve(REFUND_ID, SELLER_ID);

        assertTrue(message.contains("已同意退货"));
        verify(refundMapper).updateAudit(REFUND_ID, APPROVED, SELLER_ID, null);
        verify(orderMapper).updateStatusAndRefundStatus(ORDER_ID, "returned", "returned");
        verify(orderMapper).refundEscrow(ORDER_ID);
        verify(goodsMapper).updateStatus(GOODS_ID, "on_sale");
    }

    @Test
    @DisplayName("同意退货：非卖家无权审核，不做任何写操作")
    void approve_rejectsNonSeller() {
        when(refundMapper.findById(REFUND_ID)).thenReturn(refund(PENDING));

        assertThrows(ForbiddenException.class,
                () -> refundService.approve(REFUND_ID, STRANGER_ID));

        verify(refundMapper, never()).updateAudit(any(), anyString(), any(), any());
        verify(orderMapper, never()).updateStatusAndRefundStatus(any(), anyString(), anyString());
        verify(orderMapper, never()).refundEscrow(any());
    }

    @Test
    @DisplayName("同意退货：已处理过的申请不能重复审核")
    void approve_rejectsProcessedRefund() {
        when(refundMapper.findById(REFUND_ID)).thenReturn(refund(APPROVED));

        String message = refundService.approve(REFUND_ID, SELLER_ID);

        assertTrue(message.contains("已处理"));
        verify(refundMapper, never()).updateAudit(any(), anyString(), any(), any());
        verify(orderMapper, never()).refundEscrow(any());
    }

    @Test
    @DisplayName("同意退货：商品已下架时不强行上架，订单与资金仍正常处理")
    void approve_skipsRelistWhenGoodsNotSold() {
        when(refundMapper.findById(REFUND_ID)).thenReturn(refund(PENDING));
        when(goodsMapper.findById(GOODS_ID)).thenReturn(goods("off_shelf", 0));

        refundService.approve(REFUND_ID, SELLER_ID);

        verify(orderMapper).updateStatusAndRefundStatus(ORDER_ID, "returned", "returned");
        verify(orderMapper).refundEscrow(ORDER_ID);
        verify(goodsMapper, never()).updateStatus(any(), anyString());
    }

    @Test
    @DisplayName("同意退货：商品重新上架失败时抛异常，交给事务回滚")
    void approve_throwsWhenRelistFails() {
        when(refundMapper.findById(REFUND_ID)).thenReturn(refund(PENDING));
        when(goodsMapper.findById(GOODS_ID)).thenReturn(goods("sold", 0));
        when(goodsMapper.updateStatus(GOODS_ID, "on_sale")).thenReturn(0);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> refundService.approve(REFUND_ID, SELLER_ID));

        assertTrue(ex.getMessage().contains("回滚"));
    }

    // ---------------- 卖家拒绝 ----------------

    @Test
    @DisplayName("拒绝退货：订单保持已完成，仅记录退货被拒")
    void reject_marksOrderRefundRejected() {
        when(refundMapper.findById(REFUND_ID)).thenReturn(refund(PENDING));

        String message = refundService.reject(REFUND_ID, SELLER_ID, "与描述一致，划痕为正常使用痕迹");

        assertTrue(message.contains("已拒绝"));
        verify(refundMapper).updateAudit(REFUND_ID, REJECTED, SELLER_ID, "与描述一致，划痕为正常使用痕迹");
        verify(orderMapper).updateRefundStatus(ORDER_ID, "return_rejected");
        verify(orderMapper, never()).updateStatusAndRefundStatus(any(), anyString(), anyString());
        verify(orderMapper, never()).refundEscrow(any());
    }

    @Test
    @DisplayName("拒绝退货：非卖家无权审核")
    void reject_rejectsNonSeller() {
        when(refundMapper.findById(REFUND_ID)).thenReturn(refund(PENDING));

        assertThrows(ForbiddenException.class,
                () -> refundService.reject(REFUND_ID, STRANGER_ID, "不同意"));

        verify(refundMapper, never()).updateAudit(any(), anyString(), any(), any());
        verify(orderMapper, never()).updateRefundStatus(any(), anyString());
    }

    @Test
    @DisplayName("拒绝退货：必须填写拒绝原因")
    void reject_requiresReason() {
        when(refundMapper.findById(REFUND_ID)).thenReturn(refund(PENDING));

        String message = refundService.reject(REFUND_ID, SELLER_ID, "  ");

        assertTrue(message.contains("拒绝原因"));
        verify(refundMapper, never()).updateAudit(any(), anyString(), any(), any());
        verify(orderMapper, never()).updateRefundStatus(any(), anyString());
    }

    @Test
    @DisplayName("拒绝退货：拒绝原因超过 255 字时截断后入库")
    void reject_truncatesTooLongReason() {
        when(refundMapper.findById(REFUND_ID)).thenReturn(refund(PENDING));

        refundService.reject(REFUND_ID, SELLER_ID, "理".repeat(300));

        verify(refundMapper).updateAudit(eq(REFUND_ID), eq(REJECTED), eq(SELLER_ID),
                eq("理".repeat(255)));
        verify(orderMapper).updateRefundStatus(ORDER_ID, "return_rejected");
    }

    // ---------------- 测试数据构造 ----------------

    private Order order(String status) {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setOrderNo("ORD202606010001abcd");
        order.setBuyerId(BUYER_ID);
        order.setSellerId(SELLER_ID);
        order.setGoodsId(GOODS_ID);
        order.setPrice(new BigDecimal("88.00"));
        order.setStatus(status);
        return order;
    }

    private Refund form(String reason) {
        Refund refund = new Refund();
        refund.setOrderId(ORDER_ID);
        refund.setReason(reason);
        return refund;
    }

    private Refund refund(String status) {
        Refund refund = new Refund();
        refund.setId(REFUND_ID);
        refund.setOrderId(ORDER_ID);
        refund.setGoodsId(GOODS_ID);
        refund.setBuyerId(BUYER_ID);
        refund.setSellerId(SELLER_ID);
        refund.setAmount(new BigDecimal("88.00"));
        refund.setStatus(status);
        return refund;
    }

    private Goods goods(String status, int isDeleted) {
        Goods goods = new Goods();
        goods.setId(GOODS_ID);
        goods.setTitle("九成新机械键盘");
        goods.setStatus(status);
        goods.setIsDeleted(isDeleted);
        return goods;
    }
}
