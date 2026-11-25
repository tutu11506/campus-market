package com.example.campusmarket.service;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.ForbiddenException;
import com.example.campusmarket.entity.Goods;
import com.example.campusmarket.entity.Message;
import com.example.campusmarket.entity.Order;
import com.example.campusmarket.entity.User;
import com.example.campusmarket.entity.UserCoupon;
import com.example.campusmarket.mapper.GoodsMapper;
import com.example.campusmarket.mapper.MessageMapper;
import com.example.campusmarket.mapper.OrderMapper;
import com.example.campusmarket.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CouponService couponService;

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_DEALING = "dealing";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_CANCELLED = "cancelled";

    private static final String GOODS_ON_SALE = "on_sale";
    private static final String GOODS_LOCKED = "locked";
    private static final String GOODS_SOLD = "sold";

    // 卖家接单时限（分钟）
    private static final int CONFIRM_TIMEOUT_MINUTES = 30;
    private static final String CUSTOMER_SERVICE_NAME = "校园二手客服";

    // 创建订单
    @Transactional(rollbackFor = Exception.class)
    public String createOrder(Order order) {
        if (order == null || order.getGoodsId() == null) {
            return "参数错误";
        }
        // 用 mapper 只读取商品，避免下单时给浏览量 +1 造成计数污染
        Goods goods = goodsMapper.findById(order.getGoodsId());
        if (goods == null) {
            return "商品不存在";
        }
        if (!GOODS_ON_SALE.equals(goods.getStatus())) {
            return "商品已下架、交易中或已售出";
        }
        if (order.getBuyerId() == null) {
            return "请先登录";
        }
        if (order.getBuyerId().equals(goods.getSellerId())) {
            return "不能购买自己发布的商品";
        }
        // 防止重复下单：同一买家对同一商品已有进行中订单则拒绝
        Order active = orderMapper.findActiveByGoodsAndBuyer(order.getGoodsId(), order.getBuyerId());
        if (active != null) {
            return "您已有该商品的进行中订单，请勿重复下单";
        }
        // 生成唯一订单号并填充服务端可信字段（价格、卖家以服务端为准，不信任前端）
        String orderNo = "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4);
        order.setOrderNo(orderNo);
        order.setSellerId(goods.getSellerId());
        order.setPrice(goods.getPrice());
        order.setDiscountAmount(BigDecimal.ZERO);
        // 平台满减券：满足门槛时自动抵扣，用掉后会自动补发下一张
        UserCoupon coupon = couponService.findApplicable(order.getBuyerId(), goods.getPrice());
        if (coupon != null) {
            BigDecimal pay = goods.getPrice().subtract(coupon.getDiscountAmount());
            if (pay.compareTo(BigDecimal.ZERO) < 0) {
                pay = BigDecimal.ZERO;
            }
            order.setPrice(pay);
            order.setDiscountAmount(coupon.getDiscountAmount());
        }
        order.setStatus(STATUS_PENDING);
        // 下单即锁定商品：其他买家立刻无法购买，并发时只有一人能锁定成功
        if (goodsMapper.lockIfOnSale(order.getGoodsId()) == 0) {
            return "商品已被他人拍下，请刷新后重试";
        }
        orderMapper.insert(order);
        // 核销优惠券并补发下一张
        couponService.consume(coupon, order.getId());
        // 客服给卖家发送下单提醒
        notifySellerOrdered(goods, order);
        return "下单成功";
    }

    // 订单详情
    public Order findById(Integer id) {
        return orderMapper.findById(id);
    }

    // 我买到的（分页）
    public PageResult<Order> findBuyerOrders(Integer userId, Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        List<Order> list = orderMapper.findBuyerOrdersPage(userId, p[2], p[1]);
        return PageResult.of(list, orderMapper.countBuyerOrders(userId), p[0], p[1]);
    }

    // 我卖出的（分页）
    public PageResult<Order> findSellerOrders(Integer userId, Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        List<Order> list = orderMapper.findSellerOrdersPage(userId, p[2], p[1]);
        return PageResult.of(list, orderMapper.countSellerOrders(userId), p[0], p[1]);
    }

    // 模拟支付：仅买家可支付自己的待确认订单，支付后卖家才可以接单
    @Transactional(rollbackFor = Exception.class)
    public String payOrder(Integer id, Integer buyerId) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            return "订单不存在";
        }
        if (!order.getBuyerId().equals(buyerId)) {
            throw new ForbiddenException("无权操作此订单");
        }
        if (!STATUS_PENDING.equals(order.getStatus())) {
            return "当前订单状态不可支付";
        }
        if (order.getPayTime() != null) {
            return "订单已支付，请勿重复支付";
        }
        // 支付即托管：资金先进入平台，生成 6 位线下自提核销码
        String verifyCode = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        if (orderMapper.payOrder(id, buyerId, verifyCode) == 0) {
            return "支付失败，订单状态已变更";
        }
        return "支付成功，资金已由平台担保托管";
    }

    /**
     * 卖家线下自提核销：买家出示核销码，卖家输入后订单完成，托管资金结算给卖家。
     */
    @Transactional(rollbackFor = Exception.class)
    public String verifyOrder(Integer id, Integer sellerId, String code) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            return "订单不存在";
        }
        if (!sellerId.equals(order.getSellerId())) {
            throw new ForbiddenException("仅卖家可核销订单");
        }
        if (code == null || code.trim().isEmpty()) {
            return "请输入买家出示的核销码";
        }
        if (!STATUS_PENDING.equals(order.getStatus()) && !STATUS_DEALING.equals(order.getStatus())) {
            return "当前订单状态不可核销";
        }
        if (order.getPayTime() == null) {
            return "买家尚未支付，无法核销";
        }
        if (order.getVerifyCode() == null || !order.getVerifyCode().equals(code.trim())) {
            return "核销码不正确，请和买家确认";
        }
        orderMapper.updateStatus(id, STATUS_COMPLETED);
        int rows = goodsMapper.updateStatus(order.getGoodsId(), GOODS_SOLD);
        if (rows == 0) {
            throw new IllegalStateException("商品状态更新失败，订单已回滚");
        }
        orderMapper.settleOrder(id);
        return "核销成功，交易完成，资金已结算到你的账户";
    }

    // 卖家确认订单（开始交易）：仅 pending 可确认
    public String confirmOrder(Integer id) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            return "订单不存在";
        }
        if (!STATUS_PENDING.equals(order.getStatus())) {
            return "当前订单状态不可确认";
        }
        // 未支付的订单不允许接单，保证「支付 → 接单」不跳步
        if (order.getPayTime() == null) {
            return "买家尚未支付，无法接单";
        }
        orderMapper.updateStatus(id, STATUS_DEALING);
        return "已确认交易";
    }

    // 完成订单（交易完成）：仅 dealing 可完成，并把商品置为已售；两步需在同一事务
    @Transactional(rollbackFor = Exception.class)
    public String completeOrder(Integer id) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            return "订单不存在";
        }
        if (!STATUS_DEALING.equals(order.getStatus())) {
            return "仅交易中的订单可完成";
        }
        if (order.getPayTime() == null) {
            return "订单未支付，无法完成";
        }
        orderMapper.updateStatus(id, STATUS_COMPLETED);
        int rows = goodsMapper.updateStatus(order.getGoodsId(), GOODS_SOLD);
        if (rows == 0) {
            // 抛出异常触发事务回滚，避免订单已完成但商品仍在售
            throw new IllegalStateException("商品状态更新失败，订单已回滚");
        }
        // 买家确认收货即视为核验无误，托管资金结算给卖家
        orderMapper.settleOrder(id);
        return "交易已完成，资金已结算给卖家";
    }

    // 取消订单：仅 pending / dealing 可取消，已完成/已取消不可再取消
    @Transactional(rollbackFor = Exception.class)
    public String cancelOrder(Integer id) {
        Order order = orderMapper.findById(id);
        if (order == null) {
            return "订单不存在";
        }
        if (!STATUS_PENDING.equals(order.getStatus()) && !STATUS_DEALING.equals(order.getStatus())) {
            return "当前订单状态不可取消";
        }
        orderMapper.updateStatus(id, STATUS_CANCELLED);
        // 已托管的资金原路退回买家
        orderMapper.refundEscrow(id);
        // 释放商品：回到在售，避免商品一直停留在交易中
        releaseGoods(order.getGoodsId());
        return "订单已取消";
    }

    // 定时扫描：卖家 30 分钟内未接单的订单自动取消，并释放商品
    @Scheduled(fixedDelay = 60000, initialDelay = 20000)
    @Transactional(rollbackFor = Exception.class)
    public void autoCancelTimeoutOrders() {
        List<Order> timeoutOrders = orderMapper.findTimeoutPendingOrders(CONFIRM_TIMEOUT_MINUTES);
        for (Order order : timeoutOrders) {
            // 条件更新，避免卖家刚好点接单时被误取消
            if (orderMapper.cancelIfPending(order.getId()) == 0) {
                continue;
            }
            releaseGoods(order.getGoodsId());
            Goods goods = goodsMapper.findById(order.getGoodsId());
            String title = goods != null ? goods.getTitle() : "商品";
            sendCustomerServiceMessage(order.getSellerId(),
                    "您的商品《" + title + "》超过30分钟未确认接单，订单已自动取消，商品已重新上架。订单号：" + order.getOrderNo(),
                    order.getGoodsId());
            sendCustomerServiceMessage(order.getBuyerId(),
                    "您购买的《" + title + "》因卖家超时未接单，订单已自动取消。订单号：" + order.getOrderNo(),
                    order.getGoodsId());
        }
    }

    // 商品仍处于锁定时恢复为在售
    private void releaseGoods(Integer goodsId) {
        if (goodsId == null) {
            return;
        }
        Goods goods = goodsMapper.findById(goodsId);
        if (goods != null && GOODS_LOCKED.equals(goods.getStatus())) {
            goodsMapper.updateStatus(goodsId, GOODS_ON_SALE);
        }
    }

    // 商品被拍下时，由客服给卖家发送提醒
    private void notifySellerOrdered(Goods goods, Order order) {
        sendCustomerServiceMessage(order.getSellerId(),
                "您的商品《" + goods.getTitle() + "》已被买家拍下，请在30分钟内确认接单，超时订单将自动取消。订单号：" + order.getOrderNo(),
                goods.getId());
    }

    // 发送客服消息（失败不影响下单/取消主流程）
    private void sendCustomerServiceMessage(Integer receiverId, String content, Integer goodsId) {
        if (receiverId == null) {
            return;
        }
        try {
            Integer kefuId = getOrCreateCustomerServiceId();
            if (kefuId == null || kefuId.equals(receiverId)) {
                return;
            }
            Message message = new Message();
            message.setSenderId(kefuId);
            message.setReceiverId(receiverId);
            message.setContent(content);
            message.setMsgType("text");
            message.setGoodsId(goodsId);
            messageMapper.insert(message);
        } catch (Exception e) {
            System.err.println("客服消息发送失败：" + e.getMessage());
        }
    }

    // 获取客服账号，不存在则自动创建
    private Integer getOrCreateCustomerServiceId() {
        User kefu = userMapper.findByUsername(CUSTOMER_SERVICE_NAME);
        if (kefu != null) {
            return kefu.getId();
        }
        synchronized (OrderService.class) {
            kefu = userMapper.findByUsername(CUSTOMER_SERVICE_NAME);
            if (kefu != null) {
                return kefu.getId();
            }
            User user = new User();
            user.setStudentNo("KEFU0001");
            user.setUsername(CUSTOMER_SERVICE_NAME);
            user.setPassword(passwordEncoder.encode("kefu_" + UUID.randomUUID()));
            userMapper.insert(user);
            User created = userMapper.findByUsername(CUSTOMER_SERVICE_NAME);
            return created == null ? null : created.getId();
        }
    }
}
