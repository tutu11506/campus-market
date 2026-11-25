package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.ImageModeration;
import com.example.campusmarket.entity.ProhibitedItem;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ModerationMapper {

    /* ==================== 违禁品词库 ==================== */

    @Select("SELECT * FROM prohibited_item WHERE status = 1 ORDER BY id")
    List<ProhibitedItem> findActiveItems();

    @Select("SELECT * FROM prohibited_item ORDER BY id")
    List<ProhibitedItem> findAllItems();

    /* ==================== 识别记录 ==================== */

    @Insert("INSERT INTO image_moderation(uploader_id, goods_id, image_url, text_sample, verdict, risk_category, labels, confidence, source, status) " +
            "VALUES(#{uploaderId}, #{goodsId}, #{imageUrl}, #{textSample}, #{verdict}, #{riskCategory}, #{labels}, #{confidence}, #{source}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRecord(ImageModeration record);

    @Update("UPDATE image_moderation SET goods_id = #{goodsId} WHERE id = #{id}")
    int attachGoods(@Param("id") Integer id, @Param("goodsId") Integer goodsId);

    @Select("SELECT * FROM image_moderation WHERE id = #{id}")
    ImageModeration findRecordById(Integer id);

    @Select("<script>" +
            "SELECT m.*, u.username AS uploader_name, g.title AS goods_title, g.cover_image AS goods_cover " +
            "FROM image_moderation m " +
            "LEFT JOIN user u ON m.uploader_id = u.id " +
            "LEFT JOIN goods g ON m.goods_id = g.id " +
            "WHERE 1 = 1 " +
            "<if test='verdict != null and verdict != \"\"'>AND m.verdict = #{verdict}</if> " +
            "ORDER BY m.id DESC LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    List<ImageModeration> findRecordPage(@Param("verdict") String verdict,
                                        @Param("limit") int limit,
                                        @Param("offset") int offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM image_moderation " +
            "WHERE 1 = 1 " +
            "<if test='verdict != null and verdict != \"\"'>AND verdict = #{verdict}</if>" +
            "</script>")
    int countRecordPage(@Param("verdict") String verdict);

    @Update("UPDATE image_moderation SET status = #{status}, handler_id = #{handlerId} WHERE id = #{id}")
    int updateRecordStatus(@Param("id") Integer id,
                           @Param("status") String status,
                           @Param("handlerId") Integer handlerId);

    @Update("UPDATE goods SET risk_level = #{level} WHERE id = #{goodsId}")
    int updateGoodsRisk(@Param("goodsId") Integer goodsId, @Param("level") String level);

    /** 按判定结果统计（管理端概览） */
    @Select("SELECT verdict, COUNT(*) AS count FROM image_moderation GROUP BY verdict")
    List<Map<String, Object>> countByVerdict();

    /** 待人工确认的复核记录数 */
    @Select("SELECT COUNT(*) FROM image_moderation WHERE verdict = 'review' AND status = 'auto'")
    int countPendingReview();
}
