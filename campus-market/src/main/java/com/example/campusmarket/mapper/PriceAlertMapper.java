package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.PriceAlert;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface PriceAlertMapper {

    /**
     * 同分类（教材再收敛到同一本书）在售商品的近期均价。
     * excludeId 排除商品自身，避免自报价稀释样本。
     */
    @Select("<script>" +
            "SELECT ROUND(AVG(price), 2) FROM goods " +
            "WHERE status = 'on_sale' AND is_deleted = 0 AND price &gt; 0 " +
            "AND category_id = #{categoryId} " +
            "AND id != #{excludeId} " +
            "<if test='textbookId != null'>AND textbook_id = #{textbookId}</if>" +
            "</script>")
    BigDecimal avgPrice(@Param("categoryId") Integer categoryId,
                        @Param("textbookId") Integer textbookId,
                        @Param("excludeId") Integer excludeId);

    @Select("<script>" +
            "SELECT COUNT(*) FROM goods " +
            "WHERE status = 'on_sale' AND is_deleted = 0 AND price &gt; 0 " +
            "AND category_id = #{categoryId} " +
            "AND id != #{excludeId} " +
            "<if test='textbookId != null'>AND textbook_id = #{textbookId}</if>" +
            "</script>")
    int countSamples(@Param("categoryId") Integer categoryId,
                     @Param("textbookId") Integer textbookId,
                     @Param("excludeId") Integer excludeId);

    @Insert("INSERT INTO price_alert(goods_id, seller_id, title, price, avg_price, ratio, sample_size, level, status, note) " +
            "VALUES(#{goodsId}, #{sellerId}, #{title}, #{price}, #{avgPrice}, #{ratio}, #{sampleSize}, #{level}, #{status}, #{note})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PriceAlert alert);

    @Update("UPDATE goods SET price_alert = #{flag}, price_alert_note = #{note} WHERE id = #{goodsId}")
    int updateGoodsFlag(@Param("goodsId") Integer goodsId,
                        @Param("flag") Integer flag,
                        @Param("note") String note);

    @Select("SELECT * FROM price_alert WHERE goods_id = #{goodsId} ORDER BY id DESC LIMIT 1")
    PriceAlert findByGoods(Integer goodsId);

    /** 当前商品是否已有未处理预警（避免重复发布巡检时重复插入） */
    @Select("SELECT COUNT(*) FROM price_alert WHERE goods_id = #{goodsId} AND status = 'open'")
    int countOpenByGoods(Integer goodsId);

    @Select("<script>" +
            "SELECT p.*, u.username AS seller_name, u.avatar AS seller_avatar, g.cover_image AS goods_cover, g.status AS goods_status " +
            "FROM price_alert p " +
            "LEFT JOIN user u ON p.seller_id = u.id " +
            "LEFT JOIN goods g ON p.goods_id = g.id " +
            "WHERE 1 = 1 " +
            "<if test='status != null and status != \"\"'>AND p.status = #{status}</if> " +
            "ORDER BY p.id DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<PriceAlert> findPage(@Param("status") String status,
                              @Param("limit") int limit,
                              @Param("offset") int offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM price_alert WHERE 1 = 1 " +
            "<if test='status != null and status != \"\"'>AND status = #{status}</if>" +
            "</script>")
    int countPage(@Param("status") String status);

    @Update("UPDATE price_alert SET status = #{status}, note = #{note}, handler_id = #{handlerId}, handled_at = NOW() WHERE id = #{id}")
    int handle(@Param("id") Integer id,
               @Param("status") String status,
               @Param("note") String note,
               @Param("handlerId") Integer handlerId);

    @Select("SELECT * FROM price_alert WHERE id = #{id}")
    PriceAlert findById(Integer id);

    /** 巡检用：全部在售商品 */
    @Select("SELECT id, title, price, category_id, textbook_id, seller_id FROM goods WHERE status = 'on_sale' AND is_deleted = 0")
    List<com.example.campusmarket.entity.Goods> findOnSaleGoods();

    /** 当前在售且带价格预警的商品数 */
    @Select("SELECT COUNT(*) FROM goods WHERE price_alert = 1 AND status = 'on_sale' AND is_deleted = 0")
    int countAlertGoods();

    @Select("SELECT COUNT(*) FROM price_alert WHERE status = 'open'")
    int countOpen();

    @Select("SELECT COUNT(*) FROM price_alert")
    int countAll();
}
