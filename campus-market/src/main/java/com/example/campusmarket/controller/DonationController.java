package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.DonationRecord;
import com.example.campusmarket.service.DonationService;
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
@RequestMapping("/donation")
public class DonationController {

    @Autowired
    private DonationService donationService;

    /** 公益社团与捐赠统计（公开） */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(donationService.overview());
    }

    /** 登记捐赠 */
    @PostMapping("/records")
    public Result<DonationRecord> create(@RequestBody DonationRecord form) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(donationService.create(uid, form));
    }

    /** 我的捐赠记录 */
    @GetMapping("/my")
    public Result<PageResult<DonationRecord>> my(@RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(donationService.my(uid, pageNum, pageSize));
    }

    /** 取消尚未交接的本人捐赠登记 */
    @PostMapping("/records/{id}/cancel")
    public Result<String> cancel(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(donationService.cancel(uid, id));
    }
}
