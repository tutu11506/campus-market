package com.example.campusmarket.entity;

import lombok.Data;
import java.util.Date;

@Data
public class User {
    private Integer id;          // 用户ID
    private String studentNo;    // 学号
    private String username;     // 昵称
    private String password;     // 密码
    private String phone;        // 手机号
    private String avatar;       // 头像
    private Integer credit;      // 信用分
    private String role;         // 角色
    private String status;       // 状态
    private Date createTime;     // 注册时间
}