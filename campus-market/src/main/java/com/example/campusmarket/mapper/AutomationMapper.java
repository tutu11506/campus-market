package com.example.campusmarket.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

public interface AutomationMapper {

    /** 写入一条预警（同日同对象重复扫描会自动忽略） */
    @Insert("INSERT IGNORE INTO automation_alert(rule_code, level, title, content, target_type, target_id, user_id, alert_date) " +
            "VALUES(#{ruleCode}, #{level}, #{title}, #{content}, #{targetType}, #{targetId}, #{userId}, CURDATE())")
    int insertAlert(@Param("ruleCode") String ruleCode,
                    @Param("level") String level,
                    @Param("title") String title,
                    @Param("content") String content,
                    @Param("targetType") String targetType,
                    @Param("targetId") Integer targetId,
                    @Param("userId") Integer userId);

    /** 规则一：单个卖家在售件数达到阈值（库存积压） */
    @Select("SELECT g.seller_id AS sellerId, u.username AS sellerName, COUNT(*) AS cnt " +
            "FROM goods g LEFT JOIN user u ON g.seller_id = u.id " +
            "WHERE g.is_deleted = 0 AND g.status = 'on_sale' " +
            "GROUP BY g.seller_id, u.username HAVING COUNT(*) >= #{threshold}")
    List<Map<String, Object>> sellersWithStock(@Param("threshold") int threshold);

    /** 规则二：滞销商品（上架超过 N 天仍在售） */
    @Select("SELECT id, title, price, DATEDIFF(NOW(), create_time) AS days " +
            "FROM goods WHERE is_deleted = 0 AND status = 'on_sale' " +
            "AND create_time < DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "ORDER BY create_time ASC LIMIT #{limit}")
    List<Map<String, Object>> staleGoods(@Param("days") int days, @Param("limit") int limit);

    /** 规则三：挂牌临期（接近自动到期，3 天内） */
    @Select("SELECT id, title, price, DATEDIFF(NOW(), create_time) AS days, " +
            "(#{expireDays} - DATEDIFF(NOW(), create_time)) AS leftDays " +
            "FROM goods WHERE is_deleted = 0 AND status = 'on_sale' " +
            "AND create_time <= DATE_SUB(NOW(), INTERVAL (#{expireDays} - 3) DAY) " +
            "AND create_time > DATE_SUB(NOW(), INTERVAL #{expireDays} DAY) " +
            "ORDER BY create_time ASC LIMIT #{limit}")
    List<Map<String, Object>> expiringGoods(@Param("expireDays") int expireDays, @Param("limit") int limit);

    /** 规则四：分类缺货（在售件数低于阈值） */
    @Select("SELECT c.id AS categoryId, c.name AS categoryName, COUNT(g.id) AS cnt " +
            "FROM category c LEFT JOIN goods g ON g.category_id = c.id " +
            "AND g.is_deleted = 0 AND g.status = 'on_sale' " +
            "GROUP BY c.id, c.name HAVING COUNT(g.id) <= #{threshold} " +
            "ORDER BY cnt ASC")
    List<Map<String, Object>> lowStockCategories(@Param("threshold") int threshold);

    /** 规则五：任务即将到期（24 小时内且未完成） */
    @Select("SELECT id, title, status, reward, TIMESTAMPDIFF(MINUTE, NOW(), deadline) AS leftMinutes " +
            "FROM campus_task WHERE is_deleted = 0 AND status IN ('open', 'accepted') " +
            "AND deadline IS NOT NULL AND deadline BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 24 HOUR)")
    List<Map<String, Object>> dueTasks();

    /** 各规则的预警统计 */
    @Select("SELECT rule_code AS ruleCode, " +
            "SUM(CASE WHEN alert_date = CURDATE() THEN 1 ELSE 0 END) AS todayCount, " +
            "SUM(CASE WHEN status = 'open' THEN 1 ELSE 0 END) AS openCount " +
            "FROM automation_alert GROUP BY rule_code")
    List<Map<String, Object>> ruleStats();

    /** 全局统计 */
    @Select("SELECT COALESCE(SUM(CASE WHEN alert_date = CURDATE() THEN 1 ELSE 0 END), 0) AS todayTotal, " +
            "COALESCE(SUM(CASE WHEN status = 'open' THEN 1 ELSE 0 END), 0) AS openTotal, " +
            "COALESCE(SUM(CASE WHEN status = 'open' AND level = 'high' THEN 1 ELSE 0 END), 0) AS highOpen " +
            "FROM automation_alert")
    Map<String, Object> globalStats();

    /** 预警分页 */
    @Select("<script>" +
            "SELECT id, rule_code AS ruleCode, level, title, content, target_type AS targetType, " +
            "target_id AS targetId, user_id AS userId, alert_date AS alertDate, status, create_time AS createTime " +
            "FROM automation_alert WHERE 1 = 1 " +
            "<if test='ruleCode != null and ruleCode != \"\"'> AND rule_code = #{ruleCode} </if>" +
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>" +
            "ORDER BY (status = 'open') DESC, FIELD(level, 'high', 'medium', 'low'), create_time DESC " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<Map<String, Object>> findAlerts(@Param("ruleCode") String ruleCode,
                                         @Param("status") String status,
                                         @Param("offset") int offset,
                                         @Param("pageSize") int pageSize);

    /** 预警总数 */
    @Select("<script>" +
            "SELECT COUNT(*) FROM automation_alert WHERE 1 = 1 " +
            "<if test='ruleCode != null and ruleCode != \"\"'> AND rule_code = #{ruleCode} </if>" +
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>" +
            "</script>")
    int countAlerts(@Param("ruleCode") String ruleCode, @Param("status") String status);

    /** 标记预警已处理 */
    @Update("UPDATE automation_alert SET status = 'resolved' WHERE id = #{id} AND status = 'open'")
    int resolve(@Param("id") Integer id);
}
