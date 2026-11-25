package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.Goods;
import com.example.campusmarket.vo.BrowsedGoodsVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BrowseHistoryMapper {

    /**
     * 写入浏览记录：同用户同商品去重（uk_user_goods），重复浏览刷新时间并累加次数（幂等）。
     */
    @Insert("INSERT INTO browse_history (user_id, goods_id, seller_id, browse_count, create_time) " +
            "VALUES (#{userId}, #{goodsId}, #{sellerId}, 1, NOW()) " +
            "ON DUPLICATE KEY UPDATE create_time = NOW(), browse_count = browse_count + 1, seller_id = VALUES(seller_id)")
    int upsert(@Param("userId") Integer userId,
               @Param("goodsId") Integer goodsId,
               @Param("sellerId") Integer sellerId);

    /**
     * 当前用户在某卖家店铺里浏览过的在售商品（按最近浏览倒序）—— 供聊天“发送商品”选择器。
     */
    @Select("SELECT g.* FROM browse_history bh " +
            "JOIN goods g ON g.id = bh.goods_id " +
            "WHERE bh.user_id = #{userId} AND bh.seller_id = #{sellerId} " +
            "AND g.is_deleted = 0 AND g.status = 'on_sale' " +
            "ORDER BY bh.create_time DESC")
    List<Goods> findBrowsedGoodsBySeller(@Param("userId") Integer userId,
                                         @Param("sellerId") Integer sellerId);

    /** 我的浏览历史总数（含已售/下架，排除已删除） */
    @Select("SELECT COUNT(*) FROM browse_history bh " +
            "JOIN goods g ON g.id = bh.goods_id " +
            "WHERE bh.user_id = #{userId} AND g.is_deleted = 0")
    long countMyHistory(@Param("userId") Integer userId);

    /** 我的浏览历史分页（按最近浏览倒序） */
    @Select("SELECT g.id, g.title, g.cover_image AS coverImage, g.price, g.status, " +
            "g.seller_id AS sellerId, u.username AS sellerName, " +
            "bh.create_time AS browseTime, bh.browse_count AS browseCount " +
            "FROM browse_history bh " +
            "JOIN goods g ON g.id = bh.goods_id " +
            "LEFT JOIN user u ON u.id = g.seller_id " +
            "WHERE bh.user_id = #{userId} AND g.is_deleted = 0 " +
            "ORDER BY bh.create_time DESC LIMIT #{offset}, #{size}")
    List<BrowsedGoodsVO> findMyHistory(@Param("userId") Integer userId,
                                       @Param("offset") int offset,
                                       @Param("size") int size);
}
