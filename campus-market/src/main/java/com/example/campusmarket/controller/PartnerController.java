package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.PartnerPost;
import com.example.campusmarket.service.PartnerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/partners")
public class PartnerController {

    @Autowired
    private PartnerService partnerService;

    /** 搭子列表（公开） */
    @GetMapping
    public Result<PageResult<PartnerPost>> list(@RequestParam(required = false) String type,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "12") Integer pageSize) {
        return Result.success(partnerService.list(type, status, keyword,
                SecurityUtils.getCurrentUserId(), pageNum, pageSize));
    }

    /** 搭子详情（公开） */
    @GetMapping("/{id}")
    public Result<PartnerPost> detail(@PathVariable Integer id) {
        return Result.success(partnerService.detail(id, SecurityUtils.getCurrentUserId()));
    }

    /** 发起搭子（登录） */
    @PostMapping
    public Result<PartnerPost> create(@RequestBody PartnerPost form) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(partnerService.create(uid, form));
    }

    /** 加入搭子（登录） */
    @PostMapping("/{id}/join")
    public Result<PartnerPost> join(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(partnerService.join(uid, id));
    }

    /** 退出搭子（登录） */
    @PostMapping("/{id}/leave")
    public Result<PartnerPost> leave(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(partnerService.leave(uid, id));
    }

    /** 发起人取消搭子（登录） */
    @PostMapping("/{id}/cancel")
    public Result<PartnerPost> cancel(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(partnerService.cancel(uid, id));
    }

    /** 我的搭子：role=owner 我发起的 / member 我参与的（登录） */
    @GetMapping("/my")
    public Result<PageResult<PartnerPost>> my(@RequestParam(defaultValue = "owner") String role,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(partnerService.myList(uid, role, status, pageNum, pageSize));
    }
}
