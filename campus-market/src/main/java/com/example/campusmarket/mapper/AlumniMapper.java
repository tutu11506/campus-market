package com.example.campusmarket.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 校友标签：同学打标签 + 按标签找人 / 找闲置。
 */
@Mapper
public interface AlumniMapper {

    /** 每个标签下的同学数 */
    @Select("SELECT tag, COUNT(*) AS userCount FROM user_tag GROUP BY tag")
    List<Map<String, Object>> countUsersByTag();

    /** 每个标签下的在售闲置数（按卖家标签统计） */
    @Select("SELECT t.tag, COUNT(*) AS goodsCount FROM user_tag t " +
            "JOIN goods g ON g.seller_id = t.user_id " +
            "WHERE g.is_deleted = 0 AND g.status = 'on_sale' " +
            "GROUP BY t.tag")
    List<Map<String, Object>> countGoodsByTag();

    /** 我的标签 */
    @Select("SELECT tag FROM user_tag WHERE user_id = #{userId} ORDER BY id")
    List<String> findMyTags(@Param("userId") Integer userId);

    @Delete("DELETE FROM user_tag WHERE user_id = #{userId}")
    int deleteByUser(@Param("userId") Integer userId);

    @Insert("INSERT IGNORE INTO user_tag(user_id, tag) VALUES(#{userId}, #{tag})")
    int insertTag(@Param("userId") Integer userId, @Param("tag") String tag);

    /**
     * 某个标签下的同学：带上在售件数与该同学的全部标签，按在售件数排序。
     */
    @Select("SELECT u.id, u.username, u.avatar, u.student_no AS studentNo, u.credit, " +
            "(SELECT COUNT(*) FROM goods g WHERE g.seller_id = u.id " +
            " AND g.is_deleted = 0 AND g.status = 'on_sale') AS onSaleCount, " +
            "(SELECT GROUP_CONCAT(t2.tag ORDER BY t2.id SEPARATOR ',') FROM user_tag t2 " +
            " WHERE t2.user_id = u.id) AS tagStr " +
            "FROM user u JOIN user_tag t ON t.user_id = u.id " +
            "WHERE t.tag = #{tag} " +
            "ORDER BY onSaleCount DESC, u.id ASC LIMIT #{limit}")
    List<Map<String, Object>> findUsersByTag(@Param("tag") String tag, @Param("limit") int limit);

    /**
     * 某个标签下同学的在售闲置。
     */
    @Select("SELECT g.id, g.title, g.price, g.cover_image AS coverImage, g.view_count AS viewCount, " +
            "g.seller_id AS sellerId, u.username AS sellerName, " +
            "(SELECT GROUP_CONCAT(t2.tag ORDER BY t2.id SEPARATOR ',') FROM user_tag t2 " +
            " WHERE t2.user_id = g.seller_id) AS sellerTags " +
            "FROM goods g JOIN user u ON g.seller_id = u.id " +
            "JOIN user_tag t ON t.user_id = g.seller_id " +
            "WHERE t.tag = #{tag} AND g.is_deleted = 0 AND g.status = 'on_sale' " +
            "ORDER BY g.view_count DESC, g.create_time DESC LIMIT #{limit}")
    List<Map<String, Object>> findGoodsByTag(@Param("tag") String tag, @Param("limit") int limit);
}
