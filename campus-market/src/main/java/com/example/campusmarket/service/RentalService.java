package com.example.campusmarket.service;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.entity.Message;
import com.example.campusmarket.entity.RentalItem;
import com.example.campusmarket.entity.RentalOrder;
import com.example.campusmarket.mapper.MessageMapper;
import com.example.campusmarket.mapper.RentalMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 租赁专区：短期租赁、假期寄存转租、教材学期租。
 * 流程：租客下单(pending) → 出租方确认(renting) → 租客申请归还(returning) → 出租方核销(completed)。
 * 押金仅登记，不做真实资金流；到期与逾期通过站内消息提醒双方。
 */
@Service
public class RentalService {

    private static final Set<String> TYPES = Set.of("daily", "weekly", "storage", "textbook");
    private static final Set<String> CATEGORIES = Set.of("vehicle", "digital", "photo", "formal",
            "camping", "dorm", "textbook", "storage", "other");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("M月d日");

    @Autowired
    private RentalMapper rentalMapper;

    @Autowired
    private MessageMapper messageMapper;

    /* ==================== 物品 ==================== */

    public PageResult<RentalItem> list(String type, String category, String keyword,
                                       Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 30);
        String typeFilter = normalizeType(type);
        String categoryFilter = trim(category);
        String keywordFilter = trim(keyword);
        List<RentalItem> list = rentalMapper.findItems(typeFilter, categoryFilter, keywordFilter, p[2], p[1]);
        return PageResult.of(list, rentalMapper.countItems(typeFilter, categoryFilter, keywordFilter), p[0], p[1]);
    }

    public RentalItem detail(Integer id) {
        RentalItem item = rentalMapper.findItemById(id);
        if (item == null) {
            throw new IllegalArgumentException("租赁物品不存在");
        }
        return item;
    }

    public RentalItem publish(Integer uid, RentalItem form) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        RentalItem item = new RentalItem();
        item.setOwnerId(uid);
        item.setTitle(required(form.getTitle(), 100, "请填写物品标题，最多 100 字"));
        item.setDescription(optional(form.getDescription(), 600, "物品说明最多 600 字"));

        String type = normalizeType(form.getRentalType());
        if (type == null || !TYPES.contains(type)) {
            throw new IllegalArgumentException("请选择正确的租赁类型");
        }
        item.setRentalType(type);
        item.setPriceUnit(unitFor(type));

        String category = trim(form.getCategory());
        if (category == null || !CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("请选择正确的物品品类");
        }
        item.setCategory(category);

        BigDecimal price = form.getPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0
                || price.compareTo(new BigDecimal("99999")) > 0) {
            throw new IllegalArgumentException("租金需大于 0 且不超过 99999");
        }
        item.setPrice(price.setScale(2, RoundingMode.HALF_UP));

        BigDecimal deposit = form.getDeposit() == null ? BigDecimal.ZERO : form.getDeposit();
        if (deposit.compareTo(BigDecimal.ZERO) < 0 || deposit.compareTo(new BigDecimal("99999")) > 0) {
            throw new IllegalArgumentException("押金需在 0-99999 之间");
        }
        item.setDeposit(deposit.setScale(2, RoundingMode.HALF_UP));

        item.setLocation(optional(form.getLocation(), 100, "交接地点最多 100 字"));
        item.setCoverImage(optional(form.getCoverImage(), 255, "封面图地址过长"));

        // 教材专属租赁关联 textbook
        if ("textbook".equals(type) && form.getTextbookId() != null) {
            item.setTextbookId(form.getTextbookId());
        }

        if ("storage".equals(type)) {
            if (form.getAutoOfflineDate() == null) {
                throw new IllegalArgumentException("假期寄存请填写开学自动下架日期");
            }
            if (form.getAutoOfflineDate().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("自动下架日期不能早于今天");
            }
            item.setAutoOfflineDate(form.getAutoOfflineDate());
        }

        rentalMapper.insertItem(item);
        return rentalMapper.findItemById(item.getId());
    }

    /* ==================== 订单 ==================== */

    public RentalOrder createOrder(Integer uid, RentalOrder form) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        if (form == null || form.getItemId() == null) {
            throw new IllegalArgumentException("请选择要租用的物品");
        }
        RentalItem item = rentalMapper.findItemById(form.getItemId());
        if (item == null || !"available".equals(item.getStatus())) {
            throw new IllegalArgumentException("该物品暂时不可租");
        }
        if (uid.equals(item.getOwnerId())) {
            throw new IllegalArgumentException("不能租用自己的物品");
        }
        if (form.getStartDate() == null || form.getEndDate() == null) {
            throw new IllegalArgumentException("请选择租期起止日期");
        }
        LocalDate start = form.getStartDate();
        LocalDate end = form.getEndDate();
        if (start.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("租期开始日期不能早于今天");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("租期结束日期不能早于开始日期");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > 365) {
            throw new IllegalArgumentException("单次租期不能超过 365 天");
        }

        RentalOrder order = new RentalOrder();
        order.setOrderNo(generateOrderNo());
        order.setItemId(item.getId());
        order.setRenterId(uid);
        order.setOwnerId(item.getOwnerId());
        order.setRentalType(item.getRentalType());
        order.setStartDate(start);
        order.setEndDate(end);
        order.setRentAmount(calcRent(item, days));
        order.setDeposit(item.getDeposit());
        order.setStatus("pending");
        order.setRemark(optional(form.getRemark(), 255, "备注最多 255 字"));
        order.setHandoverNote(optional(form.getHandoverNote(), 255, "交接说明最多 255 字"));

        if (rentalMapper.lockItem(item.getId()) == 0) {
            throw new IllegalArgumentException("该物品刚被他人下单，请刷新后选择其他物品");
        }
        try {
            rentalMapper.insertOrder(order);
        } catch (RuntimeException e) {
            rentalMapper.releaseItem(item.getId());
            throw e;
        }
        notifyOwnerNewOrder(order, item);
        return rentalMapper.findOrderById(order.getId());
    }

    public PageResult<RentalOrder> myOrders(Integer uid, String role, String status,
                                            Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 30);
        String statusFilter = trim(status);
        boolean lender = "lender".equals(role);
        List<RentalOrder> list = lender
                ? rentalMapper.findOwnerOrders(uid, statusFilter, p[2], p[1])
                : rentalMapper.findRenterOrders(uid, statusFilter, p[2], p[1]);
        long total = lender
                ? rentalMapper.countOwnerOrders(uid, statusFilter)
                : rentalMapper.countRenterOrders(uid, statusFilter);
        return PageResult.of(list, total, p[0], p[1]);
    }

    public RentalOrder confirm(Integer uid, Integer id) {
        RentalOrder order = mustExist(id);
        if (!uid.equals(order.getOwnerId())) {
            throw new IllegalArgumentException("只有出租方可以确认出租");
        }
        if (!"pending".equals(order.getStatus())) {
            throw new IllegalArgumentException("该订单已处理，不能重复确认");
        }
        if (rentalMapper.confirmOrder(id, uid) == 0) {
            throw new IllegalArgumentException("订单状态已变化，请刷新后重试");
        }
        notify(order.getRenterId(), "出租方已确认您租用的《" + order.getItemTitle()
                + "》，租期 " + formatDate(order.getStartDate()) + " 至 " + formatDate(order.getEndDate())
                + "，请按约定完成交接。订单号：" + order.getOrderNo());
        return rentalMapper.findOrderById(id);
    }

    public RentalOrder applyReturn(Integer uid, Integer id) {
        RentalOrder order = mustExist(id);
        if (!uid.equals(order.getRenterId())) {
            throw new IllegalArgumentException("只有租客可以发起归还");
        }
        if (!"renting".equals(order.getStatus())) {
            throw new IllegalArgumentException("当前订单不在租用中，无法归还");
        }
        if (rentalMapper.applyReturn(id, uid) == 0) {
            throw new IllegalArgumentException("订单状态已变化，请刷新后重试");
        }
        notify(order.getOwnerId(), "租客已发起归还《" + order.getItemTitle()
                + "》，请核对物品后确认收回。订单号：" + order.getOrderNo());
        return rentalMapper.findOrderById(id);
    }

    public RentalOrder complete(Integer uid, Integer id) {
        RentalOrder order = mustExist(id);
        if (!uid.equals(order.getOwnerId())) {
            throw new IllegalArgumentException("只有出租方可以确认收回");
        }
        if (!"returning".equals(order.getStatus())) {
            throw new IllegalArgumentException("租客尚未发起归还，暂不能核销");
        }
        if (rentalMapper.completeOrder(id, uid) == 0) {
            throw new IllegalArgumentException("订单状态已变化，请刷新后重试");
        }
        rentalMapper.releaseItem(order.getItemId());
        notify(order.getRenterId(), "出租方已确认收回《" + order.getItemTitle()
                + "》，本次租赁完成，押金 " + money(order.getDeposit())
                + " 元请与出租方线下结清。订单号：" + order.getOrderNo());
        return rentalMapper.findOrderById(id);
    }

    public String cancel(Integer uid, Integer id) {
        RentalOrder order = mustExist(id);
        if (!uid.equals(order.getRenterId()) && !uid.equals(order.getOwnerId())) {
            throw new IllegalArgumentException("只能取消自己参与的租赁订单");
        }
        if (!"pending".equals(order.getStatus())) {
            throw new IllegalArgumentException("只有待确认的订单可以取消");
        }
        if (rentalMapper.cancelOrder(id, uid) == 0) {
            throw new IllegalArgumentException("订单状态已变化，请刷新后重试");
        }
        rentalMapper.releaseItem(order.getItemId());
        Integer other = uid.equals(order.getRenterId()) ? order.getOwnerId() : order.getRenterId();
        notify(other, "《" + order.getItemTitle() + "》的租赁申请已取消。订单号：" + order.getOrderNo());
        return "租赁订单已取消";
    }

    /* ==================== 管理端 ==================== */

    public PageResult<RentalOrder> adminOrders(String status, String type, boolean overdue,
                                               Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        String statusFilter = trim(status);
        String typeFilter = normalizeType(type);
        List<RentalOrder> list = rentalMapper.findAdminOrders(statusFilter, typeFilter, overdue, p[2], p[1]);
        long total = rentalMapper.countAdminOrders(statusFilter, typeFilter, overdue);
        return PageResult.of(list, total, p[0], p[1]);
    }

    /* ==================== 定时任务：到期/逾期提醒 + 寄存自动下架 ==================== */

    @Scheduled(initialDelay = 90_000, fixedDelay = 6 * 60 * 60 * 1000)
    public void remindDueAndOverdue() {
        for (RentalOrder order : rentalMapper.findDueSoonOrders()) {
            if (rentalMapper.markDueReminded(order.getId()) == 0) {
                continue;
            }
            notify(order.getRenterId(), "您租用的《" + order.getItemTitle() + "》将于 "
                    + formatDate(order.getEndDate()) + " 到期，请按时归还。订单号：" + order.getOrderNo());
            notify(order.getOwnerId(), "您出租的《" + order.getItemTitle() + "》将于 "
                    + formatDate(order.getEndDate()) + " 到期，请提醒租客按时归还。订单号：" + order.getOrderNo());
        }
        for (RentalOrder order : rentalMapper.findOverdueOrders()) {
            if (rentalMapper.markOverdueReminded(order.getId()) == 0) {
                continue;
            }
            int overdueDays = Math.max(1, order.getOverdueDays() == null ? 1 : order.getOverdueDays());
            notify(order.getRenterId(), "您租用的《" + order.getItemTitle() + "》已逾期 "
                    + overdueDays + " 天，请尽快归还。订单号：" + order.getOrderNo());
            notify(order.getOwnerId(), "您出租的《" + order.getItemTitle() + "》已逾期 "
                    + overdueDays + " 天，请及时联系租客收回。订单号：" + order.getOrderNo());
        }
    }

    @Scheduled(initialDelay = 120_000, fixedDelay = 6 * 60 * 60 * 1000)
    public void offlineExpiredStorage() {
        rentalMapper.offlineExpiredStorage();
    }

    /* ==================== 内部工具 ==================== */

    private RentalOrder mustExist(Integer id) {
        RentalOrder order = rentalMapper.findOrderById(id);
        if (order == null) {
            throw new IllegalArgumentException("租赁订单不存在");
        }
        return order;
    }

    private BigDecimal calcRent(RentalItem item, long days) {
        BigDecimal price = item.getPrice() == null ? BigDecimal.ZERO : item.getPrice();
        if ("weekly".equals(item.getRentalType())) {
            long weeks = (days + 6) / 7;
            return price.multiply(BigDecimal.valueOf(weeks)).setScale(2, RoundingMode.HALF_UP);
        }
        if ("textbook".equals(item.getRentalType())) {
            // 教材按学期整租，租金为标价
            return price.setScale(2, RoundingMode.HALF_UP);
        }
        return price.multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeType(String type) {
        String value = trim(type);
        if (value == null || "all".equals(value)) {
            return null;
        }
        return value;
    }

    private String unitFor(String type) {
        if ("weekly".equals(type)) {
            return "week";
        }
        if ("textbook".equals(type)) {
            return "semester";
        }
        return "day";
    }

    private String generateOrderNo() {
        String time = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "R" + time + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private void notifyOwnerNewOrder(RentalOrder order, RentalItem item) {
        notify(order.getOwnerId(), "您发布的《" + item.getTitle() + "》收到新的租赁申请，租期 "
                + formatDate(order.getStartDate()) + " 至 " + formatDate(order.getEndDate())
                + "，请及时确认。订单号：" + order.getOrderNo());
    }

    private void notify(Integer receiverId, String content) {
        if (receiverId == null) {
            return;
        }
        try {
            Integer kefuId = rentalMapper.findCustomerServiceId();
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
            System.err.println("租赁提醒发送失败：" + e.getMessage());
        }
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DATE_FMT);
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
