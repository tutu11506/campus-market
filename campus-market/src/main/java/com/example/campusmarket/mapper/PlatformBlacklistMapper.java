package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.PlatformBlacklist;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PlatformBlacklistMapper {

    String PAGE_SELECT =
            "SELECT b.*, u.username AS user_name, u.avatar AS user_avatar, u.student_no, u.credit, " +
            "a.username AS created_by_name " +
            "FROM platform_blacklist b " +
            "LEFT JOIN user u ON b.user_id = u.id " +
            "LEFT JOIN user a ON b.created_by = a.id ";

    @Insert("INSERT INTO platform_blacklist(user_id, level, reason, evidence, report_count, status, created_by) " +
            "VALUES(#{userId}, #{level}, #{reason}, #{evidence}, #{reportCount}, 'active', #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PlatformBlacklist item);

    @Select("SELECT COUNT(*) FROM platform_blacklist WHERE user_id = #{userId} AND status = 'active'")
    int countActiveByUser(Integer userId);

    @Select(PAGE_SELECT + "WHERE b.user_id = #{userId} AND b.status = 'active' ORDER BY b.id DESC LIMIT 1")
    PlatformBlacklist findActiveByUser(Integer userId);

    @Select(PAGE_SELECT + "WHERE b.id = #{id}")
    PlatformBlacklist findById(Integer id);

    @Select("<script>" + PAGE_SELECT +
            "WHERE 1 = 1 " +
            "<if test='status != null and status != \"\"'>AND b.status = #{status}</if> " +
            "ORDER BY b.id DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<PlatformBlacklist> findPage(@Param("status") String status,
                                     @Param("limit") int limit,
                                     @Param("offset") int offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM platform_blacklist WHERE 1 = 1 " +
            "<if test='status != null and status != \"\"'>AND status = #{status}</if>" +
            "</script>")
    int countPage(@Param("status") String status);

    @Update("UPDATE platform_blacklist SET status = 'revoked', revoked_by = #{revokedBy}, revoked_at = NOW() WHERE id = #{id} AND status = 'active'")
    int revoke(@Param("id") Integer id, @Param("revokedBy") Integer revokedBy);

    /** 关联该用户的举报数量，作为拉黑时的失信佐证 */
    @Select("SELECT COUNT(*) FROM report WHERE target_type = 'user' AND target_id = #{userId}")
    int countUserReports(Integer userId);

    @Select("SELECT COUNT(*) FROM platform_blacklist WHERE status = 'active'")
    int countActive();

    @Select("SELECT COUNT(*) FROM platform_blacklist")
    int countAll();
}
