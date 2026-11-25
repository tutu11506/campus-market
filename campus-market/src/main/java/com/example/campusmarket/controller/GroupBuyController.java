package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.GroupBuy;
import com.example.campusmarket.service.GroupBuyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/group-buys")
public class GroupBuyController {

    @Autowired
    private GroupBuyService groupBuyService;

    /** 拼单列表（公开，登录后可看到自己已参与份数） */
    @GetMapping
    public Result<PageResult<GroupBuy>> list(@RequestParam(required = false) String type,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "12") Integer pageSize) {
        return Result.success(groupBuyService.list(type, status, keyword,
                SecurityUtils.getCurrentUserId(), pageNum, pageSize));
    }

    /** 我发起的、可被搭子关联的进行中拼单（登录） */
    @GetMapping("/linkable")
    public Result<List<GroupBuy>> linkable() {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(groupBuyService.linkable(uid));
    }

    /** 拼单详情（公开） */
    @GetMapping("/{id}")
    public Result<GroupBuy> detail(@PathVariable Integer id) {
        return Result.success(groupBuyService.detail(id, SecurityUtils.getCurrentUserId()));
    }

    /** 发起拼单（登录） */
    @PostMapping
    public Result<GroupBuy> create(@RequestBody GroupBuy form) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(groupBuyService.create(uid, form));
    }

    /** 参与拼单（登录） */
    @PostMapping("/{id}/join")
    public Result<GroupBuy> join(@PathVariable Integer id,
                                 @RequestBody(required = false) JoinForm form) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(groupBuyService.join(uid, id, form == null ? 1 : form.getShares()));
    }

    /** 退出拼单（登录） */
    @PostMapping("/{id}/leave")
    public Result<GroupBuy> leave(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(groupBuyService.leave(uid, id));
    }

    /** 发起人确认成团（登录） */
    @PostMapping("/{id}/confirm")
    public Result<GroupBuy> confirm(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(groupBuyService.confirm(uid, id));
    }

    /** 发起人关闭拼单（登录） */
    @PostMapping("/{id}/close")
    public Result<GroupBuy> close(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(groupBuyService.close(uid, id));
    }

    /** 我的拼单：role=owner 我发起的 / member 我参与的（登录） */
    @GetMapping("/my")
    public Result<PageResult<GroupBuy>> my(@RequestParam(defaultValue = "owner") String role,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(groupBuyService.myList(uid, role, status, pageNum, pageSize));
    }

    /** 参与份数表单 */
    public static class JoinForm {
        private Integer shares;

        public Integer getShares() {
            return shares;
        }

        public void setShares(Integer shares) {
            this.shares = shares;
        }
    }
}
