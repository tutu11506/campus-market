package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.Invoice;
import com.example.campusmarket.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/invoice")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    /** 申请开票（电子普通发票） */
    @PostMapping("/apply")
    public Result<Invoice> apply(@RequestBody Map<String, Object> body) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        Integer orderId = null;
        Object o = body.get("orderId");
        if (o instanceof Number) {
            orderId = ((Number) o).intValue();
        } else if (o instanceof String && !((String) o).isBlank()) {
            try {
                orderId = Integer.valueOf(((String) o).trim());
            } catch (NumberFormatException ignore) {
                orderId = null;
            }
        }
        return Result.success(invoiceService.apply(uid, orderId,
                str(body, "titleType"), str(body, "title"), str(body, "taxNo"), str(body, "email")));
    }

    /** 我的发票 */
    @GetMapping("/my")
    public Result<PageResult<Invoice>> my(@RequestParam(defaultValue = "1") int pageNum,
                                          @RequestParam(defaultValue = "10") int pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(invoiceService.myInvoices(uid, pageNum, pageSize));
    }

    /** 按订单查发票（页面判断是否已开票） */
    @GetMapping("/order/{orderId}")
    public Result<Invoice> byOrder(@PathVariable Integer orderId) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        Invoice invoice = invoiceService.byOrder(orderId);
        if (invoice == null) {
            return Result.success(null);
        }
        if (!uid.equals(invoice.getUserId()) && !SecurityUtils.isAdmin()) {
            return Result.error("无权查看该发票");
        }
        return Result.success(invoice);
    }

    /** 发票详情 */
    @GetMapping("/{id}")
    public Result<Invoice> detail(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        Invoice invoice = invoiceService.detail(id);
        if (invoice == null) {
            return Result.error("发票不存在");
        }
        if (!uid.equals(invoice.getUserId()) && !SecurityUtils.isAdmin()) {
            return Result.error("无权查看该发票");
        }
        return Result.success(invoice);
    }

    private String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }
}
