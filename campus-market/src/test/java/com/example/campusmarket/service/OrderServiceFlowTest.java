package com.example.campusmarket.service;

import com.example.campusmarket.common.ForbiddenException;
import com.example.campusmarket.entity.Goods;
import com.example.campusmarket.entity.Message;
import com.example.campusmarket.entity.Order;
import com.example.campusmarket.entity.User;
import com.example.campusmarket.mapper.GoodsMapper;
import com.example.campusmarket.mapper.MessageMapper;
import com.example.campusmarket.mapper.OrderMapper;
import com.example.campusmarket.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 核心交易流程回归测试：下单锁货 → 卖家接单 → 30 分钟超时取消 → 取消后重新上架。
 * 只针对 OrderService 的编排与状态机做单元测试，Mapper 用 Mock 隔离，不依赖数据库。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceFlowTest {

    private static final Integer GOODS_ID = 10;
    private static final Integer SELLER_ID = 2;
    private static final Integer BUYER_ID = 3;
    private static final Integer ORDER_ID = 100;
    private static final Integer KEFU_ID = 99;
    private static final String KEFU_NAME = "校园二手客服";

    private static final String ON_SALE = "on_sale";
    private static final String LOCKED = "locked";
    private static final String SOLD = "sold";
    private static final String PENDING = "pending";
    private static final String DEALING = "dealing";
    private static final String COMPLETED = "completed";
    private static final String CANCELLED = "cancelled";

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private GoodsMapper goodsMapper;
    @Mock
    private MessageMapper messageMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CouponService couponService;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        User kefu = new User();
        kefu.setId(KEFU_ID);
        kefu.setUsername(KEFU_NAME);
        // 大部分用例都会触发客服消息，这里统一给出客服账号；个别用例不触发也不算冗余
        lenient().when(userMapper.findByUsername(KEFU_NAME)).thenReturn(kefu);
    }

    // ---------------- 下单锁货 ----------------

    @Test
    @DisplayName("下单成功：先锁定商品，再落订单，价格与卖家以服务端为准")
    void createOrder_locksGoodsAndPersistsServerSideFields() {
        Goods goods = goods(ON_SALE);
        when(goodsMapper.findById(GOODS_ID)).thenReturn(goods);
        when(orderMapper.findActiveByGoodsAndBuyer(GOODS_ID, BUYER_ID)).thenReturn(null);
        when(goodsMapper.lockIfOnSale(GOODS_ID)).thenReturn(1);

        Order request = new Order();
        request.setGoodsId(GOODS_ID);
        request.setBuyerId(BUYER_ID);
        request.setPrice(new BigDecimal("0.01")); // 前端伪造低价，必须被服务端覆盖

        String result = orderService.createOrder(request);

        assertEquals("下单成功", result);
        // 锁货必须发生在写订单之前，保证并发下只有一人能锁成功
        InOrder inOrder = inOrder(goodsMapper, orderMapper);
        inOrder.verify(goodsMapper).lockIfOnSale(GOODS_ID);
        inOrder.verify(orderMapper).insert(any(Order.class));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(captor.capture());
        Order saved = captor.getValue();
        assertEquals(PENDING, saved.getStatus());
        assertEquals(SELLER_ID, saved.getSellerId());
        assertEquals(new BigDecimal("88.00"), saved.getPrice());
        assertNotNull(saved.getOrderNo());
        assertTrue(saved.getOrderNo().startsWith("ORD"));

        // 通知卖家在 30 分钟内接单
        ArgumentCaptor<Message> msg = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).insert(msg.capture());
        assertEquals(SELLER_ID, msg.getValue().getReceiverId());
        assertEquals(KEFU_ID, msg.getValue().getSenderId());
        assertTrue(msg.getValue().getContent().contains("30分钟"));
    }

    @Test
    @DisplayName("下单失败：商品已被他人锁定，不写订单也不发消息")
    void createOrder_whenLockFails_doesNotInsertOrder() {
        when(goodsMapper.findById(GOODS_ID)).thenReturn(goods(ON_SALE));
        when(orderMapper.findActiveByGoodsAndBuyer(GOODS_ID, BUYER_ID)).thenReturn(null);
        when(goodsMapper.lockIfOnSale(GOODS_ID)).thenReturn(0);

        String result = orderService.createOrder(orderRequest());

        assertEquals("商品已被他人拍下，请刷新后重试", result);
        verify(orderMapper, never()).insert(any(Order.class));
        verifyNoInteractions(messageMapper);
    }

    @Test
    @DisplayName("下单失败：商品不在售（已锁定/已售出/已下架）时不尝试锁货")
    void createOrder_rejectsGoodsNotOnSale() {
        when(goodsMapper.findById(GOODS_ID)).thenReturn(goods(LOCKED));

        String result = orderService.createOrder(orderRequest());

        assertEquals("商品已下架、交易中或已售出", result);
        verify(goodsMapper, never()).lockIfOnSale(any());
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    @DisplayName("下单失败：不能购买自己发布的商品")
    void createOrder_rejectsBuyingOwnGoods() {
        when(goodsMapper.findById(GOODS_ID)).thenReturn(goods(ON_SALE));

        Order request = orderRequest();
        request.setBuyerId(SELLER_ID);

        String result = orderService.createOrder(request);

        assertEquals("不能购买自己发布的商品", result);
        verify(goodsMapper, never()).lockIfOnSale(any());
    }

    @Test
    @DisplayName("下单失败：同一买家已有进行中订单时不重复锁货")
    void createOrder_rejectsDuplicateActiveOrder() {
        when(goodsMapper.findById(GOODS_ID)).thenReturn(goods(ON_SALE));
        when(orderMapper.findActiveByGoodsAndBuyer(GOODS_ID, BUYER_ID)).thenReturn(order(ORDER_ID, PENDING));

        String result = orderService.createOrder(orderRequest());

        assertEquals("您已有该商品的进行中订单，请勿重复下单", result);
        verify(goodsMapper, never()).lockIfOnSale(any());
        verify(orderMapper, never()).insert(any(Order.class));
    }

    // ---------------- 模拟支付 ----------------

    @Test
    @DisplayName("支付：买家支付自己的待确认订单，写入支付时间")
    void payOrder_marksPaid() {
        Order pending = order(ORDER_ID, PENDING);
        pending.setPayTime(null);
        when(orderMapper.findById(ORDER_ID)).thenReturn(pending);
        when(orderMapper.payOrder(eq(ORDER_ID), eq(BUYER_ID), anyString())).thenReturn(1);

        String result = orderService.payOrder(ORDER_ID, BUYER_ID);

        assertEquals("支付成功，资金已由平台担保托管", result);
        verify(orderMapper).payOrder(eq(ORDER_ID), eq(BUYER_ID), anyString());
    }

    @Test
    @DisplayName("支付：非买家不可支付自己的订单")
    void payOrder_rejectsOtherUser() {
        Order pending = order(ORDER_ID, PENDING);
        pending.setPayTime(null);
        when(orderMapper.findById(ORDER_ID)).thenReturn(pending);

        assertThrows(ForbiddenException.class, () -> orderService.payOrder(ORDER_ID, 999));

        verify(orderMapper, never()).payOrder(any(), any(), any());
    }

    @Test
    @DisplayName("支付：重复支付被拒绝，不覆盖原支付时间")
    void payOrder_rejectsAlreadyPaid() {
        when(orderMapper.findById(ORDER_ID)).thenReturn(order(ORDER_ID, PENDING));

        String result = orderService.payOrder(ORDER_ID, BUYER_ID);

        assertEquals("订单已支付，请勿重复支付", result);
        verify(orderMapper, never()).payOrder(any(), any(), any());
    }

    @Test
    @DisplayName("接单：未支付的订单不允许接单，保证流程不跳步")
    void confirmOrder_rejectsUnpaidOrder() {
        Order unpaid = order(ORDER_ID, PENDING);
        unpaid.setPayTime(null);
        when(orderMapper.findById(ORDER_ID)).thenReturn(unpaid);

        String result = orderService.confirmOrder(ORDER_ID);

        assertEquals("买家尚未支付，无法接单", result);
        verify(orderMapper, never()).updateStatus(any(), any());
    }

    // ---------------- 卖家接单 ----------------

    @Test
    @DisplayName("卖家接单：pending 订单进入 dealing")
    void confirmOrder_pendingBecomesDealing() {
        when(orderMapper.findById(ORDER_ID)).thenReturn(order(ORDER_ID, PENDING));

        String result = orderService.confirmOrder(ORDER_ID);

        assertEquals("已确认交易", result);
        verify(orderMapper).updateStatus(ORDER_ID, DEALING);
    }

    @Test
    @DisplayName("卖家接单：已取消/已完成订单不可再确认")
    void confirmOrder_rejectsNonPendingOrder() {
        when(orderMapper.findById(ORDER_ID)).thenReturn(order(ORDER_ID, CANCELLED));

        String result = orderService.confirmOrder(ORDER_ID);

        assertEquals("当前订单状态不可确认", result);
        verify(orderMapper, never()).updateStatus(any(), any());
    }

    // ---------------- 30 分钟超时取消 ----------------

    @Test
    @DisplayName("超时取消：超过 30 分钟未接单的订单自动取消，商品重新上架并通知双方")
    void autoCancelTimeoutOrders_cancelsAndRelistsGoods() {
        Order timeoutOrder = order(ORDER_ID, PENDING);
        Goods goods = goods(LOCKED);
        when(orderMapper.findTimeoutPendingOrders(30)).thenReturn(List.of(timeoutOrder));
        when(orderMapper.cancelIfPending(ORDER_ID)).thenReturn(1);
        when(goodsMapper.findById(GOODS_ID)).thenReturn(goods);

        orderService.autoCancelTimeoutOrders();

        // 超时阈值就是 30 分钟
        verify(orderMapper).findTimeoutPendingOrders(30);
        verify(orderMapper).cancelIfPending(ORDER_ID);
        verify(goodsMapper).updateStatus(GOODS_ID, ON_SALE);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper, times(2)).insert(captor.capture());
        List<Message> messages = captor.getAllValues();
        assertEquals(SELLER_ID, messages.get(0).getReceiverId());
        assertTrue(messages.get(0).getContent().contains("自动取消"));
        assertTrue(messages.get(0).getContent().contains("重新上架"));
        assertEquals(BUYER_ID, messages.get(1).getReceiverId());
        assertTrue(messages.get(1).getContent().contains("超时未接单"));
    }

    @Test
    @DisplayName("超时取消：卖家已抢先在扫描前接单，则跳过取消、不释放商品")
    void autoCancelTimeoutOrders_skipsOrderConfirmedConcurrently() {
        when(orderMapper.findTimeoutPendingOrders(30)).thenReturn(List.of(order(ORDER_ID, PENDING)));
        when(orderMapper.cancelIfPending(ORDER_ID)).thenReturn(0);

        orderService.autoCancelTimeoutOrders();

        verify(goodsMapper, never()).updateStatus(any(), any());
        verifyNoInteractions(messageMapper);
    }

    @Test
    @DisplayName("超时取消：没有超时订单时不产生任何写操作")
    void autoCancelTimeoutOrders_noTimeoutOrderDoesNothing() {
        when(orderMapper.findTimeoutPendingOrders(30)).thenReturn(Collections.emptyList());

        orderService.autoCancelTimeoutOrders();

        verifyNoInteractions(goodsMapper);
        verifyNoInteractions(messageMapper);
    }

    // ---------------- 取消后重新上架 ----------------

    @Test
    @DisplayName("手动取消：订单取消后锁定中的商品回到在售")
    void cancelOrder_relistsLockedGoods() {
        when(orderMapper.findById(ORDER_ID)).thenReturn(order(ORDER_ID, DEALING));
        when(goodsMapper.findById(GOODS_ID)).thenReturn(goods(LOCKED));

        String result = orderService.cancelOrder(ORDER_ID);

        assertEquals("订单已取消", result);
        verify(orderMapper).updateStatus(ORDER_ID, CANCELLED);
        verify(goodsMapper).updateStatus(GOODS_ID, ON_SALE);
    }

    @Test
    @DisplayName("手动取消：已完成订单不可取消，商品状态不变")
    void cancelOrder_rejectsCompletedOrder() {
        when(orderMapper.findById(ORDER_ID)).thenReturn(order(ORDER_ID, COMPLETED));

        String result = orderService.cancelOrder(ORDER_ID);

        assertEquals("当前订单状态不可取消", result);
        verify(orderMapper, never()).updateStatus(any(), any());
        verify(goodsMapper, never()).updateStatus(any(), any());
    }

    @Test
    @DisplayName("手动取消：商品已售出时不会被误改回在售")
    void cancelOrder_doesNotResurrectSoldGoods() {
        when(orderMapper.findById(ORDER_ID)).thenReturn(order(ORDER_ID, PENDING));
        when(goodsMapper.findById(GOODS_ID)).thenReturn(goods(SOLD));

        String result = orderService.cancelOrder(ORDER_ID);

        assertEquals("订单已取消", result);
        verify(orderMapper).updateStatus(ORDER_ID, CANCELLED);
        verify(goodsMapper, never()).updateStatus(any(), any());
    }

    // ---------------- 交易完成 ----------------

    @Test
    @DisplayName("完成交易：dealing 订单完成并把商品置为已售")
    void completeOrder_marksGoodsSold() {
        when(orderMapper.findById(ORDER_ID)).thenReturn(order(ORDER_ID, DEALING));
        when(goodsMapper.updateStatus(GOODS_ID, SOLD)).thenReturn(1);

        String result = orderService.completeOrder(ORDER_ID);

        assertEquals("交易已完成，资金已结算给卖家", result);
        verify(orderMapper).updateStatus(ORDER_ID, COMPLETED);
        verify(goodsMapper).updateStatus(GOODS_ID, SOLD);
    }

    @Test
    @DisplayName("完成交易：商品状态更新失败时抛异常，保证订单不落成已完成")
    void completeOrder_throwsWhenGoodsUpdateFails() {
        when(orderMapper.findById(ORDER_ID)).thenReturn(order(ORDER_ID, DEALING));
        when(goodsMapper.updateStatus(GOODS_ID, SOLD)).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> orderService.completeOrder(ORDER_ID));
    }

    // ---------------- 测试数据构造 ----------------

    private Order orderRequest() {
        Order request = new Order();
        request.setGoodsId(GOODS_ID);
        request.setBuyerId(BUYER_ID);
        return request;
    }

    private Order order(Integer id, String status) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNo("ORD202606010001abcd");
        order.setGoodsId(GOODS_ID);
        order.setBuyerId(BUYER_ID);
        order.setSellerId(SELLER_ID);
        order.setPrice(new BigDecimal("88.00"));
        order.setStatus(status);
        // 默认视为已支付；需要验证“未支付”分支的用例会显式置空
        order.setPayTime(new java.util.Date());
        return order;
    }

    private Goods goods(String status) {
        Goods goods = new Goods();
        goods.setId(GOODS_ID);
        goods.setTitle("九成新自行车");
        goods.setSellerId(SELLER_ID);
        goods.setPrice(new BigDecimal("88.00"));
        goods.setStatus(status);
        return goods;
    }
}
