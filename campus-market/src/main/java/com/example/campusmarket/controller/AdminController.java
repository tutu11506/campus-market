package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.DonationRecord;
import com.example.campusmarket.entity.Goods;
import com.example.campusmarket.entity.RentalOrder;
import com.example.campusmarket.entity.User;
import com.example.campusmarket.service.AdminService;
import com.example.campusmarket.service.DonationService;
import com.example.campusmarket.service.RentalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private DonationService donationService;

    @Autowired
    private RentalService rentalService;

    // ========== 用户管理 ==========
    @GetMapping("/user/list")
    public Result<PageResult<User>> getUserList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(adminService.getAllUsers(pageNum, pageSize));
    }

    @PutMapping("/user/status")
    public Result<String> updateUserStatus(@RequestParam Integer id, @RequestParam String status) {
        return Result.success(adminService.updateUserStatus(id, status));
    }

    // ========== 商品管理 ==========
    @GetMapping("/goods/list")
    public Result<PageResult<Goods>> getGoodsList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(adminService.getAllGoods(pageNum, pageSize));
    }

    @PutMapping("/goods/offline/{id}")
    public Result<String> offlineGoods(@PathVariable Integer id) {
        return Result.success(adminService.offlineGoods(id));
    }

    @DeleteMapping("/goods/delete/{id}")
    public Result<String> deleteGoods(@PathVariable Integer id) {
        return Result.success(adminService.deleteGoods(id));
    }

    // ========== 公益捐赠接收 ==========
    @GetMapping("/donations")
    public Result<PageResult<DonationRecord>> getDonationList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String itemType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(donationService.adminList(status, itemType, pageNum, pageSize));
    }

    @PutMapping("/donations/{id}/receive")
    public Result<DonationRecord> receiveDonation(@PathVariable Integer id,
                                                  @RequestBody DonationRecord body) {
        return Result.success(donationService.receive(
                SecurityUtils.getCurrentUserId(), id, body.getReceivedQuantity()));
    }

    // ========== 租赁记录 ==========
    @GetMapping("/rentals")
    public Result<PageResult<RentalOrder>> getRentalList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "false") boolean overdue,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(rentalService.adminOrders(status, type, overdue, pageNum, pageSize));
    }

    // ========== 数据统计 ==========
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(adminService.getStatistics());
    }

    // ========== 数据看板（趋势 + 分类分布） ==========
    // period: "day"（近 7 天，默认）| "week"（近 8 周）
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard(
            @RequestParam(defaultValue = "day") String period) {
        return Result.success(adminService.getDashboard(period));
    }

    // ========== CSV 导出（全量统计 + 近 7/8 周趋势） ==========
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(defaultValue = "day") String period) {
        StringBuilder sb = new StringBuilder();
        sb.append("校园二手市场 · 数据看板导出\n");
        sb.append("导出时间, ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())).append("\n\n");

        Map<String, Object> stats = adminService.getStatistics();
        sb.append("=== 汇总指标 ===\n");
        sb.append("用户总数, ").append(stats.get("userCount")).append("\n");
        sb.append("商品总数, ").append(stats.get("goodsCount")).append("\n");
        sb.append("订单总数, ").append(stats.get("orderCount")).append("\n");
        sb.append("已完成订单, ").append(stats.get("completedCount")).append("\n");
        sb.append("成交率(%), ").append(stats.get("completionRate")).append("\n");
        sb.append("总交易额(元), ").append(stats.get("totalAmount")).append("\n");
        sb.append("近7天活跃卖家, ").append(stats.get("activeSellers")).append("\n\n");

        Map<String, Object> dashboard = adminService.getDashboard(period);
        String periodLabel = "week".equalsIgnoreCase(period) ? "周" : "日";

        sb.append("=== 注册用户趋势（按").append(periodLabel).append("） ===\n");
        sb.append("日期,新增注册用户\n");
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> userTrend = (java.util.List<Map<String, Object>>) dashboard.get("userTrend");
        for (Map<String, Object> row : userTrend) {
            sb.append(row.get("date")).append(",").append(row.get("count")).append("\n");
        }
        sb.append("\n");

        sb.append("=== 订单与成交趋势（按").append(periodLabel).append("） ===\n");
        sb.append("日期,订单数,成交额(元)\n");
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> orderTrend = (java.util.List<Map<String, Object>>) dashboard.get("orderTrend");
        for (Map<String, Object> row : orderTrend) {
            sb.append(row.get("date")).append(",").append(row.get("count")).append(",").append(row.get("amount")).append("\n");
        }
        sb.append("\n");

        sb.append("=== 分类商品分布 ===\n");
        sb.append("分类,商品数\n");
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> catStats = (java.util.List<Map<String, Object>>) dashboard.get("categoryStats");
        for (Map<String, Object> row : catStats) {
            sb.append(row.get("name")).append(",").append(row.get("count")).append("\n");
        }

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        // 加 BOM 让 Excel 正确识别 UTF-8
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] finalBytes = new byte[bom.length + bytes.length];
        System.arraycopy(bom, 0, finalBytes, 0, bom.length);
        System.arraycopy(bytes, 0, finalBytes, bom.length, bytes.length);

        String filename = URLEncoder.encode("campus-market-dashboard.csv", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + filename)
                .body(finalBytes);
    }
}