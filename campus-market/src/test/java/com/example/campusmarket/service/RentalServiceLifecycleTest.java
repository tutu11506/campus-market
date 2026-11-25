package com.example.campusmarket.service;

import com.example.campusmarket.entity.Message;
import com.example.campusmarket.entity.RentalItem;
import com.example.campusmarket.entity.RentalOrder;
import com.example.campusmarket.mapper.MessageMapper;
import com.example.campusmarket.mapper.RentalMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 租赁状态机回归：pending 待确认 → renting 租用中 → returning 待收回 → completed 已完成。
 * 覆盖原子锁货、写单失败回滚、取消释放、租金计算与到期/逾期提醒。
 * Mapper 全部 Mock，不依赖真实数据库。
 */
@ExtendWith(MockitoExtension.class)
class RentalServiceLifecycleTest {

    private static final Integer ITEM_ID = 20;
    private static final Integer OWNER_ID = 4;
    private static final Integer RENTER_ID = 5;
    private static final Integer ORDER_ID = 200;
    private static final Integer KEFU_ID = 99;

    private static final String AVAILABLE = "available";
    private static final String RENTED = "rented";
    private static final String PENDING = "pending";
    private static final String RENTING = "renting";
    private static final String RETURNING = "returning";
    private static final String COMPLETED = "completed";
    private static final String CANCELLED = "cancelled";

    @Mock
    private RentalMapper rentalMapper;
    @Mock
    private MessageMapper messageMapper;

    @InjectMocks
    private RentalService rentalService;

    // ---------------- 下单：原子锁货 ----------------

    @Test
    @DisplayName("下单成功：先锁定物品再落单，租金按天计算，状态为待确认")
    void createOrder_locksItemThenPersistsOrder() {
        RentalItem item = item(AVAILABLE, "daily", "10.00");
        when(rentalMapper.findItemById(ITEM_ID)).thenReturn(item);
        when(rentalMapper.lockItem(ITEM_ID)).thenReturn(1);
        when(rentalMapper.findOrderById(any())).thenReturn(order(ORDER_ID, PENDING));

        RentalOrder form = orderRequest();
        RentalOrder result = rentalService.createOrder(RENTER_ID, form);

        assertEquals(PENDING, result.getStatus());

        // 锁货必须早于写单，保证并发下只有一人能租到
        InOrder inOrder = inOrder(rentalMapper);
        inOrder.verify(rentalMapper).lockItem(ITEM_ID);
        inOrder.verify(rentalMapper).insertOrder(any(RentalOrder.class));

        ArgumentCaptor<RentalOrder> captor = ArgumentCaptor.forClass(RentalOrder.class);
        verify(rentalMapper).insertOrder(captor.capture());
        RentalOrder saved = captor.getValue();
        assertEquals(RENTER_ID, saved.getRenterId());
        assertEquals(OWNER_ID, saved.getOwnerId());
        assertEquals(PENDING, saved.getStatus());
        // 3 天 × 10 元 = 30 元，金额一律由服务端计算
        assertEquals(new BigDecimal("30.00"), saved.getRentAmount());
        assertEquals(new BigDecimal("50.00"), saved.getDeposit());
        assertTrue(saved.getOrderNo().startsWith("R"));
    }

    @Test
    @DisplayName("下单失败：物品不可租（已出租/已下架）时不尝试锁定")
    void createOrder_rejectsUnavailableItem() {
        when(rentalMapper.findItemById(ITEM_ID)).thenReturn(item(RENTED, "daily", "10.00"));

        assertThrows(IllegalArgumentException.class,
                () -> rentalService.createOrder(RENTER_ID, orderRequest()));

        verify(rentalMapper, never()).lockItem(any());
        verify(rentalMapper, never()).insertOrder(any());
    }

    @Test
    @DisplayName("下单失败：不能租用自己发布的物品")
    void createOrder_rejectsRentingOwnItem() {
        when(rentalMapper.findItemById(ITEM_ID)).thenReturn(item(AVAILABLE, "daily", "10.00"));

        assertThrows(IllegalArgumentException.class,
                () -> rentalService.createOrder(OWNER_ID, orderRequest()));

        verify(rentalMapper, never()).lockItem(any());
    }

    @Test
    @DisplayName("下单失败：开始日期早于今天被拒绝，不做任何写操作")
    void createOrder_rejectsPastStartDate() {
        when(rentalMapper.findItemById(ITEM_ID)).thenReturn(item(AVAILABLE, "daily", "10.00"));

        RentalOrder form = orderRequest();
        form.setStartDate(LocalDate.now().minusDays(1));

        assertThrows(IllegalArgumentException.class,
                () -> rentalService.createOrder(RENTER_ID, form));

        verify(rentalMapper, never()).lockItem(any());
    }

    @Test
    @DisplayName("下单失败：结束日期早于开始日期被拒绝")
    void createOrder_rejectsEndBeforeStart() {
        when(rentalMapper.findItemById(ITEM_ID)).thenReturn(item(AVAILABLE, "daily", "10.00"));

        RentalOrder form = orderRequest();
        form.setEndDate(form.getStartDate().minusDays(1));

        assertThrows(IllegalArgumentException.class,
                () -> rentalService.createOrder(RENTER_ID, form));

        verify(rentalMapper, never()).lockItem(any());
    }

    @Test
    @DisplayName("下单失败：单次租期超过 365 天被拒绝")
    void createOrder_rejectsTooLongTerm() {
        when(rentalMapper.findItemById(ITEM_ID)).thenReturn(item(AVAILABLE, "daily", "10.00"));

        RentalOrder form = orderRequest();
        form.setStartDate(LocalDate.now().plusDays(1));
        form.setEndDate(form.getStartDate().plusDays(366));

        assertThrows(IllegalArgumentException.class,
                () -> rentalService.createOrder(RENTER_ID, form));

        verify(rentalMapper, never()).lockItem(any());
    }

    @Test
    @DisplayName("下单失败：并发抢单锁定失败时直接报错，不写订单")
    void createOrder_whenLockFails_doesNotInsertOrder() {
        when(rentalMapper.findItemById(ITEM_ID)).thenReturn(item(AVAILABLE, "daily", "10.00"));
        when(rentalMapper.lockItem(ITEM_ID)).thenReturn(0);

        assertThrows(IllegalArgumentException.class,
                () -> rentalService.createOrder(RENTER_ID, orderRequest()));

        verify(rentalMapper, never()).insertOrder(any());
        verify(messageMapper, never()).insert(any());
    }

    @Test
    @DisplayName("下单失败：写单抛异常时释放已锁定的物品，避免物品被永久占用")
    void createOrder_releasesLockWhenInsertFails() {
        when(rentalMapper.findItemById(ITEM_ID)).thenReturn(item(AVAILABLE, "daily", "10.00"));
        when(rentalMapper.lockItem(ITEM_ID)).thenReturn(1);
        when(rentalMapper.insertOrder(any())).thenThrow(new RuntimeException("insert failed"));

        assertThrows(RuntimeException.class,
                () -> rentalService.createOrder(RENTER_ID, orderRequest()));

        verify(rentalMapper).releaseItem(ITEM_ID);
    }

    @Test
    @DisplayName("租金计算：按周租 8 天计 2 周，不足一周按一周")
    void createOrder_weeklyRentRoundsUpToWholeWeeks() {
        RentalItem item = item(AVAILABLE, "weekly", "20.00");
        when(rentalMapper.findItemById(ITEM_ID)).thenReturn(item);
        when(rentalMapper.lockItem(ITEM_ID)).thenReturn(1);
        when(rentalMapper.findOrderById(any())).thenReturn(order(ORDER_ID, PENDING));

        RentalOrder form = orderRequest();
        form.setStartDate(LocalDate.now().plusDays(1));
        form.setEndDate(form.getStartDate().plusDays(7)); // 共 8 天 = 2 周
        rentalService.createOrder(RENTER_ID, form);

        ArgumentCaptor<RentalOrder> captor = ArgumentCaptor.forClass(RentalOrder.class);
        verify(rentalMapper).insertOrder(captor.capture());
        assertEquals(new BigDecimal("40.00"), captor.getValue().getRentAmount());
    }

    // ---------------- 待确认 → 租用中 ----------------

    @Test
    @DisplayName("出租方确认：pending → renting，并通知租客")
    void confirm_movesPendingToRenting() {
        stubKefu();
        when(rentalMapper.findOrderById(ORDER_ID)).thenReturn(order(ORDER_ID, PENDING));
        when(rentalMapper.confirmOrder(ORDER_ID, OWNER_ID)).thenReturn(1);

        rentalService.confirm(OWNER_ID, ORDER_ID);

        verify(rentalMapper).confirmOrder(ORDER_ID, OWNER_ID);
        ArgumentCaptor<Message> msg = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).insert(msg.capture());
        assertEquals(RENTER_ID, msg.getValue().getReceiverId());
        assertTrue(msg.getValue().getContent().contains("已确认"));
    }

    @Test
    @DisplayName("出租方确认：非出租方不可确认")
    void confirm_rejectsNonOwner() {
        when(rentalMapper.findOrderById(ORDER_ID)).thenReturn(order(ORDER_ID, PENDING));

        assertThrows(IllegalArgumentException.class, () -> rentalService.confirm(RENTER_ID, ORDER_ID));

        verify(rentalMapper, never()).confirmOrder(any(), any());
    }

    @Test
    @DisplayName("出租方确认：已处理过的订单不可重复确认")
    void confirm_rejectsNonPendingOrder() {
        when(rentalMapper.findOrderById(ORDER_ID)).thenReturn(order(ORDER_ID, RENTING));

        assertThrows(IllegalArgumentException.class, () -> rentalService.confirm(OWNER_ID, ORDER_ID));

        verify(rentalMapper, never()).confirmOrder(any(), any());
    }

    @Test
    @DisplayName("出租方确认：条件更新失败（状态已被并发改变）时报错且不发消息")
    void confirm_whenAtomicUpdateFails_throws() {
        when(rentalMapper.findOrderById(ORDER_ID)).thenReturn(order(ORDER_ID, PENDING));
        when(rentalMapper.confirmOrder(ORDER_ID, OWNER_ID)).thenReturn(0);

        assertThrows(IllegalArgumentException.class, () -> rentalService.confirm(OWNER_ID, ORDER_ID));

        verify(messageMapper, never()).insert(any());
    }

    // ---------------- 租用中 → 待收回 → 已完成 ----------------

    @Test
    @DisplayName("租客申请归还：renting → returning，并通知出租方")
    void applyReturn_movesRentingToReturning() {
        stubKefu();
        when(rentalMapper.findOrderById(ORDER_ID)).thenReturn(order(ORDER_ID, RENTING));
        when(rentalMapper.applyReturn(ORDER_ID, RENTER_ID)).thenReturn(1);

        rentalService.applyReturn(RENTER_ID, ORDER_ID);

        verify(rentalMapper).applyReturn(ORDER_ID, RENTER_ID);
        ArgumentCaptor<Message> msg = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).insert(msg.capture());
        assertEquals(OWNER_ID, msg.getValue().getReceiverId());
    }

    @Test
    @DisplayName("租客申请归还：非租客不可发起")
    void applyReturn_rejectsNonRenter() {
        when(rentalMapper.findOrderById(ORDER_ID)).thenReturn(order(ORDER_ID, RENTING));

        assertThrows(IllegalArgumentException.class, () -> rentalService.applyReturn(OWNER_ID, ORDER_ID));

        verify(rentalMapper, never()).applyReturn(any(), any());
    }

    @Test
    @DisplayName("租客申请归还：未处于租用中时不可归还")
    void applyReturn_rejectsOrderNotRenting() {
        when(rentalMapper.findOrderById(ORDER_ID)).thenReturn(order(ORDER_ID, PENDING));

        assertThrows(IllegalArgumentException.class, () -> rentalService.applyReturn(RENTER_ID, ORDER_ID));

        verify(rentalMapper, never()).applyReturn(any(), any());
    }

    @Test
    @DisplayName("出租方核销：returning → completed，并释放物品")
    void complete_movesReturningToCompletedAndReleasesItem() {
        stubKefu();
        when(rentalMapper.findOrderById(ORDER_ID)).thenReturn(order(ORDER_ID, RETURNING));
        when(rentalMapper.completeOrder(ORDER_ID, OWNER_ID)).thenReturn(1);

        rentalService.complete(OWNER_ID, ORDER_ID);

        verify(rentalMapper).completeOrder(ORDER_ID, OWNER_ID);
        verify(rentalMapper).releaseItem(ITEM_ID);
        verify(messageMapper).insert(any());
    }

    @Test
    @DisplayName("出租方核销：租客尚未申请归还时不可核销")
    void complete_rejectsOrderNotReturning() {
        when(rentalMapper.findOrderById(ORDER_ID)).thenReturn(order(ORDER_ID, RENTING));

        assertThrows(IllegalArgumentException.class, () -> rentalService.complete(OWNER_ID, ORDER_ID));

        verify(rentalMapper, never()).completeOrder(any(), any());
        verify(rentalMapper, never()).releaseItem(any());
    }

    // ---------------- 取消 ----------------

    @Test
    @DisplayName("取消：待确认订单取消后释放物品，并通知对方")
    void cancel_releasesItemAndNotifiesOther() {
        stubKefu();
        when(rentalMapper.findOrderById(ORDER_ID)).thenReturn(order(ORDER_ID, PENDING));
        when(rentalMapper.cancelOrder(ORDER_ID, RENTER_ID)).thenReturn(1);

        assertEquals("租赁订单已取消", rentalService.cancel(RENTER_ID, ORDER_ID));

        verify(rentalMapper).releaseItem(ITEM_ID);
        ArgumentCaptor<Message> msg = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).insert(msg.capture());
        assertEquals(OWNER_ID, msg.getValue().getReceiverId());
    }

    @Test
    @DisplayName("取消：租用中的订单不可取消，也不能释放物品")
    void cancel_rejectsRentingOrder() {
        when(rentalMapper.findOrderById(ORDER_ID)).thenReturn(order(ORDER_ID, RENTING));

        assertThrows(IllegalArgumentException.class, () -> rentalService.cancel(RENTER_ID, ORDER_ID));

        verify(rentalMapper, never()).cancelOrder(any(), any());
        verify(rentalMapper, never()).releaseItem(any());
    }

    @Test
    @DisplayName("取消：与订单无关的用户不可取消")
    void cancel_rejectsStranger() {
        when(rentalMapper.findOrderById(ORDER_ID)).thenReturn(order(ORDER_ID, PENDING));

        assertThrows(IllegalArgumentException.class, () -> rentalService.cancel(777, ORDER_ID));

        verify(rentalMapper, never()).cancelOrder(any(), any());
    }

    // ---------------- 到期 / 逾期提醒 ----------------

    @Test
    @DisplayName("到期提醒：标记成功才发消息，标记失败（已提醒过）则跳过")
    void remindDueAndOverdue_notifiesOnlyWhenMarkedFirstTime() {
        stubKefu();
        RentalOrder first = order(ORDER_ID, RENTING);
        RentalOrder second = order(ORDER_ID + 1, RENTING);
        when(rentalMapper.findDueSoonOrders()).thenReturn(List.of(first, second));
        when(rentalMapper.markDueReminded(ORDER_ID)).thenReturn(1);
        when(rentalMapper.markDueReminded(ORDER_ID + 1)).thenReturn(0);
        when(rentalMapper.findOverdueOrders()).thenReturn(Collections.emptyList());

        rentalService.remindDueAndOverdue();

        // 只有第一条：租客 + 出租方各一条
        verify(messageMapper, times(2)).insert(any(Message.class));
        verify(rentalMapper, never()).markOverdueReminded(any());
    }

    @Test
    @DisplayName("逾期提醒：按逾期天数分别通知双方")
    void remindDueAndOverdue_notifiesOverdueDays() {
        stubKefu();
        RentalOrder overdue = order(ORDER_ID, RETURNING);
        overdue.setOverdueDays(3);
        when(rentalMapper.findDueSoonOrders()).thenReturn(Collections.emptyList());
        when(rentalMapper.findOverdueOrders()).thenReturn(List.of(overdue));
        when(rentalMapper.markOverdueReminded(ORDER_ID)).thenReturn(1);

        rentalService.remindDueAndOverdue();

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper, times(2)).insert(captor.capture());
        assertTrue(captor.getAllValues().get(0).getContent().contains("逾期 3 天"));
        assertEquals(RENTER_ID, captor.getAllValues().get(0).getReceiverId());
        assertEquals(OWNER_ID, captor.getAllValues().get(1).getReceiverId());
    }

    // ---------------- 测试数据构造 ----------------

    private void stubKefu() {
        when(rentalMapper.findCustomerServiceId()).thenReturn(KEFU_ID);
    }

    private RentalOrder orderRequest() {
        RentalOrder form = new RentalOrder();
        form.setItemId(ITEM_ID);
        form.setStartDate(LocalDate.now().plusDays(1));
        form.setEndDate(form.getStartDate().plusDays(2));
        return form;
    }

    private RentalOrder order(Integer id, String status) {
        RentalOrder order = new RentalOrder();
        order.setId(id);
        order.setOrderNo("R202606010001abcd");
        order.setItemId(ITEM_ID);
        order.setRenterId(RENTER_ID);
        order.setOwnerId(OWNER_ID);
        order.setItemTitle("九成新自行车");
        order.setStartDate(LocalDate.now().plusDays(1));
        order.setEndDate(LocalDate.now().plusDays(3));
        order.setRentAmount(new BigDecimal("30.00"));
        order.setDeposit(new BigDecimal("50.00"));
        order.setStatus(status);
        return order;
    }

    private RentalItem item(String status, String type, String price) {
        RentalItem item = new RentalItem();
        item.setId(ITEM_ID);
        item.setTitle("九成新自行车");
        item.setOwnerId(OWNER_ID);
        item.setRentalType(type);
        item.setPrice(new BigDecimal(price));
        item.setPriceUnit("weekly".equals(type) ? "week" : "day");
        item.setDeposit(new BigDecimal("50.00"));
        item.setStatus(status);
        return item;
    }
}
