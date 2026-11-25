package com.example.campusmarket.service;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.entity.Message;
import com.example.campusmarket.entity.PartnerMember;
import com.example.campusmarket.entity.PartnerPost;
import com.example.campusmarket.mapper.MessageMapper;
import com.example.campusmarket.mapper.PartnerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 搭子组队：装备拼买、跳蚤市场同行、考研学习、运动等。
 * 发起人自动占 1 人；人数满后锁定；活动时间已过自动结束；可关联闲置或进行中的拼单。
 */
@Service
public class PartnerService {

    private static final Set<String> TYPES = Set.of("gear", "flea", "exam", "sport", "other");
    private static final Set<String> LINK_TYPES = Set.of("none", "goods", "groupbuy");

    @Autowired
    private PartnerMapper partnerMapper;

    @Autowired
    private MessageMapper messageMapper;

    /* ==================== 列表与详情 ==================== */

    public PageResult<PartnerPost> list(String type, String status, String keyword, Integer uid,
                                        Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 12);
        String typeFilter = trim(type);
        String statusFilter = trim(status);
        String keywordFilter = trim(keyword);
        List<PartnerPost> list = partnerMapper.findList(typeFilter, statusFilter, keywordFilter, uid, p[2], p[1]);
        return PageResult.of(list, partnerMapper.countList(typeFilter, statusFilter, keywordFilter), p[0], p[1]);
    }

    public PartnerPost detail(Integer id, Integer uid) {
        PartnerPost post = mustExist(id, uid);
        post.setMembers(partnerMapper.findJoinedMembers(id));
        return post;
    }

    public PageResult<PartnerPost> myList(Integer uid, String role, String status,
                                          Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 10);
        String statusFilter = trim(status);
        boolean owner = !"member".equals(role);
        List<PartnerPost> list = owner
                ? partnerMapper.findOwnerList(uid, statusFilter, p[2], p[1])
                : partnerMapper.findJoinedList(uid, statusFilter, p[2], p[1]);
        long total = owner
                ? partnerMapper.countOwnerList(uid, statusFilter)
                : partnerMapper.countJoinedList(uid, statusFilter);
        return PageResult.of(list, total, p[0], p[1]);
    }

    /* ==================== 发起与参与 ==================== */

    public PartnerPost create(Integer uid, PartnerPost form) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        if (form == null) {
            throw new IllegalArgumentException("请填写搭子信息");
        }
        PartnerPost post = new PartnerPost();
        post.setOwnerId(uid);

        String type = trim(form.getType());
        if (type == null || !TYPES.contains(type)) {
            throw new IllegalArgumentException("请选择正确的搭子类型");
        }
        post.setType(type);
        post.setTitle(required(form.getTitle(), 100, "请填写搭子标题，最多 100 字"));
        post.setDescription(optional(form.getDescription(), 600, "搭子说明最多 600 字"));

        if (form.getActivityTime() == null) {
            throw new IllegalArgumentException("请选择活动时间");
        }
        if (!form.getActivityTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("活动时间需晚于当前时间");
        }
        post.setActivityTime(form.getActivityTime());
        post.setLocation(optional(form.getLocation(), 100, "活动地点最多 100 字"));

        Integer limit = form.getMemberLimit();
        if (limit == null || limit < 2 || limit > 30) {
            throw new IllegalArgumentException("人数上限需在 2-30 人之间（含发起人 1 人）");
        }
        post.setMemberLimit(limit);

        String linkType = trim(form.getLinkType());
        if (linkType == null) {
            linkType = "none";
        }
        if (!LINK_TYPES.contains(linkType)) {
            throw new IllegalArgumentException("关联类型不正确");
        }
        post.setLinkType(linkType);
        if (!"none".equals(linkType)) {
            if (form.getLinkId() == null) {
                throw new IllegalArgumentException("请选择要关联的内容");
            }
            String linkTitle = "goods".equals(linkType)
                    ? partnerMapper.findGoodsTitle(form.getLinkId())
                    : partnerMapper.findGroupBuyTitle(form.getLinkId());
            if (linkTitle == null) {
                throw new IllegalArgumentException("关联内容不存在或已结束");
            }
            post.setLinkId(form.getLinkId());
            post.setLinkTitle(linkTitle);
        }

        partnerMapper.insert(post);
        partnerMapper.insertMember(post.getId(), uid);
        return mustExist(post.getId(), uid);
    }

    public PartnerPost join(Integer uid, Integer id) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        PartnerPost post = mustExist(id, uid);
        String block = joinBlockReason(post);
        if (block != null) {
            throw new IllegalArgumentException(block);
        }
        PartnerMember member = partnerMapper.findMember(id, uid);
        if (member != null && "joined".equals(member.getStatus())) {
            throw new IllegalArgumentException("您已加入该搭子，无需重复报名");
        }
        if (partnerMapper.joinAtomic(id) == 0) {
            throw new IllegalArgumentException("该搭子已满员或已结束，请刷新后重试");
        }
        try {
            if (partnerMapper.reactivateMember(id, uid) == 0) {
                partnerMapper.insertMember(id, uid);
            }
        } catch (RuntimeException e) {
            partnerMapper.releaseOne(id);
            throw e;
        }
        PartnerPost latest = mustExist(id, uid);
        if ("full".equals(latest.getStatus())) {
            for (PartnerMember m : partnerMapper.findJoinedMembers(id)) {
                notify(m.getUserId(), "搭子《" + latest.getTitle() + "》人数已满，请按约定时间在 "
                        + (latest.getLocation() == null ? "约定地点" : latest.getLocation()) + " 集合。");
            }
        }
        return latest;
    }

    public PartnerPost leave(Integer uid, Integer id) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        PartnerPost post = mustExist(id, uid);
        if (uid.equals(post.getOwnerId())) {
            throw new IllegalArgumentException("发起人不能退出，如不再组队请直接取消");
        }
        if (!"open".equals(post.getStatus())) {
            throw new IllegalArgumentException("该搭子已满员或已结束，不能退出");
        }
        PartnerMember member = partnerMapper.findMember(id, uid);
        if (member == null || !"joined".equals(member.getStatus())) {
            throw new IllegalArgumentException("您尚未加入该搭子");
        }
        if (partnerMapper.leaveMember(id, uid) == 0) {
            throw new IllegalArgumentException("退出失败，请刷新后重试");
        }
        if (partnerMapper.releaseOne(id) == 0) {
            partnerMapper.reactivateMember(id, uid);
            throw new IllegalArgumentException("搭子状态已变化，请刷新后重试");
        }
        notify(post.getOwnerId(), "成员 " + member.getUserName() + " 退出了您的搭子《"
                + post.getTitle() + "》。");
        return mustExist(id, uid);
    }

    public PartnerPost cancel(Integer uid, Integer id) {
        PartnerPost post = mustExist(id, uid);
        if (!uid.equals(post.getOwnerId())) {
            throw new IllegalArgumentException("只有发起人可以取消搭子");
        }
        if (partnerMapper.cancel(id, uid) == 0) {
            throw new IllegalArgumentException("当前搭子状态不能取消");
        }
        for (PartnerMember m : partnerMapper.findJoinedMembers(id)) {
            notify(m.getUserId(), "搭子《" + post.getTitle() + "》已被发起人取消。");
        }
        return mustExist(id, uid);
    }

    /* ==================== 定时任务：活动时间已过自动结束 ==================== */

    @Scheduled(initialDelay = 180_000, fixedDelay = 10 * 60 * 1000)
    public void finishExpired() {
        for (Integer id : partnerMapper.findExpiredIds()) {
            partnerMapper.markFinished(id);
        }
    }

    /* ==================== 内部工具 ==================== */

    private String joinBlockReason(PartnerPost post) {
        String status = post.getStatus();
        if (!"open".equals(status)) {
            if ("full".equals(status)) {
                return "该搭子已满员";
            }
            if ("finished".equals(status)) {
                return "该搭子已结束";
            }
            return "该搭子已取消";
        }
        if (post.getActivityTime() == null || !post.getActivityTime().isAfter(LocalDateTime.now())) {
            return "活动时间已到，不能再报名";
        }
        if (post.getMemberCount() != null && post.getMemberCount() >= post.getMemberLimit()) {
            return "该搭子已满员";
        }
        return null;
    }

    private PartnerPost mustExist(Integer id, Integer uid) {
        PartnerPost post = partnerMapper.findById(id, uid);
        if (post == null) {
            throw new IllegalArgumentException("搭子不存在");
        }
        if (post.getMyStatus() == null) {
            post.setMyStatus(0);
        }
        return post;
    }

    private void notify(Integer receiverId, String content) {
        if (receiverId == null) {
            return;
        }
        try {
            Integer kefuId = partnerMapper.findCustomerServiceId();
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
            System.err.println("搭子提醒发送失败：" + e.getMessage());
        }
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
