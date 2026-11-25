package com.example.campusmarket.service;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.entity.DonationOrganization;
import com.example.campusmarket.entity.DonationRecord;
import com.example.campusmarket.mapper.DonationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 公益捐赠：登记与实收分开统计，管理员依据社团回执确认实收。
 */
@Service
public class DonationService {

    private static final Set<String> ITEM_TYPES = Set.of("clothes", "books");
    private static final Set<String> CONDITIONS = Set.of("new", "good", "fair");
    private static final Set<String> HANDOVER_METHODS = Set.of("dropoff", "pickup");

    @Autowired
    private DonationMapper donationMapper;

    public Map<String, Object> overview() {
        long registeredClothes = 0;
        long registeredBooks = 0;
        long receivedClothes = 0;
        long receivedBooks = 0;
        for (Map<String, Object> row : donationMapper.summarizeByType()) {
            long registered = number(row.get("registeredQuantity"));
            long received = number(row.get("receivedQuantity"));
            if ("clothes".equals(row.get("itemType"))) {
                registeredClothes = registered;
                receivedClothes = received;
            } else if ("books".equals(row.get("itemType"))) {
                registeredBooks = registered;
                receivedBooks = received;
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("registeredClothes", registeredClothes);
        stats.put("registeredBooks", registeredBooks);
        stats.put("receivedClothes", receivedClothes);
        stats.put("receivedBooks", receivedBooks);
        stats.put("donorCount", donationMapper.countDonors());
        stats.put("recordCount", donationMapper.countValidRecords());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("organizations", donationMapper.findActiveOrganizations());
        data.put("stats", stats);
        data.put("organizationStats", donationMapper.summarizeByOrganization());
        return data;
    }

    public DonationRecord create(Integer uid, DonationRecord form) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        String itemType = form.getItemType() == null ? "" : form.getItemType().trim();
        if (!ITEM_TYPES.contains(itemType)) {
            throw new IllegalArgumentException("请选择衣物或书籍");
        }
        String itemName = required(form.getItemName(), 100, "请填写物品名称，最多 100 字");
        if (form.getQuantity() == null || form.getQuantity() < 1 || form.getQuantity() > 999) {
            throw new IllegalArgumentException("捐赠数量需为 1-999 的整数");
        }
        String condition = form.getConditionLevel() == null ? "" : form.getConditionLevel().trim();
        if (!CONDITIONS.contains(condition)) {
            throw new IllegalArgumentException("请选择正确的物品成色");
        }
        String handoverMethod = form.getHandoverMethod() == null ? "" : form.getHandoverMethod().trim();
        if (!HANDOVER_METHODS.contains(handoverMethod)) {
            throw new IllegalArgumentException("请选择正确的交接方式");
        }
        String handoverDetail = required(form.getHandoverDetail(), 255, "请填写交接时间、地点或宿舍说明");
        String contact = required(form.getContact(), 50, "请填写联系方式，最多 50 字");
        String remark = optional(form.getRemark(), 500, "备注最多 500 字");

        DonationOrganization org = donationMapper.findActiveOrganizationById(form.getOrgId());
        if (org == null) {
            throw new IllegalArgumentException("接收社团不存在或已停用");
        }
        boolean accepts = Arrays.stream(org.getAcceptTypes().split(","))
                .map(String::trim).anyMatch(itemType::equals);
        if (!accepts) {
            throw new IllegalArgumentException("该社团暂不接收此类物资");
        }

        DonationRecord record = new DonationRecord();
        record.setUserId(uid);
        record.setOrgId(org.getId());
        record.setItemType(itemType);
        record.setItemName(itemName);
        record.setQuantity(form.getQuantity());
        record.setConditionLevel(condition);
        record.setHandoverMethod(handoverMethod);
        record.setHandoverDetail(handoverDetail);
        record.setContact(contact);
        record.setRemark(remark);
        donationMapper.insertRecord(record);
        return donationMapper.findRecordById(record.getId());
    }

    public PageResult<DonationRecord> my(Integer uid, Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 30);
        List<DonationRecord> list = donationMapper.findMyRecords(uid, p[2], p[1]);
        return PageResult.of(list, donationMapper.countMyRecords(uid), p[0], p[1]);
    }

    public String cancel(Integer uid, Integer id) {
        DonationRecord record = donationMapper.findRecordById(id);
        if (record == null) {
            throw new IllegalArgumentException("捐赠记录不存在");
        }
        if (!uid.equals(record.getUserId())) {
            throw new IllegalArgumentException("只能取消自己的捐赠登记");
        }
        if (!"pending".equals(record.getStatus())) {
            throw new IllegalArgumentException("该记录已结束，不能取消");
        }
        if (donationMapper.cancelRecord(id, uid) == 0) {
            throw new IllegalArgumentException("记录状态已变化，请刷新后重试");
        }
        return "捐赠登记已取消";
    }

    public PageResult<DonationRecord> adminList(String status, String itemType,
                                                Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        List<DonationRecord> list = donationMapper.findAdminRecords(status, itemType, p[2], p[1]);
        return PageResult.of(list, donationMapper.countAdminRecords(status, itemType), p[0], p[1]);
    }

    public DonationRecord receive(Integer adminId, Integer id, Integer receivedQuantity) {
        if (adminId == null) {
            throw new IllegalArgumentException("请先登录");
        }
        DonationRecord record = donationMapper.findRecordById(id);
        if (record == null) {
            throw new IllegalArgumentException("捐赠记录不存在");
        }
        if (!"pending".equals(record.getStatus())) {
            throw new IllegalArgumentException("该记录已处理，不能重复确认");
        }
        if (receivedQuantity == null || receivedQuantity < 1 || receivedQuantity > record.getQuantity()) {
            throw new IllegalArgumentException("实收数量需大于 0 且不能超过登记数量");
        }
        if (donationMapper.receiveRecord(id, receivedQuantity, adminId) == 0) {
            throw new IllegalArgumentException("记录状态已变化，请刷新后重试");
        }
        return donationMapper.findRecordById(id);
    }

    private String required(String value, int max, String message) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty() || v.length() > max) {
            throw new IllegalArgumentException(message);
        }
        return v;
    }

    private String optional(String value, int max, String message) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String v = value.trim();
        if (v.length() > max) {
            throw new IllegalArgumentException(message);
        }
        return v;
    }

    private long number(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }
}
