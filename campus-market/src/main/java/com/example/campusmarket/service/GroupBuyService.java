package com.example.campusmarket.service;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.entity.GroupBuy;
import com.example.campusmarket.entity.GroupBuyMember;
import com.example.campusmarket.entity.Message;
import com.example.campusmarket.mapper.GroupBuyMapper;
import com.example.campusmarket.mapper.MessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * 拼单：全新团购、拼分装、共享会员、二手大件分摊。
 * 发起人自动占 1 份；满员锁定并提醒，发起人可提前确认成团或关闭；
 * 截止时未满自动标记失败；不接支付，线下结算。
 */
@Service
public class GroupBuyService {

    private static final Set<String> TYPES = Set.of("fresh", "split", "membership", "secondhand");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("M月d日 HH:mm");

    @Autowired
    private GroupBuyMapper groupBuyMapper;

    @Autowired
    private MessageMapper messageMapper;

    /* ==================== 列表与详情 ==================== */

    public PageResult<GroupBuy> list(String type, String status, String keyword, Integer uid,
                                     Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 12);
        String typeFilter = trim(type);
        String statusFilter = trim(status);
        String keywordFilter = trim(keyword);
        List<GroupBuy> list = groupBuyMapper.findList(typeFilter, statusFilter, keywordFilter, uid, p[2], p[1]);
        return PageResult.of(list, groupBuyMapper.countList(typeFilter, statusFilter, keywordFilter), p[0], p[1]);
    }

    public GroupBuy detail(Integer id, Integer uid) {
        GroupBuy groupBuy = mustExist(id, uid);
        groupBuy.setMembers(groupBuyMapper.findJoinedMembers(id));
        return groupBuy;
    }

    public PageResult<GroupBuy> myList(Integer uid, String role, String status,
                                       Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 10);
        String statusFilter = trim(status);
        boolean owner = !"member".equals(role);
        List<GroupBuy> list = owner
                ? groupBuyMapper.findOwnerList(uid, statusFilter, p[2], p[1])
                : groupBuyMapper.findJoinedList(uid, statusFilter, p[2], p[1]);
        long total = owner
                ? groupBuyMapper.countOwnerList(uid, statusFilter)
                : groupBuyMapper.countJoinedList(uid, statusFilter);
        return PageResult.of(list, total, p[0], p[1]);
    }

    /* ==================== 发起与参与 ==================== */

    public GroupBuy create(Integer uid, GroupBuy form) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        if (form == null) {
            throw new IllegalArgumentException("请填写拼单信息");
        }
        GroupBuy groupBuy = new GroupBuy();
        groupBuy.setOwnerId(uid);

        String type = trim(form.getType());
        if (type == null || !TYPES.contains(type)) {
            throw new IllegalArgumentException("请选择正确的拼单类型");
        }
        groupBuy.setType(type);
        groupBuy.setTitle(required(form.getTitle(), 100, "请填写拼单标题，最多 100 字"));
        groupBuy.setDescription(optional(form.getDescription(), 600, "拼单说明最多 600 字"));

        Integer target = form.getTargetCount();
        if (target == null || target < 2 || target > 50) {
            throw new IllegalArgumentException("目标份数需在 2-50 份之间（含发起人 1 份）");
        }
        groupBuy.setTargetCount(target);

        BigDecimal price = form.getPricePerShare();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0
                || price.compareTo(new BigDecimal("99999")) > 0) {
            throw new IllegalArgumentException("每份费用需大于 0 且不超过 99999");
        }
        groupBuy.setPricePerShare(price.setScale(2, RoundingMode.HALF_UP));

        if (form.getDeadline() == null) {
            throw new IllegalArgumentException("请选择拼单截止时间");
        }
        if (!form.getDeadline().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("截止时间需晚于当前时间");
        }
        groupBuy.setDeadline(form.getDeadline());
        groupBuy.setGoodsId(form.getGoodsId());
        groupBuy.setLocation(optional(form.getLocation(), 100, "取货地点最多 100 字"));

        groupBuyMapper.insert(groupBuy);
        groupBuyMapper.insertMember(groupBuy.getId(), uid, 1);
        return mustExist(groupBuy.getId(), uid);
    }

    public GroupBuy join(Integer uid, Integer id, Integer shares) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        GroupBuy groupBuy = mustExist(id, uid);
        String block = joinBlockReason(groupBuy);
        if (block != null) {
            throw new IllegalArgumentException(block);
        }
        int remaining = Math.max(0, groupBuy.getTargetCount() - groupBuy.getJoinedCount());
        int want = shares == null ? 1 : shares;
        if (want < 1 || want > remaining) {
            throw new IllegalArgumentException("参与份数需在 1-" + remaining + " 份之间");
        }
        GroupBuyMember member = groupBuyMapper.findMember(id, uid);
        if (member != null && "joined".equals(member.getStatus())) {
            throw new IllegalArgumentException("您已参与该拼单，无需重复报名");
        }
        if (groupBuyMapper.joinAtomic(id, want) == 0) {
            throw new IllegalArgumentException("该拼单已满员或已截止，请刷新后重试");
        }
        try {
            int updated = groupBuyMapper.reactivateMember(id, uid, want);
            if (updated == 0) {
                groupBuyMapper.insertMember(id, uid, want);
            }
        } catch (RuntimeException e) {
            groupBuyMapper.releaseShares(id, want);
            throw e;
        }
        GroupBuy latest = mustExist(id, uid);
        if ("full".equals(latest.getStatus())) {
            for (GroupBuyMember m : groupBuyMapper.findJoinedMembers(id)) {
                notify(m.getUserId(), "拼单《" + latest.getTitle() + "》已满员，等待发起人确认成团，请留意后续通知。");
            }
        }
        return latest;
    }

    public GroupBuy leave(Integer uid, Integer id) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        GroupBuy groupBuy = mustExist(id, uid);
        if (uid.equals(groupBuy.getOwnerId())) {
            throw new IllegalArgumentException("发起人不能退出，如不再拼单请直接关闭");
        }
        if (!"open".equals(groupBuy.getStatus())) {
            throw new IllegalArgumentException("该拼单已满员或已结束，不能退出");
        }
        GroupBuyMember member = groupBuyMapper.findMember(id, uid);
        if (member == null || !"joined".equals(member.getStatus())) {
            throw new IllegalArgumentException("您尚未参与该拼单");
        }
        if (groupBuyMapper.leaveMember(id, uid) == 0) {
            throw new IllegalArgumentException("退出失败，请刷新后重试");
        }
        if (groupBuyMapper.releaseShares(id, member.getShares()) == 0) {
            groupBuyMapper.reactivateMember(id, uid, member.getShares());
            throw new IllegalArgumentException("拼单状态已变化，请刷新后重试");
        }
        notify(groupBuy.getOwnerId(), "成员 " + member.getUserName() + " 退出了您的拼单《"
                + groupBuy.getTitle() + "》，已释放 " + member.getShares() + " 份。");
        return mustExist(id, uid);
    }

    public GroupBuy confirm(Integer uid, Integer id) {
        GroupBuy groupBuy = mustExist(id, uid);
        if (!uid.equals(groupBuy.getOwnerId())) {
            throw new IllegalArgumentException("只有发起人可以确认成团");
        }
        if (!"open".equals(groupBuy.getStatus()) && !"full".equals(groupBuy.getStatus())) {
            throw new IllegalArgumentException("当前拼单状态不能确认成团");
        }
        if (groupBuyMapper.confirm(id, uid) == 0) {
            throw new IllegalArgumentException("拼单状态已变化，请刷新后重试");
        }
        for (GroupBuyMember m : groupBuyMapper.findJoinedMembers(id)) {
            notify(m.getUserId(), "拼单《" + groupBuy.getTitle() + "》已由发起人确认成团，每份 "
                    + money(groupBuy.getPricePerShare()) + " 元，请与发起人联系完成线下结算。");
        }
        return mustExist(id, uid);
    }

    public GroupBuy close(Integer uid, Integer id) {
        GroupBuy groupBuy = mustExist(id, uid);
        if (!uid.equals(groupBuy.getOwnerId())) {
            throw new IllegalArgumentException("只有发起人可以关闭拼单");
        }
        if (groupBuyMapper.close(id, uid) == 0) {
            throw new IllegalArgumentException("当前拼单状态不能关闭");
        }
        for (GroupBuyMember m : groupBuyMapper.findJoinedMembers(id)) {
            notify(m.getUserId(), "拼单《" + groupBuy.getTitle() + "》已被发起人关闭，无需再等待。");
        }
        return mustExist(id, uid);
    }

    /** 搭子发布时可关联的进行中拼单 */
    public List<GroupBuy> linkable(Integer uid) {
        return groupBuyMapper.findLinkable(uid);
    }

    /* ==================== 定时任务：截止未满自动失败 ==================== */

    @Scheduled(initialDelay = 150_000, fixedDelay = 10 * 60 * 1000)
    public void expireOverdue() {
        for (Integer id : groupBuyMapper.findExpiredIds()) {
            if (groupBuyMapper.markFailed(id) == 0) {
                continue;
            }
            GroupBuy groupBuy = groupBuyMapper.findById(id, null);
            if (groupBuy == null) {
                continue;
            }
            for (GroupBuyMember m : groupBuyMapper.findJoinedMembers(id)) {
                notify(m.getUserId(), "拼单《" + groupBuy.getTitle() + "》到 "
                        + formatTime(groupBuy.getDeadline()) + " 截止时未满 "
                        + groupBuy.getTargetCount() + " 份，本次未成团，可与发起人协商后重新发起。");
            }
        }
    }

    /* ==================== 内部工具 ==================== */

    private String joinBlockReason(GroupBuy groupBuy) {
        String status = groupBuy.getStatus();
        if (!"open".equals(status)) {
            if ("full".equals(status)) {
                return "该拼单已满员";
            }
            if ("formed".equals(status)) {
                return "该拼单已成团，不能再参与";
            }
            if ("failed".equals(status)) {
                return "该拼单已截止且未成团";
            }
            return "该拼单已关闭";
        }
        if (groupBuy.getDeadline() == null || !groupBuy.getDeadline().isAfter(LocalDateTime.now())) {
            return "该拼单已到截止时间，不能再参与";
        }
        if (groupBuy.getJoinedCount() != null && groupBuy.getJoinedCount() >= groupBuy.getTargetCount()) {
            return "该拼单已满员";
        }
        return null;
    }

    private GroupBuy mustExist(Integer id, Integer uid) {
        GroupBuy groupBuy = groupBuyMapper.findById(id, uid);
        if (groupBuy == null) {
            throw new IllegalArgumentException("拼单不存在");
        }
        if (groupBuy.getMyShares() == null) {
            groupBuy.setMyShares(0);
        }
        return groupBuy;
    }

    private void notify(Integer receiverId, String content) {
        if (receiverId == null) {
            return;
        }
        try {
            Integer kefuId = groupBuyMapper.findCustomerServiceId();
            if (kefuId == null || kefuId.equals(receiverId)) {
                return;
            }
            Message message = new Message();
            message.setSenderId(kefuId);
            message.setReceiverId(receiverId);
            message.setContent(content);
            message.setMsgType("text");
            messageMapper.insert(message);
        } catch (Exception e) {
            System.err.println("拼单提醒发送失败：" + e.getMessage());
        }
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "-" : time.format(TIME_FMT);
    }

    private String money(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private String required(String value, int max, String message) {
        String v = trim(value);
        if (v == null || v.length() > max) {
            throw new IllegalArgumentException(message);
        }
        return v;
    }

    private String optional(String value, int max, String message) {
        String v = trim(value);
        if (v == null) {
            return null;
        }
        if (v.length() > max) {
            throw new IllegalArgumentException(message);
        }
        return v;
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }
}
