package com.example.campusmarket.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 闲置盲盒许愿：买家按品类许愿，卖家随机打包发货。
 */
@Mapper
public interface BlindBoxMapper {

    String BASE_COLUMNS = "SELECT b.id, b.buyer_id AS buyerId, u.username AS buyerName, u.avatar AS buyerAvatar, " +
            "b.category, b.budget, b.wish_note AS wishNote, b.status, " +
            "b.seller_id AS sellerId, s.username AS sellerName, " +
            "b.box_title AS boxTitle, b.box_desc AS boxDesc, b.price, " +
            "DATE_FORMAT(b.create_time, '%Y-%m-%d %H:%i') AS createTime, " +
            "DATE_FORMAT(b.match_time, '%Y-%m-%d %H:%i') AS matchTime, " +
            "DATE_FORMAT(b.complete_time, '%Y-%m-%d %H:%i') AS completeTime " +
            "FROM blind_box_wish b " +
            "LEFT JOIN user u ON b.buyer_id = u.id " +
            "LEFT JOIN user s ON b.seller_id = s.id ";

    /** 待认领：别人发布的、仍处于 open 的许愿 */
    @Select(BASE_COLUMNS + "WHERE b.status = 'open' AND b.buyer_id <> #{userId} " +
            "ORDER BY b.create_time DESC LIMIT 60")
    List<Map<String, Object>> findOpen(@Param("userId") Integer userId);

    /** 我的许愿：我作为买家的全部许愿 */
    @Select(BASE_COLUMNS + "WHERE b.buyer_id = #{userId} ORDER BY b.create_time DESC LIMIT 60")
    List<Map<String, Object>> findMine(@Param("userId") Integer userId);

    /** 我接的单：我作为卖家打包过的许愿 */
    @Select(BASE_COLUMNS + "WHERE b.seller_id = #{userId} ORDER BY b.create_time DESC LIMIT 60")
    List<Map<String, Object>> findClaimed(@Param("userId") Integer userId);

    @Select(BASE_COLUMNS + "WHERE b.id = #{id}")
    Map<String, Object> findById(@Param("id") Integer id);

    @Select("SELECT id, buyer_id AS buyerId, category, budget, price, status FROM blind_box_wish " +
            "WHERE buyer_id = #{userId} ORDER BY id DESC LIMIT 1")
    Map<String, Object> findLatestByBuyer(@Param("userId") Integer userId);

    @Select("SELECT " +
            "COALESCE(SUM(CASE WHEN status = 'open' AND buyer_id <> #{userId} THEN 1 ELSE 0 END), 0) AS openCount, " +
            "COALESCE(SUM(CASE WHEN buyer_id = #{userId} THEN 1 ELSE 0 END), 0) AS mineCount, " +
            "COALESCE(SUM(CASE WHEN seller_id = #{userId} THEN 1 ELSE 0 END), 0) AS claimedCount " +
            "FROM blind_box_wish")
    Map<String, Object> countByScope(@Param("userId") Integer userId);

    @Insert("INSERT INTO blind_box_wish(buyer_id, category, budget, wish_note, status, create_time) " +
            "VALUES(#{buyerId}, #{category}, #{budget}, #{wishNote}, 'open', NOW())")
    int insertWish(@Param("buyerId") Integer buyerId,
                   @Param("category") String category,
                   @Param("budget") Double budget,
                   @Param("wishNote") String wishNote);

    /** 认领并打包：只有 open 且不是自己发布的许愿才能被认领（条件更新防并发抢单） */
    @Update("UPDATE blind_box_wish SET status = 'matched', seller_id = #{sellerId}, " +
            "box_title = #{boxTitle}, box_desc = #{boxDesc}, price = #{price}, match_time = NOW() " +
            "WHERE id = #{id} AND status = 'open' AND buyer_id <> #{sellerId}")
    int claim(@Param("id") Integer id,
              @Param("sellerId") Integer sellerId,
              @Param("boxTitle") String boxTitle,
              @Param("boxDesc") String boxDesc,
              @Param("price") Double price);

    /** 买家确认收货：matched -> completed */
    @Update("UPDATE blind_box_wish SET status = 'completed', complete_time = NOW() " +
            "WHERE id = #{id} AND buyer_id = #{userId} AND status = 'matched'")
    int confirm(@Param("id") Integer id, @Param("userId") Integer userId);

    /** 买家取消许愿：open -> cancelled */
    @Update("UPDATE blind_box_wish SET status = 'cancelled' " +
            "WHERE id = #{id} AND buyer_id = #{userId} AND status = 'open'")
    int cancel(@Param("id") Integer id, @Param("userId") Integer userId);
}
