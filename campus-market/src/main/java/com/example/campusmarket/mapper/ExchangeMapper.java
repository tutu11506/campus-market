package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.GoodsExchange;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ExchangeMapper {

    String BASE = "SELECT e.*, g.title AS goodsTitle, g.cover_image AS goodsCover, g.price AS goodsPrice, " +
            "g.seller_id AS ownerId, og.title AS offerTitle, og.cover_image AS offerCover, " +
            "og.price AS offerPrice, u.username AS requesterName " +
            "FROM goods_exchange e LEFT JOIN goods g ON e.goods_id = g.id " +
            "LEFT JOIN goods og ON e.offer_goods_id = og.id " +
            "LEFT JOIN user u ON e.requester_id = u.id ";

    @Insert("INSERT INTO goods_exchange(goods_id, requester_id, offer_goods_id, message) " +
            "VALUES(#{goodsId}, #{requesterId}, #{offerGoodsId}, #{message})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(GoodsExchange exchange);

    @Select("SELECT id FROM goods_exchange WHERE goods_id = #{goodsId} AND requester_id = #{requesterId}")
    Integer findIdByGoodsAndRequester(@Param("goodsId") Integer goodsId,
                                      @Param("requesterId") Integer requesterId);

    @Select(BASE + "WHERE e.id = #{id}")
    GoodsExchange findById(@Param("id") Integer id);

    /** 我收到的换物申请（我是目标商品卖家） */
    @Select(BASE + "WHERE g.seller_id = #{uid} ORDER BY e.create_time DESC LIMIT 30")
    List<GoodsExchange> findReceived(@Param("uid") Integer uid);

    /** 我发起的换物申请 */
    @Select(BASE + "WHERE e.requester_id = #{uid} ORDER BY e.create_time DESC LIMIT 30")
    List<GoodsExchange> findSent(@Param("uid") Integer uid);

    @Update("UPDATE goods_exchange SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") String status);

    @Update("UPDATE goods_exchange SET status = 'rejected' " +
            "WHERE goods_id = #{goodsId} AND id <> #{exceptId} AND status = 'pending'")
    int rejectOthers(@Param("goodsId") Integer goodsId, @Param("exceptId") Integer exceptId);

    @Update("UPDATE goods_exchange SET requester_done = 1 WHERE id = #{id}")
    int markRequesterDone(@Param("id") Integer id);

    @Update("UPDATE goods_exchange SET owner_done = 1 WHERE id = #{id}")
    int markOwnerDone(@Param("id") Integer id);
}
