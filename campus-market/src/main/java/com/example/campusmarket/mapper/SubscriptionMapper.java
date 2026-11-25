package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.GoodsWatch;
import com.example.campusmarket.entity.KeywordSubscription;
import com.example.campusmarket.entity.NotifyRecord;
import com.example.campusmarket.entity.TextbookWish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 消息提醒与个性化订阅：关键词订阅、心愿降价、教材上新、学期提醒去重。
 */
public interface SubscriptionMapper {

    /* ==================== 关键词订阅 ==================== */

    @Select("SELECT * FROM keyword_subscription WHERE user_id = #{userId} AND status = 1 " +
            "ORDER BY create_time DESC")
    List<KeywordSubscription> findKeywords(Integer userId);

    /** 同一用户同一关键词唯一，重复订阅返回 0 行 */
    @Insert("INSERT IGNORE INTO keyword_subscription(user_id, keyword) VALUES(#{userId}, #{keyword})")
    int insertKeyword(@Param("userId") Integer userId, @Param("keyword") String keyword);

    @Delete("DELETE FROM keyword_subscription WHERE id = #{id} AND user_id = #{userId}")
    int deleteKeyword(@Param("id") Integer id, @Param("userId") Integer userId);

    /** 新商品文本命中所有启用中的关键词订阅（排除卖家本人） */
    @Select("SELECT * FROM keyword_subscription WHERE status = 1 AND notify_new = 1 " +
            "AND user_id <> #{sellerId} AND INSTR(#{text}, keyword) > 0")
    List<KeywordSubscription> matchKeywords(@Param("text") String text, @Param("sellerId") Integer sellerId);

    @Update("UPDATE keyword_subscription SET match_count = match_count + 1, last_notify_at = NOW() " +
            "WHERE id = #{id}")
    int markKeywordMatched(Integer id);

    @Select("SELECT COUNT(*) FROM keyword_subscription WHERE user_id = #{userId} AND status = 1")
    int countKeywords(Integer userId);

    /* ==================== 心愿商品（降价提醒） ==================== */

    /** 收藏时自动关注该商品，首次记录当前价格 */
    @Insert("INSERT IGNORE INTO goods_watch(user_id, goods_id, last_price) " +
            "SELECT #{userId}, id, price FROM goods WHERE id = #{goodsId} AND is_deleted = 0")
    int watchFromGoods(@Param("userId") Integer userId, @Param("goodsId") Integer goodsId);

    @Delete("DELETE FROM goods_watch WHERE user_id = #{userId} AND goods_id = #{goodsId}")
    int unwatch(@Param("userId") Integer userId, @Param("goodsId") Integer goodsId);

    @Select("SELECT w.*, g.title AS title, g.cover_image AS cover_image, g.price AS current_price, " +
            "g.status AS goods_status, u.username AS seller_name " +
            "FROM goods_watch w " +
            "JOIN goods g ON w.goods_id = g.id " +
            "LEFT JOIN user u ON g.seller_id = u.id " +
            "WHERE w.user_id = #{userId} AND w.status = 1 AND g.is_deleted = 0 " +
            "ORDER BY w.create_time DESC")
    List<GoodsWatch> findWatches(Integer userId);

    @Select("SELECT * FROM goods_watch WHERE goods_id = #{goodsId} AND status = 1")
    List<GoodsWatch> findWatchers(Integer goodsId);

    @Update("UPDATE goods_watch SET last_price = #{price} WHERE id = #{id}")
    int updateWatchPrice(@Param("id") Integer id, @Param("price") BigDecimal price);

    @Update("UPDATE goods_watch SET last_price = #{price}, last_notify_at = NOW() WHERE id = #{id}")
    int updateWatchPriceNotified(@Param("id") Integer id, @Param("price") BigDecimal price);

    @Select("SELECT COUNT(*) FROM goods_watch w JOIN goods g ON w.goods_id = g.id " +
            "WHERE w.user_id = #{userId} AND w.status = 1 AND g.is_deleted = 0")
    int countWatches(Integer userId);

    /* ==================== 教材心愿（上新提醒） ==================== */

    @Insert("INSERT IGNORE INTO textbook_wish(user_id, textbook_id, textbook_title) " +
            "SELECT #{userId}, id, title FROM textbook WHERE id = #{textbookId} AND status = 1")
    int wishTextbook(@Param("userId") Integer userId, @Param("textbookId") Integer textbookId);

    @Delete("DELETE FROM textbook_wish WHERE id = #{id} AND user_id = #{userId}")
    int deleteWish(@Param("id") Integer id, @Param("userId") Integer userId);

    @Select("SELECT w.*, t.cover_image AS cover_image, t.subject AS subject, t.edition AS edition, " +
            "t.isbn AS isbn, t.title AS title " +
            "FROM textbook_wish w LEFT JOIN textbook t ON w.textbook_id = t.id " +
            "WHERE w.user_id = #{userId} AND w.status = 1 ORDER BY w.create_time DESC")
    List<TextbookWish> findWishes(Integer userId);

    /** 新商品命中教材心愿：优先按 textbook_id，其次标题包含教材书名 */
    @Select("<script>SELECT w.*, t.title AS title FROM textbook_wish w " +
            "JOIN textbook t ON w.textbook_id = t.id " +
            "WHERE w.status = 1 AND w.user_id &lt;&gt; #{sellerId} AND (" +
            "<if test='textbookId != null'>w.textbook_id = #{textbookId} OR </if>" +
            "INSTR(#{title}, t.title) &gt; 0)</script>")
    List<TextbookWish> matchTextbookWishes(@Param("textbookId") Integer textbookId,
                                           @Param("title") String title,
                                           @Param("sellerId") Integer sellerId);

    @Update("UPDATE textbook_wish SET last_notify_at = NOW() WHERE id = #{id}")
    int markWishMatched(Integer id);

    @Select("SELECT COUNT(*) FROM textbook_wish WHERE user_id = #{userId} AND status = 1")
    int countWishes(Integer userId);

    @Select("SELECT COUNT(*) FROM textbook_wish WHERE user_id = #{userId} AND textbook_id = #{textbookId} AND status = 1")
    int hasWish(@Param("userId") Integer userId, @Param("textbookId") Integer textbookId);

    /* ==================== 站内提醒记录 ==================== */

    @Insert("INSERT INTO notify_record(user_id, notify_type, title, content, link, goods_id, textbook_id, " +
            "keyword, ref_price) VALUES(#{userId}, #{notifyType}, #{title}, #{content}, #{link}, #{goodsId}, " +
            "#{textbookId}, #{keyword}, #{refPrice})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertNotify(NotifyRecord record);

    @Select("SELECT COUNT(*) FROM notify_record WHERE user_id = #{userId} AND notify_type = #{type} " +
            "AND goods_id = #{goodsId} AND keyword = #{keyword}")
    int countKeywordNotify(@Param("userId") Integer userId, @Param("type") String type,
                           @Param("goodsId") Integer goodsId, @Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM notify_record WHERE user_id = #{userId} AND notify_type = #{type} " +
            "AND goods_id = #{goodsId} AND ref_price = #{refPrice}")
    int countPriceNotify(@Param("userId") Integer userId, @Param("type") String type,
                         @Param("goodsId") Integer goodsId, @Param("refPrice") BigDecimal refPrice);

    @Select("SELECT COUNT(*) FROM notify_record WHERE user_id = #{userId} AND notify_type = #{type} " +
            "AND goods_id = #{goodsId}")
    int countGoodsNotify(@Param("userId") Integer userId, @Param("type") String type,
                         @Param("goodsId") Integer goodsId);

    @Select("SELECT * FROM notify_record WHERE user_id = #{userId} " +
            "ORDER BY create_time DESC, id DESC LIMIT #{offset}, #{size}")
    List<NotifyRecord> findNotifyPage(@Param("userId") Integer userId,
                                      @Param("offset") Integer offset,
                                      @Param("size") Integer size);

    @Select("SELECT COUNT(*) FROM notify_record WHERE user_id = #{userId}")
    int countNotify(Integer userId);

    @Select("SELECT COUNT(*) FROM notify_record WHERE user_id = #{userId} AND is_read = 0")
    int countUnread(Integer userId);

    @Update("UPDATE notify_record SET is_read = 1 WHERE id = #{id} AND user_id = #{userId}")
    int markRead(@Param("id") Integer id, @Param("userId") Integer userId);

    @Update("UPDATE notify_record SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    int markAllRead(Integer userId);

    @Delete("DELETE FROM notify_record WHERE id = #{id} AND user_id = #{userId}")
    int deleteNotify(@Param("id") Integer id, @Param("userId") Integer userId);

    /* ==================== 学期定时提醒去重 ==================== */

    /** 唯一键去重：返回 1 表示本次是首次发送，可以落提醒记录 */
    @Insert("INSERT IGNORE INTO semester_reminder_log(user_id, reminder_type, period_key) " +
            "VALUES(#{userId}, #{type}, #{periodKey})")
    int insertSemesterLog(@Param("userId") Integer userId,
                          @Param("type") String type,
                          @Param("periodKey") String periodKey);
}
