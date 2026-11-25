package com.example.campusmarket.entity;

import lombok.Data;
import java.util.Date;

/**
 * 教材版本避雷留言：老师不让用 / 错误太多 / 太难 / 过时 / 其他。
 */
@Data
public class TextbookWarning {
    private Integer id;
    private Integer textbookId;
    private Integer userId;
    private String reason;       // why_not_allowed / errors / too_difficult / outdated / other
    private String detail;
    private Integer isValid;    // 1=有效 0=已隐藏
    private Date createTime;

    // 展示字段
    private String userName;
    private String userStudentNo;
}
