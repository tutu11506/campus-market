package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.PartnerMember;
import com.example.campusmarket.entity.PartnerPost;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PartnerMapper {

    String COLUMNS = "SELECT p.*, u.username AS owner_name, u.student_no AS owner_student_no, " +
            "GREATEST(p.member_limit - p.member_count, 0) AS remaining_count, " +
            "COALESCE((SELECT 1 FROM partner_member pm " +
            "  WHERE pm.partner_id = p.id AND pm.user_id = #{uid} AND pm.status = 'joined'), 0) AS my_status " +
            "FROM partner_post p JOIN user u ON p.owner_id = u.id ";

    @Select("<script>" + COLUMNS +
            "WHERE p.status IN ('open', 'full') " +
            "<if test='type != null and type != \"\"'> AND p.type = #{type} </if>" +
            "<if test='status != null and status != \"\"'> AND p.status = #{status} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (p.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.description LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "ORDER BY (p.status = 'open') DESC, p.activity_time ASC " +
            "LIMIT #{offset}, #{pageSize}</script>")
    List<PartnerPost> findList(@Param("type") String type,
                               @Param("status") String status,
                               @Param("keyword") String keyword,
                               @Param("uid") Integer uid,
                               @Param("offset") int offset,
                               @Param("pageSize") int pageSize);

    @Select("<script>SELECT COUNT(*) FROM partner_post p " +
            "WHERE p.status IN ('open', 'full') " +
            "<if test='type != null and type != \"\"'> AND p.type = #{type} </if>" +
            "<if test='status != null and status != \"\"'> AND p.status = #{status} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (p.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.description LIKE CONCAT('%', #{keyword}, '%')) </if></script>")
    long countList(@Param("type") String type,
                   @Param("status") String status,
                   @Param("keyword") String keyword);

    @Select(COLUMNS + "WHERE p.id = #{id}")
    PartnerPost findById(@Param("id") Integer id, @Param("uid") Integer uid);

    @Insert("INSERT INTO partner_post(owner_id, type, title, description, activity_time, location, " +
            "member_limit, member_count, link_type, link_id, link_title, status) " +
            "VALUES(#{ownerId}, #{type}, #{title}, #{description}, #{activityTime}, #{location}, " +
            "#{memberLimit}, 1, #{linkType}, #{linkId}, #{linkTitle}, 'open')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PartnerPost post);

    /** 原子加入：未截止、状态招募中、人数未超上限才能成功，避免并发超员 */
    @Update("UPDATE partner_post SET status = CASE WHEN member_count + 1 >= member_limit " +
            "THEN 'full' ELSE status END, " +
            "member_count = member_count + 1 " +
            "WHERE id = #{id} AND status = 'open' AND activity_time > NOW() " +
            "AND member_count + 1 <= member_limit")
    int joinAtomic(Integer id);

    /** 退出仅限招募中，且至少保留发起人 1 人 */
    @Update("UPDATE partner_post SET member_count = member_count - 1, status = 'open' " +
            "WHERE id = #{id} AND status = 'open' AND member_count > 1")
    int releaseOne(Integer id);

    @Update("UPDATE partner_post SET status = 'cancelled' " +
            "WHERE id = #{id} AND owner_id = #{uid} AND status IN ('open', 'full')")
    int cancel(@Param("id") Integer id, @Param("uid") Integer uid);

    /** 活动时间已过：自动结束招募 */
    @Select("SELECT id FROM partner_post WHERE status IN ('open', 'full') AND activity_time < NOW()")
    List<Integer> findExpiredIds();

    @Update("UPDATE partner_post SET status = 'finished' WHERE id = #{id} AND status IN ('open', 'full')")
    int markFinished(Integer id);

    /* ==================== 成员 ==================== */

    @Select("SELECT m.*, u.username AS user_name, u.avatar AS user_avatar, u.student_no AS user_student_no " +
            "FROM partner_member m JOIN user u ON m.user_id = u.id " +
            "WHERE m.partner_id = #{partnerId} AND m.status = 'joined' ORDER BY m.create_time ASC")
    List<PartnerMember> findJoinedMembers(Integer partnerId);

    @Select("SELECT m.*, u.username AS user_name FROM partner_member m JOIN user u ON m.user_id = u.id " +
            "WHERE m.partner_id = #{partnerId} AND m.user_id = #{userId}")
    PartnerMember findMember(@Param("partnerId") Integer partnerId, @Param("userId") Integer userId);

    @Insert("INSERT INTO partner_member(partner_id, user_id, status) " +
            "VALUES(#{partnerId}, #{userId}, 'joined')")
    int insertMember(@Param("partnerId") Integer partnerId, @Param("userId") Integer userId);

    @Update("UPDATE partner_member SET status = 'joined' " +
            "WHERE partner_id = #{partnerId} AND user_id = #{userId} AND status = 'left'")
    int reactivateMember(@Param("partnerId") Integer partnerId, @Param("userId") Integer userId);

    @Update("UPDATE partner_member SET status = 'left' " +
            "WHERE partner_id = #{partnerId} AND user_id = #{userId} AND status = 'joined'")
    int leaveMember(@Param("partnerId") Integer partnerId, @Param("userId") Integer userId);

    /* ==================== 我的搭子 ==================== */

    @Select("<script>" + COLUMNS + "WHERE p.owner_id = #{uid} " +
            "<if test='status != null and status != \"\"'> AND p.status = #{status} </if>" +
            "ORDER BY p.create_time DESC LIMIT #{offset}, #{pageSize}</script>")
    List<PartnerPost> findOwnerList(@Param("uid") Integer uid,
                                    @Param("status") String status,
                                    @Param("offset") int offset,
                                    @Param("pageSize") int pageSize);

    @Select("<script>SELECT COUNT(*) FROM partner_post p WHERE p.owner_id = #{uid} " +
            "<if test='status != null and status != \"\"'> AND p.status = #{status} </if></script>")
    long countOwnerList(@Param("uid") Integer uid, @Param("status") String status);

    @Select("<script>" + COLUMNS +
            "JOIN partner_member pm ON pm.partner_id = p.id AND pm.user_id = #{uid} AND pm.status = 'joined' " +
            "WHERE p.owner_id != #{uid} " +
            "<if test='status != null and status != \"\"'> AND p.status = #{status} </if>" +
            "ORDER BY pm.create_time DESC LIMIT #{offset}, #{pageSize}</script>")
    List<PartnerPost> findJoinedList(@Param("uid") Integer uid,
                                     @Param("status") String status,
                                     @Param("offset") int offset,
                                     @Param("pageSize") int pageSize);

    @Select("<script>SELECT COUNT(*) FROM partner_post p " +
            "JOIN partner_member pm ON pm.partner_id = p.id AND pm.user_id = #{uid} AND pm.status = 'joined' " +
            "WHERE p.owner_id != #{uid} " +
            "<if test='status != null and status != \"\"'> AND p.status = #{status} </if></script>")
    long countJoinedList(@Param("uid") Integer uid, @Param("status") String status);

    /** 站内提醒使用平台客服账号发送，账号不存在时跳过提醒 */
    @Select("SELECT id FROM user WHERE student_no = 'KEFU0001' LIMIT 1")
    Integer findCustomerServiceId();

    @Select("SELECT title FROM goods WHERE id = #{id} AND is_deleted = 0")
    String findGoodsTitle(Integer id);

    @Select("SELECT title FROM group_buy WHERE id = #{id} AND status IN ('open', 'full')")
    String findGroupBuyTitle(Integer id);
}
