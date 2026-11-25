package com.example.campusmarket.service;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.entity.Goods;
import com.example.campusmarket.entity.User;
import com.example.campusmarket.mapper.GoodsMapper;
import com.example.campusmarket.mapper.OrderMapper;
import com.example.campusmarket.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private OrderMapper orderMapper;

    // ========== 用户管理 ==========
    public PageResult<User> getAllUsers(Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        List<User> users = userMapper.findAllPage(p[2], p[1]);
        // 管理员列表也不得向前端返回密码哈希，避免敏感信息泄露
        users.forEach(u -> u.setPassword(null));
        return PageResult.of(users, userMapper.countAll(), p[0], p[1]);
    }

    public String updateUserStatus(Integer id, String status) {
        userMapper.updateStatus(id, status);
        return "状态更新成功";
    }

    // ========== 商品管理 ==========
    public PageResult<Goods> getAllGoods(Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        List<Goods> list = goodsMapper.findAllForAdminPage(p[2], p[1]);
        return PageResult.of(list, goodsMapper.countAllForAdmin(), p[0], p[1]);
    }

    public String offlineGoods(Integer id) {
        goodsMapper.updateStatus(id, "off_shelf");
        return "商品已下架";
    }

    public String deleteGoods(Integer id) {
        goodsMapper.delete(id);
        return "商品已删除";
    }

    // ========== 数据统计 ==========
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        int totalOrders = orderMapper.countAll();
        int completedOrders = orderMapper.countCompletedOrders();
        // 用户总数
        stats.put("userCount", userMapper.countAll());
        // 商品总数
        stats.put("goodsCount", goodsMapper.countAllForAdmin());
        // 订单总数
        stats.put("orderCount", totalOrders);
        // 已完成订单数
        stats.put("completedCount", completedOrders);
        // 成交率（百分比，保留 1 位小数）
        double rate = totalOrders == 0 ? 0 : (completedOrders * 100.0 / totalOrders);
        stats.put("completionRate", Math.round(rate * 10.0) / 10.0);
        // 总交易额（仅已完成订单）
        BigDecimal totalAmount = orderMapper.sumTotalAmount();
        stats.put("totalAmount", totalAmount);
        // 近 7 天活跃卖家（有成交的卖家数）
        stats.put("activeSellers", orderMapper.countActiveSellersDays(7));
        return stats;
    }

    // ========== 数据看板：近 7 天趋势 + 分类分布 ==========
    // period: "day"（近 7 天）| "week"（近 8 周）
    public Map<String, Object> getDashboard(String period) {
        Map<String, Map<String, Object>> userRaw = new HashMap<>();
        for (Map<String, Object> row : userMapper.countRegistrationsByDay()) {
            userRaw.put(String.valueOf(row.get("date")), row);
        }

        List<Map<String, Object>> orderRaw;
        if ("week".equalsIgnoreCase(period)) {
            orderRaw = orderMapper.countOrdersByWeek(8);
        } else {
            orderRaw = orderMapper.countOrdersByDay();
        }

        Map<String, Map<String, Object>> orderMap = new HashMap<>();
        for (Map<String, Object> row : orderRaw) {
            orderMap.put(String.valueOf(row.get("date")), row);
        }

        List<Map<String, Object>> userTrend = new ArrayList<>();
        List<Map<String, Object>> orderTrend = new ArrayList<>();

        if ("week".equalsIgnoreCase(period)) {
            // 周视图：按周一填充 8 周
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1L);
            for (int i = 7; i >= 0; i--) {
                String date = monday.minusWeeks(i).format(formatter);
                Map<String, Object> userRow = new HashMap<>();
                Map<String, Object> userData = userRaw.get(date);
                userRow.put("date", date);
                userRow.put("count", userData == null ? 0 : userData.get("count"));
                userTrend.add(userRow);

                Map<String, Object> orderRow = new HashMap<>();
                Map<String, Object> orderData = orderMap.get(date);
                orderRow.put("date", date);
                orderRow.put("count", orderData == null ? 0 : orderData.get("count"));
                orderRow.put("amount", orderData == null ? 0 : orderData.get("amount"));
                orderTrend.add(orderRow);
            }
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
            for (int i = 6; i >= 0; i--) {
                String date = LocalDate.now().minusDays(i).format(formatter);

                Map<String, Object> userRow = new HashMap<>();
                Map<String, Object> userData = userRaw.get(date);
                userRow.put("date", date);
                userRow.put("count", userData == null ? 0 : userData.get("count"));
                userTrend.add(userRow);

                Map<String, Object> orderRow = new HashMap<>();
                Map<String, Object> orderData = orderMap.get(date);
                orderRow.put("date", date);
                orderRow.put("count", orderData == null ? 0 : orderData.get("count"));
                orderRow.put("amount", orderData == null ? 0 : orderData.get("amount"));
                orderTrend.add(orderRow);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("period", "week".equalsIgnoreCase(period) ? "week" : "day");
        data.put("userTrend", userTrend);
        data.put("orderTrend", orderTrend);
        data.put("categoryStats", goodsMapper.countGoodsByCategory());
        return data;
    }
}