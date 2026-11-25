package com.example.campusmarket.service;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.config.JwtUtil;
import com.example.campusmarket.entity.User;
import com.example.campusmarket.mapper.UserMapper;
import com.example.campusmarket.vo.UserStatsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // 注册
    public String register(User user) {
        if (user == null) {
            return "注册信息不能为空";
        }
        String username = user.getUsername();
        String password = user.getPassword();
        String studentNo = user.getStudentNo();
        // 参数校验
        if (username == null || username.trim().isEmpty()) {
            return "昵称不能为空";
        }
        if (username.length() < 2 || username.length() > 20) {
            return "昵称长度需在 2-20 之间";
        }
        if (password == null || password.length() < 6) {
            return "密码长度不能少于 6 位";
        }
        if (studentNo == null || studentNo.trim().isEmpty()) {
            return "学号不能为空";
        }
        if (user.getPhone() != null && !user.getPhone().isEmpty()
                && !user.getPhone().matches("^1[3-9]\\d{9}$")) {
            return "手机号格式不正确";
        }
        // 1. 检查用户名是否已存在
        if (userMapper.findByUsername(username) != null) {
            return "用户名已被占用";
        }
        // 2. 检查学号是否已存在
        if (userMapper.findByStudentNo(studentNo) != null) {
            return "该学号已注册";
        }
        // 3. 对密码进行 BCrypt 加密后再存储
        String encodedPassword = passwordEncoder.encode(password);
        user.setPassword(encodedPassword);
        // 4. 插入数据库
        userMapper.insert(user);
        return "注册成功";
    }

    // 登录
    public Map<String, Object> login(String username, String password) {
        Map<String, Object> result = new HashMap<>();
        User user = userMapper.findByUsername(username);

        if (user == null) {
            result.put("success", false);
            result.put("msg", "用户不存在");
            return result;
        }

        // 使用 BCrypt 的 matches 方法比对密码（数据库存的是加密后的哈希值）
        if (!passwordEncoder.matches(password, user.getPassword())) {
            result.put("success", false);
            result.put("msg", "密码错误");
            return result;
        }

        if ("ban".equals(user.getStatus())) {
            result.put("success", false);
            result.put("msg", "账号已被封禁");
            return result;
        }

        // 登录成功后返回完整用户对象（前端统一按 user 结构存本地，避免字段散落）
        user.setPassword(null);
        // 签发 JWT，subject=userId，claim role 用于鉴权
        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        result.put("success", true);
        result.put("msg", "登录成功");
        result.put("token", token);
        result.put("user", user);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("avatar", user.getAvatar());
        return result;
    }

    // 根据ID查询用户信息
    public User findById(Integer id) {
        return userMapper.findById(id);
    }

    // 修改个人信息（仅更新本次提交的字段）
    public String update(User user) {
        if (user == null || user.getId() == null) {
            return "参数错误";
        }
        if (user.getUsername() != null) {
            user.setUsername(user.getUsername().trim());
            if (user.getUsername().isEmpty()) {
                return "昵称不能为空";
            }
        }
        if (user.getPhone() != null) {
            user.setPhone(user.getPhone().trim());
        }
        if (user.getUsername() == null && user.getPhone() == null && user.getAvatar() == null) {
            return "没有需要修改的内容";
        }
        // 昵称唯一性校验（排除自身）
        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            User exist = userMapper.findByUsername(user.getUsername());
            if (exist != null && !exist.getId().equals(user.getId())) {
                return "昵称已被占用";
            }
        }
        if (user.getPhone() != null && !user.getPhone().isEmpty()
                && !user.getPhone().matches("^1[3-9]\\d{9}$")) {
            return "手机号格式不正确";
        }
        userMapper.update(user);
        return "修改成功";
    }

    // 按昵称/学号搜索用户（不含封禁账号，包含自己）；keyword 为空直接返回空结果，避免全表爬取
    public PageResult<User> search(String keyword, Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        if (keyword == null || keyword.trim().isEmpty()) {
            return PageResult.of(new java.util.ArrayList<>(), 0, p[0], p[1]);
        }
        String kw = keyword.trim();
        List<User> list = userMapper.searchByKeyword(kw, p[2], p[1]);
        list.forEach(u -> u.setPassword(null));   // 不向前端返回密码哈希
        return PageResult.of(list, userMapper.countSearch(kw), p[0], p[1]);
    }

    // 个人中心数据概览
    public UserStatsVO getStats(Integer userId) {
        return userMapper.findStats(userId);
    }
}