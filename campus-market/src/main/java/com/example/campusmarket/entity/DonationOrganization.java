package com.example.campusmarket.entity;

import lombok.Data;

import java.util.Date;

/**
 * 公益捐赠对接社团。
 */
@Data
public class DonationOrganization {
    private Integer id;
    private String name;
    private String intro;
    private String acceptTypes;
    private String contact;
    private String dropoffAddress;
    private Integer isDemo;
    private Integer isActive;
    private Date createTime;
}
