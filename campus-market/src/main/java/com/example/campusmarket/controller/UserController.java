package com.example.campusmarket.controller;

import com.example.campusmarket.common.LoginAttemptLimiter;
import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.User;
import com.example.campusmarket.service.UserService;
import com.example.campusmarket.vo.UserStatsVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private LoginAttemptLimiter loginAttemptLimiter;

    // 注册接口
    @PostMapping("/register")
    public Result<String> register(@RequestBody User user) {
        String msg = userService.register(user);
        if ("注册成功".equals(msg)) {
            return Result.success(msg);
        }
        return Result.error(msg);
    }

    // 登录接口
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody User user, HttpServletRequest request) {
        String username = user.getUsername() == null ? "" : user.getUsername().trim();
        // 同一来源 + 账号连续失败会在这里被挡住，避免密码被批量尝试
        String attemptKey = request.getRemoteAddr() + "|" + username;
        if (loginAttemptLimiter.isBlocked(attemptKey)) {
            return Result.error("登录失败次数过多，请 5 分钟后再试");
        }
        Map<String, Object> result = userService.login(user.getUsername(), user.getPassword());
        if ((Boolean) result.get("success")) {
            loginAttemptLimiter.clear(attemptKey);
            return Result.success(result);
        }
        loginAttemptLimiter.recordFailure(attemptKey);
        return Result.error((String) result.get("msg"));
    }

    // 个人中心数据概览（仅本人，userId 取自 JWT）
    @GetMapping("/stats")
    public Result<UserStatsVO> stats() {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        return Result.success(userService.getStats(uid));
    }

    // 根据ID查询用户信息
    @GetMapping("/{id}")
    public Result<User> findById(@PathVariable Integer id) {
        User user = userService.findById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(null); // 不返回密码
        return Result.success(user);
    }

    // 搜索用户（按昵称或学号模糊匹配，结果包含自己，排除封禁账号），需登录
    @GetMapping("/search")
    public Result<PageResult<User>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        return Result.success(userService.search(keyword, pageNum, pageSize));
    }

    // 修改个人信息（强制修改本人，忽略前端传入的 id）
    @PutMapping("/update")
    public Result<String> update(@RequestBody User user) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("未登录");
        }
        user.setId(uid);
        return Result.success(userService.update(user));
    }
}