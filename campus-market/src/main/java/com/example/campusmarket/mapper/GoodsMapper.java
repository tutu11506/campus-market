package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.Goods;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface GoodsMapper {

    // 新增商品
    @Insert("INSERT INTO goods(title, description, price, original_price, base_price, list_price, coupon_amount, " +
            "images, cover_image, category_id, textbook_id, seller_id, location, can_mail, can_deliver, trade_mode, " +
            "exchange_supported, exchange_want, risk_level, risk_reason, price_alert, price_alert_note) " +
            "VALUES(#{title}, #{description}, #{price}, #{originalPrice}, #{basePrice}, #{listPrice}, #{couponAmount}, " +
            "#{images}, #{coverImage}, #{categoryId}, #{textbookId}, #{sellerId}, #{location}, #{canMail}, #{canDeliver}, " +
            "#{tradeMode}, #{exchangeSupported}, #{exchangeWant}, " +
            "IFNULL(#{riskLevel}, 'none'), #{riskReason}, IFNULL(#{priceAlert}, 0), #{priceAlertNote})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Goods goods);

    // 根据ID查询详情（关联卖家和分类信息）
    @Select("SELECT g.*, u.username as seller_name, u.avatar as seller_avatar, u.student_no as seller_student_no, c.name as category_name, " +
            "(SELECT COUNT(*) FROM comment cm WHERE cm.goods_id = g.id) AS comment_count, " +
            "(SELECT ROUND(AVG(cm.rating), 1) FROM comment cm WHERE cm.goods_id = g.id) AS avg_rating " +
            "FROM goods g " +
            "LEFT JOIN user u ON g.seller_id = u.id " +
            "LEFT JOIN category c ON g.category_id = c.id " +
            "WHERE g.id = #{id} AND g.is_deleted = 0")
    Goods findById(Integer id);

    // 商品列表（分页 + 分类筛选 + 关键词搜索 + 价格区间 + 排序）
    @Select("<script>" +
            "SELECT g.*, u.username as seller_name, u.avatar as seller_avatar, u.student_no as seller_student_no, c.name as category_name, " +
            "(SELECT COUNT(*) FROM comment cm WHERE cm.goods_id = g.id) AS comment_count, " +
            "(SELECT ROUND(AVG(cm.rating), 1) FROM comment cm WHERE cm.goods_id = g.id) AS avg_rating " +
            "FROM goods g " +
            "LEFT JOIN user u ON g.seller_id = u.id " +
            "LEFT JOIN category c ON g.category_id = c.id " +
            "WHERE g.is_deleted = 0 AND g.status = 'on_sale' " +
            "<if test='categoryId != null'>AND g.category_id = #{categoryId}</if> " +
            "<if test='sellerId != null'>AND g.seller_id = #{sellerId}</if> " +
            "<if test='location != null and location != \"\"'>AND g.location = #{location}</if> " +
            "<if test='canMail != null and canMail == 1'>AND g.can_mail = 1</if> " +
            "<if test='canDeliver != null and canDeliver == 1'>AND g.can_deliver = 1</if> " +
            "<if test='exchangeSupported != null and exchangeSupported == 1'>AND (g.exchange_supported = 1 OR g.trade_mode = 'exchange')</if> " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "<choose>" +
            "<when test='keyword.length() >= 2'>AND MATCH(g.title, g.description) AGAINST (CONCAT('\"', #{keyword}, '\"') IN BOOLEAN MODE)</when>" +
            "<otherwise>AND g.title LIKE CONCAT('%', #{keyword}, '%')</otherwise>" +
            "</choose></if> " +
            "<if test='minPrice != null'>AND g.price &gt;= #{minPrice}</if> " +
            "<if test='maxPrice != null'>AND g.price &lt;= #{maxPrice}</if> " +
            "<choose>" +
            "<when test='sort == \"price_asc\"'>ORDER BY g.price ASC</when> " +
            "<when test='sort == \"price_desc\"'>ORDER BY g.price DESC</when> " +
            "<when test='sort == \"views\"'>ORDER BY g.view_count DESC</when> " +
            "<otherwise>ORDER BY g.create_time DESC</otherwise>" +
            "</choose> " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<Goods> findList(@Param("categoryId") Integer categoryId,
                         @Param("keyword") String keyword,
                         @Param("minPrice") java.math.BigDecimal minPrice,
                         @Param("maxPrice") java.math.BigDecimal maxPrice,
                         @Param("sellerId") Integer sellerId,
                         @Param("sort") String sort,
                         @Param("offset") Integer offset,
                         @Param("size") Integer size,
                         @Param("location") String location,
                         @Param("canMail") Integer canMail,
                         @Param("canDeliver") Integer canDeliver,
                         @Param("exchangeSupported") Integer exchangeSupported);

    // 统计总数（与 findList 条件保持一致）
    @Select("<script>" +
            "SELECT COUNT(*) FROM goods g LEFT JOIN user u ON g.seller_id = u.id WHERE g.is_deleted = 0 AND g.status = 'on_sale' " +
            "<if test='categoryId != null'>AND g.category_id = #{categoryId}</if> " +
            "<if test='sellerId != null'>AND g.seller_id = #{sellerId}</if> " +
            "<if test='location != null and location != \"\"'>AND g.location = #{location}</if> " +
            "<if test='canMail != null and canMail == 1'>AND g.can_mail = 1</if> " +
            "<if test='canDeliver != null and canDeliver == 1'>AND g.can_deliver = 1</if> " +
            "<if test='exchangeSupported != null and exchangeSupported == 1'>AND (g.exchange_supported = 1 OR g.trade_mode = 'exchange')</if> " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "<choose>" +
            "<when test='keyword.length() >= 2'>AND MATCH(g.title, g.description) AGAINST (CONCAT('\"', #{keyword}, '\"') IN BOOLEAN MODE)</when>" +
            "<otherwise>AND g.title LIKE CONCAT('%', #{keyword}, '%')</otherwise>" +
            "</choose></if> " +
            "<if test='minPrice != null'>AND g.price &gt;= #{minPrice}</if> " +
            "<if test='maxPrice != null'>AND g.price &lt;= #{maxPrice}</if> " +
            "</script>")
    int count(@Param("categoryId") Integer categoryId,
              @Param("keyword") String keyword,
              @Param("minPrice") java.math.BigDecimal minPrice,
              @Param("maxPrice") java.math.BigDecimal maxPrice,
              @Param("sellerId") Integer sellerId,
              @Param("location") String location,
              @Param("canMail") Integer canMail,
              @Param("canDeliver") Integer canDeliver,
              @Param("exchangeSupported") Integer exchangeSupported);

    // 根据卖家ID查询商品
    @Select("SELECT * FROM goods WHERE seller_id = #{sellerId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<Goods> findBySellerId(Integer sellerId);

    // 分页查询某个卖家的商品
    @Select("SELECT * FROM goods WHERE seller_id = #{sellerId} AND is_deleted = 0 ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<Goods> findBySellerIdPage(@Param("sellerId") Integer sellerId,
                                   @Param("offset") Integer offset,
                                   @Param("size") Integer size);

    // 统计某个卖家的商品数
    @Select("SELECT COUNT(*) FROM goods WHERE seller_id = #{sellerId} AND is_deleted = 0")
    int countBySellerId(@Param("sellerId") Integer sellerId);

    // 修改商品
    @Update("UPDATE goods SET title = #{title}, description = #{description}, price = #{price}, " +
            "original_price = #{originalPrice}, base_price = #{basePrice}, list_price = #{listPrice}, " +
            "coupon_amount = #{couponAmount}, images = #{images}, cover_image = #{coverImage}, " +
            "category_id = #{categoryId}, location = #{location}, " +
            "risk_level = IFNULL(#{riskLevel}, 'none'), risk_reason = #{riskReason} WHERE id = #{id}")
    int update(Goods goods);

    // 修改商品状态（上下架、售出）
    @Update("UPDATE goods SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") String status);

    // 下单锁定：仅当商品仍在售时才能锁定，防止并发重复售出
    @Update("UPDATE goods SET status = 'locked' WHERE id = #{id} AND status = 'on_sale'")
    int lockIfOnSale(Integer id);

    // 增加浏览量
    @Update("UPDATE goods SET view_count = view_count + 1 WHERE id = #{id}")
    int addViewCount(Integer id);

    // 删除商品（逻辑删除）
    @Update("UPDATE goods SET is_deleted = 1 WHERE id = #{id}")
    int delete(Integer id);

    // 管理员统计商品总数（避免加载全表）
    @Select("SELECT COUNT(*) FROM goods WHERE is_deleted = 0")
    int countAllForAdmin();

    // 管理员查询所有商品（含下架的）
    @Select("SELECT g.*, u.username as seller_name, c.name as category_name " +
            "FROM goods g " +
            "LEFT JOIN user u ON g.seller_id = u.id " +
            "LEFT JOIN category c ON g.category_id = c.id " +
            "WHERE g.is_deleted = 0 " +
            "ORDER BY g.create_time DESC")
    List<Goods> findAllForAdmin();

    // 管理员分页查询所有商品（含下架的）
    @Select("SELECT g.*, u.username as seller_name, c.name as category_name " +
            "FROM goods g " +
            "LEFT JOIN user u ON g.seller_id = u.id " +
            "LEFT JOIN category c ON g.category_id = c.id " +
            "WHERE g.is_deleted = 0 " +
            "ORDER BY g.create_time DESC LIMIT #{offset}, #{size}")
    List<Goods> findAllForAdminPage(@Param("offset") Integer offset, @Param("size") Integer size);

    // 各分类商品数量（管理后台看板）
    @Select("SELECT c.name AS name, COUNT(g.id) AS count FROM category c " +
            "LEFT JOIN goods g ON g.category_id = c.id AND g.is_deleted = 0 " +
            "GROUP BY c.id, c.name ORDER BY count DESC")
    List<Map<String, Object>> countGoodsByCategory();

    // ================== 卖家数据看板 SQL ==================

    @Select("SELECT COUNT(*) FROM goods WHERE seller_id = #{sellerId} AND is_deleted = 0")
    int countBySeller(@Param("sellerId") Integer sellerId);

    @Select("SELECT COUNT(*) FROM goods WHERE seller_id = #{sellerId} AND is_deleted = 0 AND status = #{status}")
    int countBySellerAndStatus(@Param("sellerId") Integer sellerId, @Param("status") String status);

    @Select("SELECT COALESCE(SUM(view_count), 0) FROM goods WHERE seller_id = #{sellerId} AND is_deleted = 0")
    int sumViewCountBySeller(@Param("sellerId") Integer sellerId);

    // 卖家已完成订单的总交易额
    @Select("SELECT COALESCE(SUM(o.price), 0) FROM `order` o " +
            "WHERE o.seller_id = #{sellerId} AND o.status = 'completed' AND o.pay_time IS NOT NULL")
    BigDecimal sumSellerCompletedAmount(@Param("sellerId") Integer sellerId);

    @Select("SELECT COUNT(*) FROM `order` WHERE seller_id = #{sellerId} AND status = 'completed'")
    int countSellerCompletedOrders(@Param("sellerId") Integer sellerId);

    @Select("SELECT COUNT(*) FROM `order` WHERE seller_id = #{sellerId} AND status IN ('cancelled', 'returned')")
    int countSellerFailedOrders(@Param("sellerId") Integer sellerId);

    // 卖家分类分布
    @Select("SELECT c.name AS name, COUNT(g.id) AS count FROM goods g " +
            "JOIN category c ON g.category_id = c.id " +
            "WHERE g.seller_id = #{sellerId} AND g.is_deleted = 0 " +
            "GROUP BY c.id, c.name ORDER BY count DESC")
    List<Map<String, Object>> sellerCategoryDist(@Param("sellerId") Integer sellerId);

    // 卖家 Top N 热门商品
    @Select("SELECT id, title, view_count AS viewCount, price, cover_image AS coverImage, status " +
            "FROM goods WHERE seller_id = #{sellerId} AND is_deleted = 0 " +
            "ORDER BY view_count DESC LIMIT #{n}")
    List<Map<String, Object>> sellerTopGoods(@Param("sellerId") Integer sellerId, @Param("n") int n);

    // 卖家最近 7 天成交趋势
    @Select("SELECT DATE_FORMAT(o.pay_time, '%m-%d') AS date, COUNT(*) AS count, " +
            "COALESCE(SUM(o.price), 0) AS amount " +
            "FROM `order` o WHERE o.seller_id = #{sellerId} AND o.status = 'completed' AND o.pay_time IS NOT NULL " +
            "AND o.pay_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "GROUP BY DATE_FORMAT(o.pay_time, '%m-%d') ORDER BY date")
    List<Map<String, Object>> sellerRecentTrend(@Param("sellerId") Integer sellerId);

    // ================== 同款比价 SQL ==================

    /**
     * 找校园内同款在售商品。
     * 策略：同分类 + 标题关键词重叠度高 + 价格区间合理（不超过 +/- 50%）+ 不是卖家自己的。
     * 返回最多 4 件，按价格升序 —— 让买家一眼看到最低价。
     */
    @Select("SELECT g.id, g.title, g.price, g.cover_image AS coverImage, g.view_count AS viewCount, " +
            "g.status, u.username AS sellerName, u.student_no AS sellerStudentNo " +
            "FROM goods g JOIN user u ON g.seller_id = u.id " +
            "WHERE g.is_deleted = 0 AND g.status = 'on_sale' " +
            "AND g.category_id = #{categoryId} " +
            "AND g.id != #{excludeGoodsId} " +
            "AND g.seller_id != #{excludeSellerId} " +
            "AND g.price BETWEEN #{minPrice} AND #{maxPrice} " +
            "ORDER BY g.price ASC LIMIT 4")
    List<Map<String, Object>> findCampusSimilar(@Param("categoryId") Integer categoryId,
                                                @Param("excludeGoodsId") Integer excludeGoodsId,
                                                @Param("excludeSellerId") Integer excludeSellerId,
                                                @Param("minPrice") BigDecimal minPrice,
                                                @Param("maxPrice") BigDecimal maxPrice);

    // ================== 校园生活 / 卖家信用档案 SQL ==================

    /**
     * 校园面交点热力：按交易地点聚合在售商品，热度和均价一次算出。
     */
    @Select("SELECT location AS name, COUNT(*) AS count, ROUND(AVG(price), 0) AS avgPrice " +
            "FROM goods WHERE is_deleted = 0 AND status = 'on_sale' " +
            "AND location IS NOT NULL AND location <> '' " +
            "GROUP BY location ORDER BY count DESC, name LIMIT #{limit}")
    List<Map<String, Object>> pickupSpots(@Param("limit") int limit);

    /**
     * 校园交易时段分布：按小时聚合订单，用于识别下课/午饭/晚自习后的交易高峰。
     */
    @Select("SELECT HOUR(COALESCE(pay_time, create_time)) AS hour, COUNT(*) AS count " +
            "FROM `order` WHERE status IN ('completed', 'dealing', 'pending') " +
            "GROUP BY HOUR(COALESCE(pay_time, create_time))")
    List<Map<String, Object>> activeHours();

    /**
     * 卖家信用档案：信用分 + 历史成交 + 在售件数 + 注册天数，一次查询取全。
     */
    @Select("SELECT u.credit AS credit, " +
            "(SELECT COUNT(*) FROM `order` o WHERE o.seller_id = #{sellerId} AND o.status = 'completed') AS soldCount, " +
            "(SELECT COUNT(*) FROM goods g WHERE g.seller_id = #{sellerId} AND g.is_deleted = 0 AND g.status = 'on_sale') AS onSaleCount, " +
            "DATEDIFF(NOW(), u.create_time) AS joinDays " +
            "FROM user u WHERE u.id = #{sellerId}")
    Map<String, Object> findSellerProfile(@Param("sellerId") Integer sellerId);

    // ================== 校园行情价 / 捡漏榜 SQL ==================

    /**
     * 同类在售商品价格（排除自己），升序 —— 用于算中位数、四分位与行情区间。
     */
    @Select("SELECT price FROM goods WHERE is_deleted = 0 AND status = 'on_sale' " +
            "AND category_id = #{categoryId} AND id != #{excludeGoodsId} AND price > 0 ORDER BY price")
    List<BigDecimal> findPeerPrices(@Param("categoryId") Integer categoryId,
                                    @Param("excludeGoodsId") Integer excludeGoodsId);

    /**
     * 近半年同分类真实成交统计：成交单数 + 成交均价。
     */
    @Select("SELECT COUNT(*) AS cnt, COALESCE(ROUND(AVG(o.price), 0), 0) AS avgPrice " +
            "FROM `order` o JOIN goods g ON o.goods_id = g.id " +
            "WHERE o.status = 'completed' AND o.pay_time >= DATE_SUB(NOW(), INTERVAL 180 DAY) " +
            "AND g.category_id = #{categoryId}")
    Map<String, Object> categoryDealStats(@Param("categoryId") Integer categoryId);

    /**
     * 校园行情总览：每个分类的在售件数、均价、最低/最高价。
     * 让同学一眼看清"校园里各类东西大概什么价"。
     */
    @Select("SELECT c.id AS categoryId, c.name AS categoryName, COUNT(g.id) AS count, " +
            "COALESCE(ROUND(AVG(g.price), 0), 0) AS avgPrice, " +
            "COALESCE(MIN(g.price), 0) AS minPrice, COALESCE(MAX(g.price), 0) AS maxPrice " +
            "FROM category c LEFT JOIN goods g ON g.category_id = c.id " +
            "AND g.status = 'on_sale' AND g.is_deleted = 0 " +
            "GROUP BY c.id, c.name ORDER BY count DESC")
    List<Map<String, Object>> categoryMarketOverview();

    /**
     * 校园捡漏榜：当前价低于同分类在售均价 40% 以上的商品，按折扣力度排序。
     * 校园里信息不对称更严重，低价好货往往还没被看到 —— 这个榜单专门把它们捞出来。
     */
    @Select("SELECT g.id, g.title, g.price, g.cover_image AS coverImage, g.images, " +
            "g.category_id AS categoryId, g.view_count AS viewCount, c.name AS categoryName, " +
            "(SELECT ROUND(AVG(g2.price), 0) FROM goods g2 " +
            " WHERE g2.category_id = g.category_id AND g2.status = 'on_sale' AND g2.is_deleted = 0 AND g2.id != g.id) AS categoryAvg " +
            "FROM goods g LEFT JOIN category c ON g.category_id = c.id " +
            "WHERE g.status = 'on_sale' AND g.is_deleted = 0 AND g.price > 0 " +
            "AND g.price < (SELECT AVG(g2.price) * 0.6 FROM goods g2 " +
            "               WHERE g2.category_id = g.category_id AND g2.status = 'on_sale' AND g2.is_deleted = 0 AND g2.id != g.id) " +
            "ORDER BY g.price / (SELECT AVG(g2.price) FROM goods g2 " +
            "                     WHERE g2.category_id = g.category_id AND g2.status = 'on_sale' AND g2.is_deleted = 0 AND g2.id != g.id) ASC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> findDealRadar(@Param("limit") int limit);

    // ================== 教材循环站 / 校园估价器 SQL ==================

    /**
     * 教材循环站：教材分类（category_id = 1）在售商品的书名首词与现价。
     * 只取空格前的书名首词，避免"机械工业出版社""电子工业出版社"等出版社名污染学科归类。
     */
    @Select("SELECT SUBSTRING_INDEX(title, ' ', 1) AS book, price FROM goods " +
            "WHERE category_id = 1 AND status = 'on_sale' AND is_deleted = 0 AND price > 0")
    List<Map<String, Object>> findTextbookRows();

    /**
     * 校园估价器样本：同分类在售商品的现价 + 原价，用于计算校园保值率分位。
     */
    @Select("SELECT g.price AS price, g.original_price AS originalPrice, c.name AS categoryName " +
            "FROM goods g LEFT JOIN category c ON g.category_id = c.id " +
            "WHERE g.category_id = #{categoryId} AND g.status = 'on_sale' AND g.is_deleted = 0 " +
            "AND g.price > 0 AND g.original_price > 0")
    List<Map<String, Object>> findValuationSamples(@Param("categoryId") Integer categoryId);

    // ================== 同地点交易榜 / 成交风向标 SQL ==================

    /**
     * 同地点在售汇总：件数、均价、最低价、各分类 TOP3。
     */
    @Select("SELECT c.name AS categoryName, COUNT(g.id) AS count, ROUND(AVG(g.price), 0) AS avgPrice " +
            "FROM goods g JOIN category c ON g.category_id = c.id " +
            "WHERE g.location = #{location} AND g.status = 'on_sale' AND g.is_deleted = 0 " +
            "GROUP BY c.id, c.name ORDER BY count DESC LIMIT 3")
    List<Map<String, Object>> locationTopCategories(@Param("location") String location);

    /**
     * 同地点在售整体统计。
     */
    @Select("SELECT COUNT(*) AS count, COALESCE(ROUND(AVG(price), 0), 0) AS avgPrice, " +
            "COALESCE(MIN(price), 0) AS minPrice, COALESCE(MAX(price), 0) AS maxPrice " +
            "FROM goods WHERE location = #{location} AND status = 'on_sale' AND is_deleted = 0")
    Map<String, Object> locationOnSaleStats(@Param("location") String location);

    /**
     * 同地点近 90 天完成订单数与成交总额。
     */
    @Select("SELECT COUNT(*) AS dealCount, COALESCE(ROUND(SUM(o.price), 0), 0) AS dealAmount " +
            "FROM `order` o JOIN goods g ON o.goods_id = g.id " +
            "WHERE o.status = 'completed' AND o.pay_time >= DATE_SUB(NOW(), INTERVAL 90 DAY) " +
            "AND g.location = #{location}")
    Map<String, Object> locationDealStats(@Param("location") String location);

    /**
     * 同地点在售商品列表（limit 8）。
     */
    @Select("SELECT g.id, g.title, g.price, g.cover_image AS coverImage, g.view_count AS viewCount, " +
            "c.name AS categoryName FROM goods g LEFT JOIN category c ON g.category_id = c.id " +
            "WHERE g.location = #{location} AND g.status = 'on_sale' AND g.is_deleted = 0 " +
            "ORDER BY g.create_time DESC LIMIT #{limit}")
    List<Map<String, Object>> locationSampleGoods(@Param("location") String location, @Param("limit") int limit);

    /**
     * 成交风向标：各分类按周聚合完成订单均价与成交量。
     */
    @Select("SELECT DATE_FORMAT(o.pay_time, '%Y-%m-%d') AS weekStart, " +
            "COUNT(*) AS dealCount, ROUND(AVG(o.price), 0) AS avgPrice " +
            "FROM `order` o JOIN goods g ON o.goods_id = g.id " +
            "WHERE o.status = 'completed' AND o.pay_time IS NOT NULL " +
            "AND o.pay_time >= DATE_SUB(NOW(), INTERVAL 12 WEEK) " +
            "GROUP BY DATE_FORMAT(o.pay_time, '%Y-%m-%d') " +
            "ORDER BY weekStart")
    List<Map<String, Object>> dealTrendByWeek();

    /**
     * 各分类近 12 周的周成交量与均价。
     */
    @Select("SELECT YEARWEEK(o.pay_time, 1) AS weekKey, c.name AS categoryName, " +
            "COUNT(*) AS dealCount, ROUND(AVG(o.price), 0) AS avgPrice " +
            "FROM `order` o JOIN goods g ON o.goods_id = g.id " +
            "JOIN category c ON g.category_id = c.id " +
            "WHERE o.status = 'completed' AND o.pay_time IS NOT NULL " +
            "AND o.pay_time >= DATE_SUB(NOW(), INTERVAL 12 WEEK) " +
            "GROUP BY YEARWEEK(o.pay_time, 1), c.id, c.name " +
            "ORDER BY weekKey, dealCount DESC")
    List<Map<String, Object>> dealTrendByWeekCategory();

    /**
     * 各分类在售均价基准（用于成交风向标对比）。
     */
    @Select("SELECT c.name AS categoryName, ROUND(AVG(g.price), 0) AS avgOnSalePrice, " +
            "COUNT(g.id) AS onSaleCount FROM goods g JOIN category c ON g.category_id = c.id " +
            "WHERE g.status = 'on_sale' AND g.is_deleted = 0 GROUP BY c.id, c.name " +
            "ORDER BY onSaleCount DESC")
    List<Map<String, Object>> categoryOnSaleBaseline();

    // ================== 宿舍楼下好货 / 求购热力榜 SQL ==================

    /**
     * 所有在售商品的 location 聚合：件数 + 均价 + 分类 TOP1。
     * 返回原始 location，Java 层再归类为"北区宿舍"/"南区食堂"等更粗的区域。
     */
    @Select("SELECT g.location AS location, COUNT(*) AS count, ROUND(AVG(g.price), 0) AS avgPrice, " +
            "(SELECT c.name FROM goods g2 JOIN category c ON g2.category_id = c.id " +
            " WHERE g2.location = g.location AND g2.status = 'on_sale' AND g2.is_deleted = 0 " +
            " GROUP BY c.id ORDER BY COUNT(*) DESC LIMIT 1) AS topCategory " +
            "FROM goods g " +
            "WHERE g.is_deleted = 0 AND g.status = 'on_sale' " +
            "AND g.location IS NOT NULL AND g.location <> '' " +
            "GROUP BY g.location ORDER BY count DESC")
    List<Map<String, Object>> locationAggregation();

    /**
     * 某个 location 的在售商品样本（limit 6）。
     */
    @Select("SELECT g.id, g.title, g.price, g.cover_image AS coverImage, c.name AS categoryName " +
            "FROM goods g LEFT JOIN category c ON g.category_id = c.id " +
            "WHERE g.location = #{location} AND g.status = 'on_sale' AND g.is_deleted = 0 " +
            "ORDER BY g.create_time DESC LIMIT #{limit}")
    List<Map<String, Object>> locationSample(@Param("location") String location, @Param("limit") int limit);

    /**
     * 求购热力榜：按分类统计浏览人数（去重 user）、浏览次数、在售件数、近 30 天成交数。
     * "浏览多、在售少、成交少" = 大家都想要但没人卖，就是求购缺口最大的品类。
     * 热度分 = log(浏览人数+1) / (在售件数+1) * 需求因子，Java 层再做二次排序。
     */
    @Select("SELECT c.id AS categoryId, c.name AS categoryName, " +
            "COUNT(DISTINCT bh.user_id) AS browseUsers, " +
            "COALESCE(SUM(bh.browse_count), 0) AS browseCount, " +
            "(SELECT COUNT(*) FROM goods g2 WHERE g2.category_id = c.id " +
            " AND g2.status = 'on_sale' AND g2.is_deleted = 0) AS onSaleCount, " +
            "(SELECT COUNT(*) FROM `order` o JOIN goods g3 ON o.goods_id = g3.id " +
            " WHERE g3.category_id = c.id AND o.status = 'completed' " +
            " AND o.pay_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)) AS dealCount30d " +
            "FROM category c " +
            "LEFT JOIN goods g ON g.category_id = c.id AND g.is_deleted = 0 " +
            "LEFT JOIN browse_history bh ON bh.goods_id = g.id " +
            "GROUP BY c.id, c.name HAVING browseUsers > 0 " +
            "ORDER BY browseUsers DESC LIMIT #{limit}")
    List<Map<String, Object>> demandHeatByCategory(@Param("limit") int limit);

    /**
     * 宿舍楼下好货：按宿舍区关键词提取 location 后聚合。
     * 这里直接 SQL 归类：包含宿舍区/楼栋号关键词 → 归类到宿舍区前缀（如"北区宿舍"）。
     */
    @Select("SELECT " +
            "CASE " +
            " WHEN location REGEXP '(北区|南区|东区|西区)[0-9]*栋' THEN " +
            "   CONCAT(SUBSTRING_INDEX(location, '栋', 1), '栋') " +
            " WHEN location LIKE '%宿舍%' THEN " +
            "   CONCAT(CASE WHEN location LIKE '%北区%' THEN '北区' " +
            "               WHEN location LIKE '%南区%' THEN '南区' " +
            "               WHEN location LIKE '%东区%' THEN '东区' " +
            "               WHEN location LIKE '%西区%' THEN '西区' " +
            "               ELSE '' END, " +
            "         '宿舍区') " +
            " WHEN location REGEXP '[0-9]+号楼' THEN " +
            "   SUBSTRING_INDEX(location, '号楼', 1) " +
            " ELSE NULL " +
            "END AS dormArea, " +
            "COUNT(*) AS count, " +
            "ROUND(AVG(price), 0) AS avgPrice, " +
            "MIN(price) AS minPrice " +
            "FROM goods " +
            "WHERE is_deleted = 0 AND status = 'on_sale' " +
            "AND location IS NOT NULL AND location <> '' " +
            "AND (" +
            "  location LIKE '%宿舍%' " +
            "  OR location REGEXP '(北区|南区|东区|西区)[0-9]+栋' " +
            "  OR location REGEXP '[0-9]+号楼'" +
            ") " +
            "GROUP BY dormArea " +
            "ORDER BY count DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> dormAreaStats(@Param("limit") int limit);

    /**
     * 某个宿舍区的在售商品样本（location 匹配宿舍区前缀）。
     */
    @Select("SELECT g.id, g.title, g.price, g.cover_image AS coverImage, g.location, " +
            "c.name AS categoryName " +
            "FROM goods g LEFT JOIN category c ON g.category_id = c.id " +
            "WHERE g.status = 'on_sale' AND g.is_deleted = 0 " +
            "AND g.location LIKE CONCAT(#{areaPrefix}, '%') " +
            "ORDER BY g.create_time DESC LIMIT #{limit}")
    List<Map<String, Object>> dormAreaSample(@Param("areaPrefix") String areaPrefix, @Param("limit") int limit);

    // ================== 毕业清仓专区 / 教材价格指数 SQL ==================

    /**
     * 应届毕业生的在售商品：卖家学号前4位 = 当前年-4，且是毕业季（5-8月）。
     * 返回 seller_student_no 让 Java 层算"离校倒计时"。
     */
    @Select("SELECT g.id, g.title, g.price, g.cover_image AS coverImage, g.create_time AS createTime, " +
            "u.username AS sellerName, u.student_no AS sellerStudentNo " +
            "FROM goods g JOIN user u ON g.seller_id = u.id " +
            "WHERE g.is_deleted = 0 AND g.status = 'on_sale' " +
            "AND u.student_no REGEXP '^[0-9]{8,}' " +
            "AND CAST(SUBSTRING(u.student_no, 1, 4) AS SIGNED) + 4 = YEAR(CURDATE()) " +
            "AND MONTH(CURDATE()) BETWEEN 5 AND 8 " +
            "ORDER BY g.price ASC, g.create_time DESC LIMIT #{limit}")
    List<Map<String, Object>> graduationSaleGoods(@Param("limit") int limit);

    /**
     * 教材分类（category_id = 1）按周统计：完成订单均价 + 在售均价 + 成交量。
     * 用于计算"教材价格指数"——开学季涨价、毕业季跌价。
     */
    @Select("SELECT " +
            "YEARWEEK(o.pay_time, 1) AS weekKey, " +
            "MIN(DATE(o.pay_time)) AS weekStart, " +
            "COUNT(*) AS dealCount, " +
            "ROUND(AVG(o.price), 0) AS dealAvgPrice, " +
            "ROUND((SELECT AVG(g2.price) FROM goods g2 " +
            "  WHERE g2.category_id = 1 AND g2.status = 'on_sale' AND g2.is_deleted = 0), 0) AS onSaleAvg " +
            "FROM `order` o JOIN goods g ON o.goods_id = g.id " +
            "WHERE o.status = 'completed' AND o.pay_time IS NOT NULL " +
            "AND g.category_id = 1 " +
            "AND o.pay_time >= DATE_SUB(NOW(), INTERVAL 12 WEEK) " +
            "GROUP BY YEARWEEK(o.pay_time, 1) " +
            "ORDER BY weekKey DESC " +
            "LIMIT 8")
    List<Map<String, Object>> textbookWeeklyStats();

    /**
     * 教材分类近 30 天真实成交统计 + 当前在售均价。
     */
    @Select("SELECT " +
            "COUNT(*) AS dealCount30d, " +
            "COALESCE(ROUND(AVG(o.price), 0), 0) AS dealAvg30d, " +
            "(SELECT ROUND(AVG(g2.price), 0) FROM goods g2 " +
            "  WHERE g2.category_id = 1 AND g2.status = 'on_sale' AND g2.is_deleted = 0) AS onSaleAvgNow, " +
            "(SELECT COUNT(*) FROM goods g2 " +
            "  WHERE g2.category_id = 1 AND g2.status = 'on_sale' AND g2.is_deleted = 0) AS onSaleCountNow " +
            "FROM `order` o JOIN goods g ON o.goods_id = g.id " +
            "WHERE o.status = 'completed' AND o.pay_time IS NOT NULL " +
            "AND g.category_id = 1 " +
            "AND o.pay_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)")
    Map<String, Object> textbookCurrentStats();

    /**
     * 教材分类上一个 30 天的成交均价，用于对比涨跌。
     */
    @Select("SELECT COALESCE(ROUND(AVG(o.price), 0), 0) AS prevAvg " +
            "FROM `order` o JOIN goods g ON o.goods_id = g.id " +
            "WHERE o.status = 'completed' AND o.pay_time IS NOT NULL " +
            "AND g.category_id = 1 " +
            "AND o.pay_time >= DATE_SUB(DATE_SUB(NOW(), INTERVAL 30 DAY), INTERVAL 30 DAY) " +
            "AND o.pay_time < DATE_SUB(NOW(), INTERVAL 30 DAY)")
    Map<String, Object> textbookPrevStats();

    // ================== 校园闪送 SQL ==================

    /**
     * 闪送配送点：按交易地点聚合在售商品（只保留地点有效且件数 >= 2 的点）。
     * 每个地点就是一个"校园闪送仓"。
     */
    @Select("SELECT location, COUNT(*) AS count, ROUND(AVG(price), 0) AS avgPrice, " +
            "MIN(price) AS minPrice, MAX(create_time) AS latestTime " +
            "FROM goods " +
            "WHERE is_deleted = 0 AND status = 'on_sale' " +
            "AND location IS NOT NULL AND location <> '' " +
            "GROUP BY location HAVING COUNT(*) >= 2 " +
            "ORDER BY count DESC LIMIT #{limit}")
    List<Map<String, Object>> flashPoints(@Param("limit") int limit);

    /**
     * 闪送点商品：按地点筛选在售商品，可按分类过滤。
     */
    @Select("<script>" +
            "SELECT g.id, g.title, g.price, g.cover_image AS coverImage, g.location, " +
            "g.view_count AS viewCount, c.name AS categoryName " +
            "FROM goods g LEFT JOIN category c ON g.category_id = c.id " +
            "WHERE g.is_deleted = 0 AND g.status = 'on_sale' " +
            "<if test='location != null and location != \"\"'> AND g.location = #{location} </if>" +
            "<if test='categoryId != null'> AND g.category_id = #{categoryId} </if>" +
            "ORDER BY g.view_count DESC, g.create_time DESC LIMIT #{limit}" +
            "</script>")
    List<Map<String, Object>> flashGoods(@Param("location") String location,
                                         @Param("categoryId") Integer categoryId,
                                         @Param("limit") int limit);

    /**
     * 闪送分类分布：在售商品按分类统计，用于首页快捷分类入口。
     */
    @Select("SELECT c.id AS categoryId, c.name AS categoryName, COUNT(*) AS count, " +
            "MIN(g.price) AS minPrice " +
            "FROM goods g JOIN category c ON g.category_id = c.id " +
            "WHERE g.is_deleted = 0 AND g.status = 'on_sale' " +
            "AND g.location IS NOT NULL AND g.location <> '' " +
            "GROUP BY c.id, c.name ORDER BY count DESC")
    List<Map<String, Object>> flashCategories();

    /**
     * 今日已送达：当天完成的订单数（闪送履约量的真实来源）。
     */
    @Select("SELECT COUNT(*) AS c FROM `order` " +
            "WHERE status = 'completed' AND pay_time IS NOT NULL AND DATE(pay_time) = CURDATE()")
    int flashDeliveredToday();

    /**
     * 此刻最抢手：近 3 天被浏览最多的在售商品（真实热度，不是随机推荐）。
     */
    @Select("SELECT g.id, g.title, g.price, g.cover_image AS coverImage, g.location, " +
            "COALESCE(SUM(bh.browse_count), 0) AS hotCount, c.name AS categoryName " +
            "FROM goods g " +
            "JOIN browse_history bh ON bh.goods_id = g.id " +
            "LEFT JOIN category c ON g.category_id = c.id " +
            "WHERE g.is_deleted = 0 AND g.status = 'on_sale' " +
            "AND bh.create_time >= DATE_SUB(NOW(), INTERVAL 3 DAY) " +
            "GROUP BY g.id, g.title, g.price, g.cover_image, g.location, c.name " +
            "ORDER BY hotCount DESC LIMIT #{limit}")
    List<Map<String, Object>> flashHotNow(@Param("limit") int limit);
}