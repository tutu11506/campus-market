package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.GroupBuy;
import com.example.campusmarket.entity.GroupBuyMember;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface GroupBuyMapper {

    String COLUMNS = "SELECT g.*, u.username AS owner_name, u.student_no AS owner_student_no, " +
            "gd.title AS goods_title, gd.cover_image AS goods_cover, gd.price AS goods_price, " +
            "GREATEST(g.target_count - g.joined_count, 0) AS remaining_count, " +
            "COALESCE((SELECT SUM(m.shares) FROM group_buy_member m " +
            "  WHERE m.group_id = g.id AND m.user_id = #{uid} AND m.status = 'joined'), 0) AS my_shares " +
            "FROM group_buy g " +
            "JOIN user u ON g.owner_id = u.id " +
            "LEFT JOIN goods gd ON g.goods_id = gd.id AND gd.is_deleted = 0 ";

    @Select("<script>" + COLUMNS +
            "WHERE g.status IN ('open', 'full', 'formed') " +
            "<if test='type != null and type != \"\"'> AND g.type = #{type} </if>" +
            "<if test='status != null and status != \"\"'> AND g.status = #{status} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (g.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR g.description LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "ORDER BY (g.status = 'open') DESC, g.deadline ASC " +
            "LIMIT #{offset}, #{pageSize}</script>")
    List<GroupBuy> findList(@Param("type") String type,
                            @Param("status") String status,
                            @Param("keyword") String keyword,
                            @Param("uid") Integer uid,
                            @Param("offset") int offset,
                            @Param("pageSize") int pageSize);

    @Select("<script>SELECT COUNT(*) FROM group_buy g " +
            "WHERE g.status IN ('open', 'full', 'formed') " +
            "<if test='type != null and type != \"\"'> AND g.type = #{type} </if>" +
            "<if test='status != null and status != \"\"'> AND g.status = #{status} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (g.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR g.description LIKE CONCAT('%', #{keyword}, '%')) </if></script>")
    long countList(@Param("type") String type,
                   @Param("status") String status,
                   @Param("keyword") String keyword);

    @Select(COLUMNS + "WHERE g.id = #{id}")
    GroupBuy findById(@Param("id") Integer id, @Param("uid") Integer uid);

    @Insert("INSERT INTO group_buy(owner_id, type, title, description, target_count, joined_count, " +
            "price_per_share, deadline, goods_id, location, status) " +
            "VALUES(#{ownerId}, #{type}, #{title}, #{description}, #{targetCount}, 1, " +
            "#{pricePerShare}, #{deadline}, #{goodsId}, #{location}, 'open')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(GroupBuy groupBuy);

    /** 原子拼单：份数不超过剩余、未截止、状态为拼单中才能成功，避免并发超拼 */
    @Update("UPDATE group_buy SET status = CASE WHEN joined_count + #{shares} >= target_count " +
            "THEN 'full' ELSE status END, " +
            "joined_count = joined_count + #{shares} " +
            "WHERE id = #{id} AND status = 'open' AND deadline > NOW() " +
            "AND joined_count + #{shares} <= target_count")
    int joinAtomic(@Param("id") Integer id, @Param("shares") int shares);

    /** 退出仅限拼单中，退出的份数释放回剩余份数 */
    @Update("UPDATE group_buy SET joined_count = joined_count - #{shares}, status = 'open' " +
            "WHERE id = #{id} AND status = 'open' AND joined_count >= #{shares}")
    int releaseShares(@Param("id") Integer id, @Param("shares") int shares);

    @Update("UPDATE group_buy SET status = 'formed' " +
            "WHERE id = #{id} AND owner_id = #{uid} AND status IN ('open', 'full') AND joined_count > 0")
    int confirm(@Param("id") Integer id, @Param("uid") Integer uid);

    @Update("UPDATE group_buy SET status = 'closed' " +
            "WHERE id = #{id} AND owner_id = #{uid} AND status IN ('open', 'full')")
    int close(@Param("id") Integer id, @Param("uid") Integer uid);

    /** 到期未满：未成团失败（满员未确认也视为到期失败，由发起人决定是否提前确认成团） */
    @Select("SELECT id FROM group_buy WHERE status IN ('open', 'full') AND deadline < NOW()")
    List<Integer> findExpiredIds();

    @Update("UPDATE group_buy SET status = 'failed', deadline_reminded = 1 " +
            "WHERE id = #{id} AND status IN ('open', 'full')")
    int markFailed(Integer id);

    /* ==================== 成员 ==================== */

    @Select("SELECT m.*, u.username AS user_name, u.avatar AS user_avatar, u.student_no AS user_student_no " +
            "FROM group_buy_member m JOIN user u ON m.user_id = u.id " +
            "WHERE m.group_id = #{groupId} AND m.status = 'joined' ORDER BY m.create_time ASC")
    List<GroupBuyMember> findJoinedMembers(Integer groupId);

    @Select("SELECT m.*, u.username AS user_name FROM group_buy_member m JOIN user u ON m.user_id = u.id " +
            "WHERE m.group_id = #{groupId} AND m.user_id = #{userId}")
    GroupBuyMember findMember(@Param("groupId") Integer groupId, @Param("userId") Integer userId);

    @Insert("INSERT INTO group_buy_member(group_id, user_id, shares, status) " +
            "VALUES(#{groupId}, #{userId}, #{shares}, 'joined')")
    int insertMember(@Param("groupId") Integer groupId,
                     @Param("userId") Integer userId,
                     @Param("shares") int shares);

    @Update("UPDATE group_buy_member SET shares = #{shares}, status = 'joined' " +
            "WHERE group_id = #{groupId} AND user_id = #{userId} AND status = 'left'")
    int reactivateMember(@Param("groupId") Integer groupId,
                         @Param("userId") Integer userId,
                         @Param("shares") int shares);

    @Update("UPDATE group_buy_member SET status = 'left' " +
            "WHERE group_id = #{groupId} AND user_id = #{userId} AND status = 'joined'")
    int leaveMember(@Param("groupId") Integer groupId, @Param("userId") Integer userId);

    /* ==================== 我的拼单 ==================== */

    @Select("<script>" + COLUMNS + "WHERE g.owner_id = #{uid} " +
            "<if test='status != null and status != \"\"'> AND g.status = #{status} </if>" +
            "ORDER BY g.create_time DESC LIMIT #{offset}, #{pageSize}</script>")
    List<GroupBuy> findOwnerList(@Param("uid") Integer uid,
                                 @Param("status") String status,
                                 @Param("offset") int offset,
                                 @Param("pageSize") int pageSize);

    @Select("<script>SELECT COUNT(*) FROM group_buy g WHERE g.owner_id = #{uid} " +
            "<if test='status != null and status != \"\"'> AND g.status = #{status} </if></script>")
    long countOwnerList(@Param("uid") Integer uid, @Param("status") String status);

    @Select("<script>" + COLUMNS +
            "JOIN group_buy_member gm ON gm.group_id = g.id AND gm.user_id = #{uid} AND gm.status = 'joined' " +
            "WHERE g.owner_id != #{uid} " +
            "<if test='status != null and status != \"\"'> AND g.status = #{status} </if>" +
            "ORDER BY gm.create_time DESC LIMIT #{offset}, #{pageSize}</script>")
    List<GroupBuy> findJoinedList(@Param("uid") Integer uid,
                                  @Param("status") String status,
                                  @Param("offset") int offset,
                                  @Param("pageSize") int pageSize);

    @Select("<script>SELECT COUNT(*) FROM group_buy g " +
            "JOIN group_buy_member gm ON gm.group_id = g.id AND gm.user_id = #{uid} AND gm.status = 'joined' " +
            "WHERE g.owner_id != #{uid} " +
            "<if test='status != null and status != \"\"'> AND g.status = #{status} </if></script>")
    long countJoinedList(@Param("uid") Integer uid, @Param("status") String status);

    /** 搭子发布时可关联的、我发起的进行中拼单 */
    @Select("SELECT g.*, GREATEST(g.target_count - g.joined_count, 0) AS remaining_count " +
            "FROM group_buy g WHERE g.owner_id = #{uid} AND g.status IN ('open', 'full') " +
            "ORDER BY g.create_time DESC LIMIT 20")
    List<GroupBuy> findLinkable(Integer uid);

    /** 站内提醒使用平台客服账号发送，账号不存在时跳过提醒 */
    @Select("SELECT id FROM user WHERE student_no = 'KEFU0001' LIMIT 1")
    Integer findCustomerServiceId();
}
