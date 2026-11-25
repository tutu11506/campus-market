package com.example.campusmarket.service;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.entity.Invoice;
import com.example.campusmarket.entity.Order;
import com.example.campusmarket.mapper.InvoiceMapper;
import com.example.campusmarket.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 电子发票：已完成订单可申请开票，演示环境提交后立即开票。
 */
@Service
public class InvoiceService {

    @Autowired
    private InvoiceMapper invoiceMapper;

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 申请开票
     */
    public Invoice apply(Integer uid, Integer orderId, String titleType, String title,
                         String taxNo, String email) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("请选择订单");
        }
        Order order = orderMapper.findById(orderId);
        if (order == null || !uid.equals(order.getBuyerId())) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!"completed".equals(order.getStatus())) {
            throw new IllegalArgumentException("订单完成后才能申请开票");
        }
        if ("returned".equals(order.getRefundStatus())) {
            throw new IllegalArgumentException("已退货的订单不能开票");
        }
        if (invoiceMapper.findByOrderId(orderId) != null) {
            throw new IllegalArgumentException("该订单已开票，可在「我的发票」查看");
        }

        boolean company = "company".equals(titleType);
        String name = title == null ? "" : title.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException(company ? "请填写企业名称" : "请填写发票抬头");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("发票抬头最多 100 个字");
        }
        String tax = taxNo == null ? null : taxNo.trim();
        if (company) {
            if (tax == null || tax.isEmpty()) {
                throw new IllegalArgumentException("企业抬头需要填写税号");
            }
            if (tax.length() > 32) {
                throw new IllegalArgumentException("税号最多 32 位");
            }
        } else {
            tax = null;
        }
        String mail = email == null ? null : email.trim();
        if (mail != null && !mail.isEmpty()
                && !mail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        if (mail != null && mail.isEmpty()) {
            mail = null;
        }

        BigDecimal amount = order.getPrice() == null ? BigDecimal.ZERO : order.getPrice();
        if (order.getDiscountAmount() != null) {
            amount = amount.subtract(order.getDiscountAmount());
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            amount = BigDecimal.ZERO;
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);

        Invoice invoice = new Invoice();
        invoice.setOrderId(orderId);
        invoice.setOrderNo(order.getOrderNo());
        invoice.setUserId(uid);
        invoice.setTitleType(company ? "company" : "personal");
        invoice.setTitle(name);
        invoice.setTaxNo(tax);
        invoice.setEmail(mail);
        invoice.setAmount(amount);
        invoice.setInvoiceNo(generateInvoiceNo());
        invoice.setStatus("issued");
        invoice.setIssueTime(new Date());
        invoiceMapper.insert(invoice);
        return invoiceMapper.findDetailById(invoice.getId());
    }

    /** 发票号码：FP + 年月日时分秒 + 3 位随机数 */
    private String generateInvoiceNo() {
        String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        int rand = ThreadLocalRandom.current().nextInt(1000);
        return "FP" + time + String.format("%03d", rand);
    }

    public PageResult<Invoice> myInvoices(Integer uid, int pageNum, int pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 30);
        List<Invoice> list = invoiceMapper.findByUser(uid, p[2], p[1]);
        return PageResult.of(list, invoiceMapper.countByUser(uid), p[0], p[1]);
    }

    public Invoice detail(Integer id) {
        return id == null ? null : invoiceMapper.findDetailById(id);
    }

    public Invoice byOrder(Integer orderId) {
        Invoice invoice = invoiceMapper.findByOrderId(orderId);
        return invoice == null ? null : invoiceMapper.findDetailById(invoice.getId());
    }
}
