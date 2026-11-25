package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.GoodsDraft;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface GoodsDraftMapper {

    @Insert("INSERT INTO goods_draft(user_id, title, description, price, original_price, images, cover_image, " +
            "category_id, textbook_id, location, trade_mode, can_mail, can_deliver, exchange_supported, exchange_want) " +
            "VALUES(#{userId}, #{title}, #{description}, #{price}, #{originalPrice}, #{images}, #{coverImage}, " +
            "#{categoryId}, #{textbookId}, #{location}, #{tradeMode}, #{canMail}, #{canDeliver}, #{exchangeSupported}, #{exchangeWant})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(GoodsDraft draft);

    @Update("UPDATE goods_draft SET title = #{title}, description = #{description}, price = #{price}, " +
            "original_price = #{originalPrice}, images = #{images}, cover_image = #{coverImage}, category_id = #{categoryId}, " +
            "textbook_id = #{textbookId}, location = #{location}, trade_mode = #{tradeMode}, can_mail = #{canMail}, " +
            "can_deliver = #{canDeliver}, exchange_supported = #{exchangeSupported}, exchange_want = #{exchangeWant} " +
            "WHERE id = #{id} AND user_id = #{userId}")
    int update(GoodsDraft draft);

    @Select("SELECT d.*, c.name AS category_name FROM goods_draft d " +
            "LEFT JOIN category c ON d.category_id = c.id " +
            "WHERE d.id = #{id} AND d.user_id = #{userId}")
    GoodsDraft findByIdAndUser(@Param("id") Integer id, @Param("userId") Integer userId);

    @Select("SELECT d.*, c.name AS category_name FROM goods_draft d " +
            "LEFT JOIN category c ON d.category_id = c.id " +
            "WHERE d.user_id = #{userId} ORDER BY d.update_time DESC, d.id DESC LIMIT 50")
    List<GoodsDraft> findByUser(Integer userId);

    @Delete("DELETE FROM goods_draft WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUser(@Param("id") Integer id, @Param("userId") Integer userId);
}
