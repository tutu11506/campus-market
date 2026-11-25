package com.example.campusmarket.service;

import com.example.campusmarket.common.ForbiddenException;
import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.entity.Dispute;
import com.example.campusmarket.entity.DisputeEvidence;
import com.example.campusmarket.entity.Message;
import com.example.campusmarket.entity.Order;
import com.example.campusmarket.mapper.DisputeMapper;
import com.example.campusmarket.mapper.MessageMapper;
import com.example.campusmarket.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 交易纠纷简易仲裁：
 * 买家/卖家线上提交聊天记录、照片证据 -> 管理员介入 -> 担保资金冻结/退款/结算。
 * 发起仲裁即冻结该订单托管资金，避免判前资金被结算。
 */
@Service
public class DisputeService {

    /** 系统通知发送者（管理员账号） */
    private static final int SYSTEM_SENDER_ID = 1;

    @Autowired
    private DisputeMapper disputeMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private RiskService riskService;

    private static final Map<String, String> REASON_LABELS = Map.of(
            "quality", "商品质量问题",
            "not_as_described", "与描述不符",
            "no_delivery", "未发货/未交付",
            "refund_refused", "拒绝退款",
            "other", "其他纠纷"
    );

    @Transactional
    public Map<String, Object> create(Integer uid, Integer orderId, String reasonType,
                                      String description, BigDecimal claimAmount) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        boolean isBuyer = uid.equals(order.getBuyerId());
        boolean isSeller = uid.equals(order.getSellerId());
        if (!isBuyer && !isSeller) {
            throw new ForbiddenException("无权对该订单发起仲裁");
        }
        if (order.getPayTime() == null) {
            throw new IllegalArgumentException("订单尚未支付，无法发起仲裁");
        }
        if ("cancelled".equals(order.getStatus())) {
            throw new IllegalArgumentException("已取消的订单无法发起仲裁");
        }
        if (disputeMapper.countActiveByOrder(orderId) > 0) {
            throw new IllegalArgumentException("该订单已有进行中的仲裁，请等待管理员处理");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("请填写纠纷说明");
        }
        String reason = (reasonType == null || reasonType.isEmpty()) ? "other" : reasonType;
        if (!REASON_LABELS.containsKey(reason)) {
            reason = "other";
        }

        Dispute dispute = new Dispute();
        dispute.setOrderId(orderId);
        dispute.setInitiatorId(uid);
        dispute.setRespondentId(isBuyer ? order.getSellerId() : order.getBuyerId());
        dispute.setInitiatorRole(isBuyer ? "buyer" : "seller");
        dispute.setReasonType(reason);
        dispute.setDescription(cut(description.trim(), 1000));
        dispute.setClaimAmount(claimAmount);
        dispute.setStatus("open");
        dispute.setEscrowFrozen(0);
        disputeMapper.insert(dispute);

        // 冻结担保资金（仅托管中可冻结；已结算的订单只记录纠纷）
        if (disputeMapper.freezeEscrow(orderId) > 0) {
            disputeMapper.markFrozen(dispute.getId());
            dispute.setEscrowFrozen(1);
        }

        String reasonLabel = REASON_LABELS.get(reason);
        notify(dispute.getRespondentId(), "【交易纠纷】订单 " + order.getOrderNo() + " 被发起仲裁（"
                + reasonLabel + "），担保资金已冻结。请尽快在「我的-订单」中补充证据并等待管理员仲裁。");
        notify(uid, "【交易纠纷】你的仲裁申请已提交（" + reasonLabel + "），管理员介入前可继续补充聊天记录/照片证据。");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("disputeId", dispute.getId());
        data.put("escrowFrozen", dispute.getEscrowFrozen());
        data.put("message", dispute.getEscrowFrozen() == 1
                ? "仲裁已受理，担保资金已冻结，请等待管理员处理"
                : "仲裁已受理（该订单资金已结算，管理员将依据证据处理）");
        return data;
    }

    @Transactional
    public String addEvidence(Integer uid, Integer disputeId, String type, String content, String fileUrl) {
        Dispute dispute = disputeMapper.findById(disputeId);
        if (dispute == null) {
            throw new IllegalArgumentException("纠纷不存在");
        }
        if (!uid.equals(dispute.getInitiatorId()) && !uid.equals(dispute.getRespondentId())) {
            throw new ForbiddenException("无权提交该纠纷的证据");
        }
        if (!"open".equals(dispute.getStatus()) && !"processing".equals(dispute.getStatus())) {
            throw new IllegalArgumentException("该纠纷已结案，无法再补充证据");
        }
        String t = (type == null || type.isEmpty()) ? "text" : type;
        if (!"chat".equals(t) && !"image".equals(t) && !"text".equals(t)) {
            t = "text";
        }
        if ((content == null || content.trim().isEmpty()) && (fileUrl == null || fileUrl.trim().isEmpty())) {
            throw new IllegalArgumentException("请填写说明或上传截图");
        }

        DisputeEvidence evidence = new DisputeEvidence();
        evidence.setDisputeId(disputeId);
        evidence.setUploaderId(uid);
        evidence.setType(t);
        evidence.setContent(content == null ? null : cut(content.trim(), 1000));
        evidence.setFileUrl(fileUrl);
        disputeMapper.insertEvidence(evidence);

        // 告知对方有新证据
        Integer other = uid.equals(dispute.getInitiatorId()) ? dispute.getRespondentId() : dispute.getInitiatorId();
        notify(other, "【交易纠纷】对方在纠纷 #" + disputeId + " 中补充了新证据，请查看并准备说明。");
        return "证据已提交";
    }

    public Map<String, Object> detail(Integer uid, Integer disputeId) {
        Dispute dispute = disputeMapper.findById(disputeId);
        if (dispute == null) {
            throw new IllegalArgumentException("纠纷不存在");
        }
        if (!uid.equals(dispute.getInitiatorId()) && !uid.equals(dispute.getRespondentId())) {
            throw new ForbiddenException("无权查看该纠纷");
        }
        return pack(dispute);
    }

    public Map<String, Object> adminDetail(Integer disputeId) {
        Dispute dispute = disputeMapper.findById(disputeId);
        if (dispute == null) {
            throw new IllegalArgumentException("纠纷不存在");
        }
        return pack(dispute);
    }

    private Map<String, Object> pack(Dispute dispute) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dispute", dispute);
        data.put("evidence", disputeMapper.findEvidence(dispute.getId()));
        return data;
    }

    public PageResult<Dispute> myPage(Integer uid, String status, Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        List<Dispute> list = disputeMapper.findMyPage(uid, status, p[1], p[2]);
        int total = disputeMapper.countMy(uid, status);
        return PageResult.of(list, total, p[0], p[1]);
    }

    public PageResult<Dispute> adminPage(String status, Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        List<Dispute> list = disputeMapper.findAdminPage(status, p[1], p[2]);
        int total = disputeMapper.countAdmin(status);
        return PageResult.of(list, total, p[0], p[1]);
    }

    @Transactional
    public String startProcessing(Integer adminId, Integer disputeId) {
        if (disputeMapper.markProcessing(disputeId, adminId) == 0) {
            throw new IllegalArgumentException("该纠纷已结案或被处理");
        }
        Dispute dispute = disputeMapper.findById(disputeId);
        notify(dispute.getInitiatorId(), "【交易纠纷】管理员已介入纠纷 #" + disputeId + "，请保持联系方式畅通。");
        notify(dispute.getRespondentId(), "【交易纠纷】管理员已介入纠纷 #" + disputeId + "，请及时提交对己方有利的证据。");
        return "已介入处理";
    }

    /** 裁决：buyer_win/seller_win/split/no_fault */
    @Transactional
    public String resolve(Integer adminId, Integer disputeId, String verdict, String note) {
        Dispute dispute = disputeMapper.findById(disputeId);
        if (dispute == null) {
            throw new IllegalArgumentException("纠纷不存在");
        }
        if (!"open".equals(dispute.getStatus()) && !"processing".equals(dispute.getStatus())) {
            throw new IllegalArgumentException("该纠纷已结案");
        }
        if (!List.of("buyer_win", "seller_win", "split", "no_fault").contains(verdict)) {
            throw new IllegalArgumentException("非法裁决结果");
        }

        disputeMapper.resolve(disputeId, verdict, note, adminId);
        Integer orderId = dispute.getOrderId();
        String resultText;

        switch (verdict) {
            case "buyer_win" -> {
                disputeMapper.refundEscrow(orderId);
                disputeMapper.updateOrderStatusAndRefund(orderId, "returned", "returned");
                riskService.deductCredit(dispute.getRespondentId(), 10);
                resultText = "买家胜诉：担保资金已退回买家，订单转为已退货；卖家信用分 -10";
            }
            case "seller_win" -> {
                disputeMapper.settleEscrow(orderId);
                disputeMapper.updateOrderStatus(orderId, "completed");
                resultText = "卖家胜诉：担保资金已结算给卖家，订单转为已完成";
            }
            case "split" -> {
                disputeMapper.refundEscrow(orderId);
                disputeMapper.updateOrderStatusAndRefund(orderId, "returned", "returned");
                resultText = "责任共担：担保资金退回买家，订单转为已退货";
            }
            case "no_fault" -> {
                disputeMapper.settleEscrow(orderId);
                disputeMapper.updateOrderStatus(orderId, "completed");
                resultText = "证据不足，维持原交易：担保资金结算给卖家";
            }
            default -> resultText = "已裁决";
        }

        String suffix = (note == null || note.trim().isEmpty()) ? "" : "。说明：" + cut(note.trim(), 200);
        notify(dispute.getInitiatorId(), "【仲裁结果】纠纷 #" + disputeId + "：" + resultText + suffix);
        notify(dispute.getRespondentId(), "【仲裁结果】纠纷 #" + disputeId + "：" + resultText + suffix);
        return resultText;
    }

    public Map<String, Object> stats() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pending", disputeMapper.countPending());
        data.put("total", disputeMapper.countAll());
        return data;
    }

    private void notify(Integer receiverId, String content) {
        if (receiverId == null) return;
        Message msg = new Message();
        msg.setSenderId(SYSTEM_SENDER_ID);
        msg.setReceiverId(receiverId);
        msg.setContent(cut(content, 480));
        msg.setMsgType("text");
        msg.setAutoReply(0);
        messageMapper.insert(msg);
    }

    private String cut(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
