package com.example.campusmarket.service;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.ForbiddenException;
import com.example.campusmarket.entity.Goods;
import com.example.campusmarket.entity.Order;
import com.example.campusmarket.entity.Refund;
import com.example.campusmarket.mapper.GoodsMapper;
import com.example.campusmarket.mapper.OrderMapper;
import com.example.campusmarket.mapper.RefundMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 退货申请业务。状态主源为 refund.status：
 * pending(待审核) --approve--> approved(已同意)，联动 order.status=returned、goods 重新上架；
 * pending(待审核) --reject---> rejected(已拒绝)，联动 order.refund_status=return_rejected（订单仍 completed）。
 * 一单可多次申请（被拒后可再次申请），存在 pending 时禁止重复申请。
 */
@Service
public class RefundService {

    @Autowired
    private RefundMapper refundMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    private static final String ORDER_COMPLETED = "completed";
    private static final String ORDER_RETURNED = "returned";

    private static final String REFUND_PENDING = "pending";
    private static final String REFUND_APPROVED = "approved";
    private static final String REFUND_REJECTED = "rejected";

    private static final String RS_RETURNING = "returning";
    private static final String RS_RETURNED = "returned";
    private static final String RS_REJECTED = "return_rejected";

    /** 买家申请退货 */
    @Transactional(rollbackFor = Exception.class)
    public String apply(Refund refund, Integer buyerId) {
        if (refund == null || refund.getOrderId() == null) {
            return "参数错误";
        }
        if (refund.getReason() == null || refund.getReason().trim().isEmpty()) {
            return "请填写退货原因";
        }
        Order order = orderMapper.findById(refund.getOrderId());
        if (order == null) {
            return "订单不存在";
        }
        if (!buyerId.equals(order.getBuyerId())) {
            throw new ForbiddenException("无权操作此订单");
        }
        if (ORDER_RETURNED.equals(order.getStatus())) {
            return "该订单已退货，不可重复申请";
        }
        if (!ORDER_COMPLETED.equals(order.getStatus())) {
            return "仅已完成的订单可申请退货";
        }
        // 防重复：存在待审核申请则拒绝
        Refund pending = refundMapper.findPendingByOrderId(refund.getOrderId());
        if (pending != null) {
            return "该订单已有待审核的退货申请，请等待卖家处理";
        }
        // 填充服务端可信字段（价格/卖家/商品以服务端为准，不信任前端）
        refund.setRefundNo("RF" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4));
        refund.setBuyerId(buyerId);
        refund.setSellerId(order.getSellerId());
        refund.setGoodsId(order.getGoodsId());
        refund.setAmount(order.getPrice());
        refund.setStatus(REFUND_PENDING);
        if (refund.getDescription() != null && refund.getDescription().length() > 500) {
            refund.setDescription(refund.getDescription().substring(0, 500));
        }
        if (refund.getEvidenceUrls() != null && refund.getEvidenceUrls().length() > 2000) {
            refund.setEvidenceUrls(refund.getEvidenceUrls().substring(0, 2000));
        }
        refundMapper.insert(refund);
        orderMapper.updateRefundStatus(order.getId(), RS_RETURNING);
        return "退货申请已提交，请等待卖家审核";
    }

    /** 卖家同意退货：审核通过 + 订单标记已退货 + 商品重新上架（同一事务） */
    @Transactional(rollbackFor = Exception.class)
    public String approve(Integer refundId, Integer sellerId) {
        Refund refund = refundMapper.findById(refundId);
        if (refund == null) {
            return "退货申请不存在";
        }
        if (!sellerId.equals(refund.getSellerId())) {
            throw new ForbiddenException("无权操作此退货申请");
        }
        if (!REFUND_PENDING.equals(refund.getStatus())) {
            return "该退货申请已处理，不可重复审核";
        }
        refundMapper.updateAudit(refundId, REFUND_APPROVED, sellerId, null);
        orderMapper.updateStatusAndRefundStatus(refund.getOrderId(), ORDER_RETURNED, RS_RETURNED);
        // 退货成立：托管资金退回买家
        orderMapper.refundEscrow(refund.getOrderId());
        // 商品重新上架：仅当商品当前为 sold 且未删除时改回 on_sale（已下架/已删除则不动，避免强行上架）
        Goods goods = goodsMapper.findById(refund.getGoodsId());
        if (goods != null && "sold".equals(goods.getStatus())
                && (goods.getIsDeleted() == null || goods.getIsDeleted() == 0)) {
            int rows = goodsMapper.updateStatus(refund.getGoodsId(), "on_sale");
            if (rows == 0) {
                throw new IllegalStateException("商品重新上架失败，退货已回滚");
            }
        }
        return "已同意退货，订单已标记为已退货，商品已重新上架";
    }

    /** 卖家拒绝退货 */
    @Transactional(rollbackFor = Exception.class)
    public String reject(Integer refundId, Integer sellerId, String rejectReason) {
        Refund refund = refundMapper.findById(refundId);
        if (refund == null) {
            return "退货申请不存在";
        }
        if (!sellerId.equals(refund.getSellerId())) {
            throw new ForbiddenException("无权操作此退货申请");
        }
        if (!REFUND_PENDING.equals(refund.getStatus())) {
            return "该退货申请已处理，不可重复审核";
        }
        if (rejectReason == null || rejectReason.trim().isEmpty()) {
            return "请填写拒绝原因";
        }
        if (rejectReason.length() > 255) {
            rejectReason = rejectReason.substring(0, 255);
        }
        refundMapper.updateAudit(refundId, REFUND_REJECTED, sellerId, rejectReason);
        orderMapper.updateRefundStatus(refund.getOrderId(), RS_REJECTED);
        return "已拒绝退货申请";
    }

    /** 某订单的退货申请列表（新→旧） */
    public List<Refund> findByOrderId(Integer orderId) {
        return refundMapper.findByOrderId(orderId);
    }

    /** 卖家的退货申请列表（分页） */
    public PageResult<Refund> findSellerRefunds(Integer sellerId, Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        List<Refund> list = refundMapper.findSellerRefundsPage(sellerId, p[2], p[1]);
        return PageResult.of(list, refundMapper.countSellerRefunds(sellerId), p[0], p[1]);
    }
}
