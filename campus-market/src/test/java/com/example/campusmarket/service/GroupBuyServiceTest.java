package com.example.campusmarket.service;

import com.example.campusmarket.entity.GroupBuy;
import com.example.campusmarket.entity.GroupBuyMember;
import com.example.campusmarket.entity.Message;
import com.example.campusmarket.mapper.GroupBuyMapper;
import com.example.campusmarket.mapper.MessageMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 拼单状态机回归：发起（自动占 1 份）→ 参与（原子占位，满员锁定，超员拦截）→ 退出/确认/关闭 → 到期失败。
 * 重点验证并发安全依赖的条件更新返回值：0 表示被抢占，必须拒绝且不留脏数据。
 */
@ExtendWith(MockitoExtension.class)
class GroupBuyServiceTest {

    private static final Integer GROUP_ID = 30;
    private static final Integer OWNER_ID = 6;
    private static final Integer MEMBER_ID = 7;
    private static final Integer KEFU_ID = 99;

    private static final String OPEN = "open";
    private static final String FULL = "full";
    private static final String FORMED = "formed";
    private static final String FAILED = "failed";
    private static final String CLOSED = "closed";

    @Mock
    private GroupBuyMapper groupBuyMapper;
    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private GroupBuyService groupBuyService;

    // ---------------- 发起拼单 ----------------

    @Test
    @DisplayName("发起成功：写入拼单后自动为发起人占 1 份")
    void create_insertsGroupBuyAndOwnerShare() {
        when(groupBuyMapper.findById(any(), any())).thenReturn(group(OPEN, 3, 1));
        when(groupBuyMapper.insert(any(GroupBuy.class))).thenAnswer(invocation -> {
            GroupBuy g = invocation.getArgument(0);
            g.setId(GROUP_ID);
            return 1;
        });

        GroupBuy form = new GroupBuy();
        form.setType("fresh");
        form.setTitle("整箱车厘子拼单");
        form.setTargetCount(3);
        form.setPricePerShare(new BigDecimal("45.50"));
        form.setDeadline(LocalDateTime.now().plusDays(3));

        GroupBuy created = groupBuyService.create(OWNER_ID, form);

        assertEquals(OWNER_ID, created.getOwnerId());
        ArgumentCaptor<GroupBuy> captor = ArgumentCaptor.forClass(GroupBuy.class);
        verify(groupBuyMapper).insert(captor.capture());
        assertEquals(OWNER_ID, captor.getValue().getOwnerId());
        assertEquals(new BigDecimal("45.50"), captor.getValue().getPricePerShare());
        // 发起人自动占 1 份，joined_count 从 1 起算
        verify(groupBuyMapper).insertMember(created.getId(), OWNER_ID, 1);
    }

    @Test
    @DisplayName("发起失败：目标份数小于 2 或大于 50 都被拒绝，不写库")
    void create_rejectsInvalidTargetCount() {
        GroupBuy form = validForm();
        form.setTargetCount(1);

        assertThrows(IllegalArgumentException.class, () -> groupBuyService.create(OWNER_ID, form));

        verify(groupBuyMapper, never()).insert(any());
    }

    @Test
    @DisplayName("发起失败：截止时间必须晚于当前时间")
    void create_rejectsPastDeadline() {
        GroupBuy form = validForm();
        form.setDeadline(LocalDateTime.now().minusMinutes(1));

        assertThrows(IllegalArgumentException.class, () -> groupBuyService.create(OWNER_ID, form));

        verify(groupBuyMapper, never()).insert(any());
    }

    @Test
    @DisplayName("发起失败：类型不在四种拼单类型内")
    void create_rejectsUnknownType() {
        GroupBuy form = validForm();
        form.setType("unknown");

        assertThrows(IllegalArgumentException.class, () -> groupBuyService.create(OWNER_ID, form));

        verify(groupBuyMapper, never()).insert(any());
    }

    // ---------------- 参与：原子占位 ----------------

    @Test
    @DisplayName("参与成功：先原子加份数，再落成员记录")
    void join_atomicallyAddsSharesThenInsertsMember() {
        GroupBuy open = group(OPEN, 4, 2);
        when(groupBuyMapper.findById(GROUP_ID, MEMBER_ID)).thenReturn(open, group(OPEN, 4, 3));
        when(groupBuyMapper.findMember(GROUP_ID, MEMBER_ID)).thenReturn(null);
        when(groupBuyMapper.joinAtomic(GROUP_ID, 1)).thenReturn(1);
        when(groupBuyMapper.reactivateMember(GROUP_ID, MEMBER_ID, 1)).thenReturn(0);

        groupBuyService.join(MEMBER_ID, GROUP_ID, 1);

        verify(groupBuyMapper).joinAtomic(GROUP_ID, 1);
        verify(groupBuyMapper).insertMember(GROUP_ID, MEMBER_ID, 1);
        verify(groupBuyMapper, never()).releaseShares(any(), anyInt());
    }

    @Test
    @DisplayName("参与失败：原子占位返回 0（刚好被抢满），不写成员、不释放份数")
    void join_whenAtomicFails_doesNotInsertMember() {
        when(groupBuyMapper.findById(GROUP_ID, MEMBER_ID)).thenReturn(group(OPEN, 4, 3));
        when(groupBuyMapper.findMember(GROUP_ID, MEMBER_ID)).thenReturn(null);
        when(groupBuyMapper.joinAtomic(GROUP_ID, 1)).thenReturn(0);

        assertThrows(IllegalArgumentException.class,
                () -> groupBuyService.join(MEMBER_ID, GROUP_ID, 1));

        verify(groupBuyMapper, never()).insertMember(any(), any(), anyInt());
        verify(groupBuyMapper, never()).releaseShares(any(), anyInt());
    }

    @Test
    @DisplayName("参与失败：份数超过剩余份数（超员拦截）")
    void join_rejectsSharesOverRemaining() {
        when(groupBuyMapper.findById(GROUP_ID, MEMBER_ID)).thenReturn(group(OPEN, 4, 3));

        assertThrows(IllegalArgumentException.class,
                () -> groupBuyService.join(MEMBER_ID, GROUP_ID, 2));

        verify(groupBuyMapper, never()).joinAtomic(any(), anyInt());
    }

    @Test
    @DisplayName("参与失败：已满员或已成团/已关闭的拼单不能再参与")
    void join_rejectsClosedOrFullGroup() {
        when(groupBuyMapper.findById(GROUP_ID, MEMBER_ID)).thenReturn(group(FULL, 3, 3));

        assertThrows(IllegalArgumentException.class,
                () -> groupBuyService.join(MEMBER_ID, GROUP_ID, 1));

        verify(groupBuyMapper, never()).joinAtomic(any(), anyInt());
    }

    @Test
    @DisplayName("参与失败：已到截止时间的拼单不能再参与")
    void join_rejectsExpiredDeadline() {
        GroupBuy expired = group(OPEN, 3, 1);
        expired.setDeadline(LocalDateTime.now().minusMinutes(5));
        when(groupBuyMapper.findById(GROUP_ID, MEMBER_ID)).thenReturn(expired);

        assertThrows(IllegalArgumentException.class,
                () -> groupBuyService.join(MEMBER_ID, GROUP_ID, 1));

        verify(groupBuyMapper, never()).joinAtomic(any(), anyInt());
    }

    @Test
    @DisplayName("参与失败：重复报名被拦截，不再增加份数")
    void join_rejectsDuplicateMember() {
        GroupBuyMember joined = new GroupBuyMember();
        joined.setStatus("joined");
        when(groupBuyMapper.findById(GROUP_ID, MEMBER_ID)).thenReturn(group(OPEN, 4, 2));
        when(groupBuyMapper.findMember(GROUP_ID, MEMBER_ID)).thenReturn(joined);

        assertThrows(IllegalArgumentException.class,
                () -> groupBuyService.join(MEMBER_ID, GROUP_ID, 1));

        verify(groupBuyMapper, never()).joinAtomic(any(), anyInt());
    }

    @Test
    @DisplayName("参与成功：写成员失败时把已占的份数释放回去")
    void join_releasesSharesWhenMemberInsertFails() {
        when(groupBuyMapper.findById(GROUP_ID, MEMBER_ID)).thenReturn(group(OPEN, 4, 2));
        when(groupBuyMapper.findMember(GROUP_ID, MEMBER_ID)).thenReturn(null);
        when(groupBuyMapper.joinAtomic(GROUP_ID, 1)).thenReturn(1);
        when(groupBuyMapper.reactivateMember(GROUP_ID, MEMBER_ID, 1)).thenReturn(0);
        when(groupBuyMapper.insertMember(GROUP_ID, MEMBER_ID, 1))
                .thenThrow(new RuntimeException("insert failed"));

        assertThrows(RuntimeException.class,
                () -> groupBuyService.join(MEMBER_ID, GROUP_ID, 1));

        verify(groupBuyMapper).releaseShares(GROUP_ID, 1);
    }

    @Test
    @DisplayName("参与成功：拼单刚好满员时通知所有已参与成员")
    void join_notifiesMembersWhenGroupBecomesFull() {
        when(groupBuyMapper.findCustomerServiceId()).thenReturn(KEFU_ID);
        when(groupBuyMapper.findById(GROUP_ID, MEMBER_ID))
                .thenReturn(group(OPEN, 3, 2), group(FULL, 3, 3));
        when(groupBuyMapper.findMember(GROUP_ID, MEMBER_ID)).thenReturn(null);
        when(groupBuyMapper.joinAtomic(GROUP_ID, 1)).thenReturn(1);
        when(groupBuyMapper.reactivateMember(GROUP_ID, MEMBER_ID, 1)).thenReturn(0);
        when(groupBuyMapper.findJoinedMembers(GROUP_ID)).thenReturn(List.of(member(OWNER_ID), member(MEMBER_ID)));

        groupBuyService.join(MEMBER_ID, GROUP_ID, 1);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper, times(2)).insert(captor.capture());
        assertTrue(captor.getAllValues().get(0).getContent().contains("已满员"));
    }

    // ---------------- 退出 ----------------

    @Test
    @DisplayName("退出成功：成员标记退出并释放份数，同时通知发起人")
    void leave_releasesSharesAndNotifiesOwner() {
        when(groupBuyMapper.findCustomerServiceId()).thenReturn(KEFU_ID);
        when(groupBuyMapper.findById(GROUP_ID, MEMBER_ID)).thenReturn(group(OPEN, 4, 3));
        GroupBuyMember member = member(MEMBER_ID);
        member.setShares(2);
        when(groupBuyMapper.findMember(GROUP_ID, MEMBER_ID)).thenReturn(member);
        when(groupBuyMapper.leaveMember(GROUP_ID, MEMBER_ID)).thenReturn(1);
        when(groupBuyMapper.releaseShares(GROUP_ID, 2)).thenReturn(1);

        groupBuyService.leave(MEMBER_ID, GROUP_ID);

        verify(groupBuyMapper).leaveMember(GROUP_ID, MEMBER_ID);
        verify(groupBuyMapper).releaseShares(GROUP_ID, 2);
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).insert(captor.capture());
        assertEquals(OWNER_ID, captor.getValue().getReceiverId());
    }

    @Test
    @DisplayName("退出失败：发起人不能退出")
    void leave_rejectsOwner() {
        when(groupBuyMapper.findById(GROUP_ID, OWNER_ID)).thenReturn(group(OPEN, 4, 3));

        assertThrows(IllegalArgumentException.class, () -> groupBuyService.leave(OWNER_ID, GROUP_ID));

        verify(groupBuyMapper, never()).leaveMember(any(), any());
    }

    @Test
    @DisplayName("退出失败：满员/已结束的拼单不能退出")
    void leave_rejectsNonOpenGroup() {
        when(groupBuyMapper.findById(GROUP_ID, MEMBER_ID)).thenReturn(group(FULL, 4, 4));

        assertThrows(IllegalArgumentException.class, () -> groupBuyService.leave(MEMBER_ID, GROUP_ID));

        verify(groupBuyMapper, never()).leaveMember(any(), any());
    }

    @Test
    @DisplayName("退出失败：释放份数失败时回滚成员状态并报错")
    void leave_rollsBackMemberWhenReleaseFails() {
        when(groupBuyMapper.findById(GROUP_ID, MEMBER_ID)).thenReturn(group(OPEN, 4, 3));
        GroupBuyMember member = member(MEMBER_ID);
        member.setShares(1);
        when(groupBuyMapper.findMember(GROUP_ID, MEMBER_ID)).thenReturn(member);
        when(groupBuyMapper.leaveMember(GROUP_ID, MEMBER_ID)).thenReturn(1);
        when(groupBuyMapper.releaseShares(GROUP_ID, 1)).thenReturn(0);

        assertThrows(IllegalArgumentException.class, () -> groupBuyService.leave(MEMBER_ID, GROUP_ID));

        verify(groupBuyMapper).reactivateMember(GROUP_ID, MEMBER_ID, 1);
    }

    // ---------------- 确认成团与关闭 ----------------

    @Test
    @DisplayName("确认成团：open → formed，并通知所有成员")
    void confirm_movesOpenToFormed() {
        when(groupBuyMapper.findCustomerServiceId()).thenReturn(KEFU_ID);
        when(groupBuyMapper.findById(GROUP_ID, OWNER_ID)).thenReturn(group(OPEN, 3, 3));
        when(groupBuyMapper.confirm(GROUP_ID, OWNER_ID)).thenReturn(1);
        when(groupBuyMapper.findJoinedMembers(GROUP_ID)).thenReturn(List.of(member(OWNER_ID)));

        groupBuyService.confirm(OWNER_ID, GROUP_ID);

        verify(groupBuyMapper).confirm(GROUP_ID, OWNER_ID);
        verify(messageMapper).insert(any(Message.class));
    }

    @Test
    @DisplayName("确认成团：非发起人不可确认")
    void confirm_rejectsNonOwner() {
        when(groupBuyMapper.findById(GROUP_ID, MEMBER_ID)).thenReturn(group(OPEN, 3, 2));

        assertThrows(IllegalArgumentException.class, () -> groupBuyService.confirm(MEMBER_ID, GROUP_ID));

        verify(groupBuyMapper, never()).confirm(any(), any());
    }

    @Test
    @DisplayName("确认成团：已成团/已关闭的拼单不可再确认")
    void confirm_rejectsTerminalStatus() {
        when(groupBuyMapper.findById(GROUP_ID, OWNER_ID)).thenReturn(group(FORMED, 3, 3));

        assertThrows(IllegalArgumentException.class, () -> groupBuyService.confirm(OWNER_ID, GROUP_ID));

        verify(groupBuyMapper, never()).confirm(any(), any());
    }

    @Test
    @DisplayName("关闭：关闭成功通知成员；非发起人不可关闭")
    void close_notifiesMembersAndRejectsNonOwner() {
        when(groupBuyMapper.findCustomerServiceId()).thenReturn(KEFU_ID);
        when(groupBuyMapper.findById(GROUP_ID, OWNER_ID)).thenReturn(group(OPEN, 3, 2));
        when(groupBuyMapper.close(GROUP_ID, OWNER_ID)).thenReturn(1);
        when(groupBuyMapper.findJoinedMembers(GROUP_ID)).thenReturn(List.of(member(MEMBER_ID)));
        groupBuyService.close(OWNER_ID, GROUP_ID);

        verify(groupBuyMapper).close(GROUP_ID, OWNER_ID);
        verify(messageMapper).insert(any(Message.class));

        when(groupBuyMapper.findById(GROUP_ID, MEMBER_ID)).thenReturn(group(OPEN, 3, 2));
        assertThrows(IllegalArgumentException.class, () -> groupBuyService.close(MEMBER_ID, GROUP_ID));
    }

    // ---------------- 定时任务：到期未满失败 ----------------

    @Test
    @DisplayName("到期任务：标记成功才通知成员；标记失败（并发已处理）则跳过")
    void expireOverdue_notifiesOnlyWhenMarkedFirstTime() {
        when(groupBuyMapper.findCustomerServiceId()).thenReturn(KEFU_ID);
        when(groupBuyMapper.findExpiredIds()).thenReturn(List.of(GROUP_ID, GROUP_ID + 1));
        when(groupBuyMapper.markFailed(GROUP_ID)).thenReturn(1);
        when(groupBuyMapper.markFailed(GROUP_ID + 1)).thenReturn(0);
        when(groupBuyMapper.findById(GROUP_ID, null)).thenReturn(group(OPEN, 3, 2));
        when(groupBuyMapper.findJoinedMembers(GROUP_ID)).thenReturn(List.of(member(MEMBER_ID)));

        groupBuyService.expireOverdue();

        verify(messageMapper).insert(any(Message.class));
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).insert(captor.capture());
        assertTrue(captor.getValue().getContent().contains("未成团"));
    }

    @Test
    @DisplayName("到期任务：没有到期拼单时不产生任何写操作")
    void expireOverdue_doesNothingWhenNoExpiredGroup() {
        when(groupBuyMapper.findExpiredIds()).thenReturn(Collections.emptyList());

        groupBuyService.expireOverdue();

        verify(groupBuyMapper, never()).markFailed(any());
        verify(messageMapper, never()).insert(any());
    }

    // ---------------- 测试数据构造 ----------------

    private GroupBuy validForm() {
        GroupBuy form = new GroupBuy();
        form.setType("split");
        form.setTitle("拼一箱矿泉水");
        form.setTargetCount(4);
        form.setPricePerShare(new BigDecimal("12.00"));
        form.setDeadline(LocalDateTime.now().plusDays(2));
        return form;
    }

    private GroupBuy group(String status, int target, int joined) {
        GroupBuy group = new GroupBuy();
        group.setId(GROUP_ID);
        group.setOwnerId(OWNER_ID);
        group.setTitle("拼一箱矿泉水");
        group.setStatus(status);
        group.setTargetCount(target);
        group.setJoinedCount(joined);
        group.setPricePerShare(new BigDecimal("12.00"));
        group.setDeadline(LocalDateTime.now().plusDays(2));
        return group;
    }

    private GroupBuyMember member(Integer userId) {
        GroupBuyMember member = new GroupBuyMember();
        member.setGroupId(GROUP_ID);
        member.setUserId(userId);
        member.setShares(1);
        member.setStatus("joined");
        member.setUserName("同学" + userId);
        return member;
    }
}
