package com.example.campusmarket.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 违禁品识别记录（标题/描述/图片名统一检测）。
 * source=rule 为关键词规则引擎，ai 预留给真实视觉模型接入。
 */
@Data
public class ImageModeration {
    private Integer id;
    private Integer uploaderId;      // 上传者
    private Integer goodsId;         // 关联商品（拦截时可能为空）
    private String imageUrl;         // 被检测图片
    private String textSample;       // 命中文本片段
    private String verdict;          // pass/review/block
    private String riskCategory;     // appliance/e_cigarette/battery...
    private String labels;           // 命中标签
    private BigDecimal confidence;   // 0-100 置信度
    private String source;           // rule/ai/manual
    private String status;           // auto/confirmed/released
    private Integer handlerId;       // 人工处理人
    private Date createTime;

    // 展示字段
    private String uploaderName;
    private String goodsTitle;
    private String goodsCover;
}
