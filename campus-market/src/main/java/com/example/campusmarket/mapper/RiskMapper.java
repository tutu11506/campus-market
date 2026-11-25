package com.example.campusmarket.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface RiskMapper {
    @Select("SELECT word FROM sensitive_word")
    List<String> findAllSensitiveWords();

    @Insert("INSERT INTO report (reporter_id, target_type, target_id, reason, detail) VALUES (#{reporterId}, #{targetType}, #{targetId}, #{reason}, #{detail})")
    int insertReport(@Param("reporterId") int reporterId, @Param("targetType") String targetType, @Param("targetId") int targetId, @Param("reason") String reason, @Param("detail") String detail);

    @Select("SELECT r.*, u.username AS reporter_name FROM report r LEFT JOIN user u ON r.reporter_id = u.id ORDER BY r.create_time DESC LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> findReports(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM report")
    int countReports();

    @Update("UPDATE report SET status = #{status}, handler_id = #{handlerId} WHERE id = #{reportId}")
    int handleReport(@Param("reportId") int reportId, @Param("status") String status, @Param("handlerId") int handlerId);

    @Select("SELECT COUNT(*) FROM blacklist WHERE user_id = #{userId} AND blocked_user_id = #{blockedUserId}")
    int isBlocked(@Param("userId") int userId, @Param("blockedUserId") int blockedUserId);

    @Insert("INSERT IGNORE INTO blacklist (user_id, blocked_user_id) VALUES (#{userId}, #{blockedUserId})")
    int blockUser(@Param("userId") int userId, @Param("blockedUserId") int blockedUserId);

    @Delete("DELETE FROM blacklist WHERE user_id = #{userId} AND blocked_user_id = #{blockedUserId}")
    int unblockUser(@Param("userId") int userId, @Param("blockedUserId") int blockedUserId);

    @Update("UPDATE user SET credit = GREATEST(0, credit - #{delta}) WHERE id = #{userId}")
    int deductCredit(@Param("userId") int userId, @Param("delta") int delta);

    @Select("SELECT credit FROM user WHERE id = #{userId}")
    Integer findCredit(@Param("userId") int userId);
}