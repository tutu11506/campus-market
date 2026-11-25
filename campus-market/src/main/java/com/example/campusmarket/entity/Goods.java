package com.example.campusmarket.entity;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class Goods {
    private Integer id;              // 商品ID

    @NotBlank(message = "商品标题不能为空")
    private String title;            // 标题

    private String description;      // 描述

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    private BigDecimal price;        // 价格

    private BigDecimal originalPrice; // 原价

    private BigDecimal basePrice;     // 卖家填写的基础价

    private BigDecimal listPrice;     // 平台标价（基础价 + 加价）

    private BigDecimal couponAmount;  // 随单优惠券面额（加价的一半）

    private String images;           // 图片（多张逗号分隔）

    @NotBlank(message = "请上传商品封面图")
    private String coverImage;       // 封面图

    @NotNull(message = "请选择商品分类")
    private Integer categoryId;      // 分类ID

    // 教材分类(category_id=1)可选关联教材主表，用于聚合同名/同版本教材的多件闲置
    private Integer textbookId;

    private Integer sellerId;        // 卖家ID

    private String status;           // 状态：on_sale在售/sold已售/off_shelf下架

    private Integer viewCount;       // 浏览量

    @NotBlank(message = "请填写交易地点")
    private String location;         // 交易地点

    private Integer canMail;          // 可邮寄 0/1
    private Integer canDeliver;       // 可送货到宿舍 0/1
    private String tradeMode;         // cash 现金出售 / exchange 换物
    private Integer exchangeSupported;// 支持以物换物 0/1
    private String exchangeWant;      // 希望换到什么

    private Integer isDeleted;       // 是否删除
    private Date createTime;         // 发布时间
    private Date updateTime;         // 更新时间

    // 风控字段（违禁品识别 / 价格监控）
    private String riskLevel;        // none/pass/review/block
    private String riskReason;       // 命中违禁品说明
    private Integer priceAlert;      // 1=价格高于近期均价
    private String priceAlertNote;   // 价格预警说明

    // 额外字段：查询时关联显示
    private String sellerName;       // 卖家昵称
    private String sellerAvatar;     // 卖家头像
    private String sellerStudentNo;  // 卖家学号（用于推断年级/是否毕业清仓）
    private String categoryName;     // 分类名称
    private Integer commentCount;    // 评价数（关联 comment 统计，展示用）
    private BigDecimal avgRating;    // 平均评分（关联 comment 统计，展示用）

    // 差异化标记（服务层 enrich 写入，不映射数据库）
    private Boolean graduationSale;  // 卖家是否处于毕业清仓期
    private String priceRelation;    // 与当前买家的关系：senior_sale/peer_sale/junior_sale
    private String priceTag;         // 前端直接展示的标签文本，如"学长特惠"

    // 卖家信用档案（详情页 enrich 写入，不映射数据库）
    private Integer sellerCredit;      // 卖家信用分
    private Integer sellerSoldCount;   // 卖家历史成交单数
    private Integer sellerOnSaleCount; // 卖家当前在售件数
    private Integer sellerJoinDays;    // 卖家入校/注册天数
    private String sellerLevel;        // 卖家等级标签：金牌卖家/可信卖家/普通卖家/谨慎交易

    // 关联教材主表展示字段（查询时 enrich 写入）
    private String textbookTitle;
    private String textbookIsbn;
    private String textbookEdition;
    private String textbookAuthor;
    private Integer textbookTeacherFlag;
    private Integer textbookWarnCount;
}
