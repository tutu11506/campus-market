package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.PointAccount;
import com.example.campusmarket.entity.PointRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface PointMapper {

    /* ==================== PointAccount ==================== */

    @Select("SELECT * FROM point_account WHERE user_id = #{userId}")
    PointAccount findAccount(Integer userId);

    @Insert("INSERT INTO point_account(user_id, balance, earned_total, spent_total) VALUES(#{userId}, 0, 0, 0) " +
            "ON DUPLICATE KEY UPDATE user_id = user_id")
    int ensureAccount(Integer userId);

    /** 原子增加余额（获得积分），返回实际影响行数（>0 成功） */
    @Update("UPDATE point_account SET balance = balance + #{delta}, earned_total = earned_total + #{delta} WHERE user_id = #{userId}")
    int credit(@Param("userId") Integer userId, @Param("delta") int delta);

    /** 原子扣减余额（消费积分），扣不到返回 0；balance_after 由外部计算 */
    @Update("UPDATE point_account SET balance = balance - #{delta}, spent_total = spent_total + #{delta} " +
            "WHERE user_id = #{userId} AND balance >= #{delta}")
    int debit(@Param("userId") Integer userId, @Param("delta") int delta);

    /* ==================== PointRecord ==================== */

    @Insert("INSERT INTO point_record(user_id, direction, points, balance_after, type, ref_type, ref_id, remark) " +
            "VALUES(#{userId}, #{direction}, #{points}, #{balanceAfter}, #{type}, #{refType}, #{refId}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRecord(PointRecord r);

    /** 我的积分流水（带用户名 JOIN 方便管理员看） */
    @Select("SELECT r.*, u.username FROM point_record r LEFT JOIN user u ON u.id = r.user_id " +
            "WHERE r.user_id = #{userId} ORDER BY r.created_at DESC LIMIT #{offset}, #{pageSize}")
    List<PointRecord> findByUser(@Param("userId") Integer userId, @Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM point_record WHERE user_id = #{userId}")
    long countByUser(@Param("userId") Integer userId);

    /* ==================== 排行榜 ==================== */

    /** 个人积分榜（取前 N） */
    @Select("SELECT a.*, u.username, u.student_no, " +
            "NULL AS college " +
            "FROM point_account a LEFT JOIN user u ON u.id = a.user_id " +
            "ORDER BY a.balance DESC LIMIT #{limit}")
    List<Map<String, Object>> userTopBalance(@Param("limit") int limit);

    /** 本月循环榜：按 goods 成交数 */
    @Select("SELECT u.id AS user_id, u.username, u.student_no, " +
            "NULL AS college, " +
            "COUNT(o.id) AS cycled_count " +
            "FROM `order` o JOIN user u ON u.id = o.seller_id " +
            "WHERE DATE_FORMAT(o.create_time, '%Y-%m') = DATE_FORMAT(NOW(), '%Y-%m') " +
            "AND o.status = 'completed' " +
            "GROUP BY u.id ORDER BY cycled_count DESC LIMIT #{limit}")
    List<Map<String, Object>> userTopCycled(@Param("limit") int limit);

    /** 学院榜：暂按学号前缀分组（user 表无 college 字段） */
    @Select("SELECT '全部' AS college, " +
            "COUNT(DISTINCT u.id) AS user_count, " +
            "COALESCE(SUM(a.balance), 0) AS total_balance, " +
            "COALESCE(SUM(a.earned_total), 0) AS total_earned " +
            "FROM user u LEFT JOIN point_account a ON a.user_id = u.id " +
            "ORDER BY total_balance DESC LIMIT #{limit}")
    List<Map<String, Object>> collegeTopBalance(@Param("limit") int limit);
}
