package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.DemandPost;
import com.example.campusmarket.entity.DemandQuote;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface DemandMapper {

    String POST_COLUMNS = "SELECT p.*, u.username AS authorName, u.avatar AS authorAvatar " +
            "FROM demand_post p LEFT JOIN user u ON p.user_id = u.id ";

    @Select("<script>" + POST_COLUMNS + "WHERE p.is_deleted = 0 " +
            "<if test='category != null and category != \"\"'> AND p.category = #{category} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (p.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.description LIKE CONCAT('%', #{keyword}, '%') OR p.keyword LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "<if test='mine != null'> AND p.user_id = #{mine} </if>" +
            "ORDER BY CASE p.status WHEN 'open' THEN 0 ELSE 1 END, p.create_time DESC " +
            "LIMIT #{offset}, #{pageSize}</script>")
    List<DemandPost> findPosts(@Param("category") String category,
                               @Param("keyword") String keyword,
                               @Param("mine") Integer mine,
                               @Param("offset") int offset,
                               @Param("pageSize") int pageSize);

    @Select("<script>SELECT COUNT(*) FROM demand_post p WHERE p.is_deleted = 0 " +
            "<if test='category != null and category != \"\"'> AND p.category = #{category} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (p.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR p.description LIKE CONCAT('%', #{keyword}, '%') OR p.keyword LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "<if test='mine != null'> AND p.user_id = #{mine} </if></script>")
    int countPosts(@Param("category") String category,
                   @Param("keyword") String keyword,
                   @Param("mine") Integer mine);

    @Select(POST_COLUMNS + "WHERE p.id = #{id} AND p.is_deleted = 0")
    DemandPost findById(@Param("id") Integer id);

    @Insert("INSERT INTO demand_post(user_id, title, category, keyword, description, budget_min, budget_max, " +
            "target_major, target_grade) VALUES(#{userId}, #{title}, #{category}, #{keyword}, #{description}, " +
            "#{budgetMin}, #{budgetMax}, #{targetMajor}, #{targetGrade})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPost(DemandPost post);

    @Update("UPDATE demand_post SET status = #{status}, deal_price = #{dealPrice} WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id,
                     @Param("status") String status,
                     @Param("dealPrice") BigDecimal dealPrice);

    @Update("UPDATE demand_post SET quote_count = quote_count + 1 WHERE id = #{id}")
    int addQuoteCount(@Param("id") Integer id);

    @Insert("INSERT INTO demand_quote(demand_id, seller_id, price, message) " +
            "VALUES(#{demandId}, #{sellerId}, #{price}, #{message})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertQuote(DemandQuote quote);

    @Select("SELECT q.*, u.username AS sellerName, u.avatar AS sellerAvatar FROM demand_quote q " +
            "LEFT JOIN user u ON q.seller_id = u.id WHERE q.demand_id = #{demandId} " +
            "ORDER BY CASE q.status WHEN 'accepted' THEN 0 WHEN 'pending' THEN 1 ELSE 2 END, q.create_time ASC")
    List<DemandQuote> findQuotes(@Param("demandId") Integer demandId);

    @Select("SELECT * FROM demand_quote WHERE demand_id = #{demandId} AND seller_id = #{sellerId}")
    DemandQuote findMyQuote(@Param("demandId") Integer demandId, @Param("sellerId") Integer sellerId);

    @Select("SELECT * FROM demand_quote WHERE id = #{id}")
    DemandQuote findQuoteById(@Param("id") Integer id);

    @Update("UPDATE demand_quote SET status = #{status} WHERE id = #{id}")
    int updateQuoteStatus(@Param("id") Integer id, @Param("status") String status);

    @Update("UPDATE demand_quote SET status = 'rejected' " +
            "WHERE demand_id = #{demandId} AND id <> #{exceptId} AND status = 'pending'")
    int rejectOthers(@Param("demandId") Integer demandId, @Param("exceptId") Integer exceptId);

    /** 系统自动匹配：按关键词找在售商品 */
    @Select("SELECT g.id, g.title, g.price, g.cover_image AS coverImage, g.description, g.location, " +
            "g.view_count AS viewCount FROM goods g WHERE g.is_deleted = 0 AND g.status = 'on_sale' " +
            "AND (g.title LIKE CONCAT('%', #{keyword}, '%') OR g.description LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY g.create_time DESC LIMIT #{limit}")
    List<Map<String, Object>> matchGoods(@Param("keyword") String keyword, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM goods g WHERE g.is_deleted = 0 AND g.status = 'on_sale' " +
            "AND (g.title LIKE CONCAT('%', #{keyword}, '%') OR g.description LIKE CONCAT('%', #{keyword}, '%'))")
    int countMatchGoods(@Param("keyword") String keyword);

    /**
     * 批量统计多个关键词各自匹配的在售商品数：整页只扫一遍 goods 表，
     * 每个关键词对应一列 c0、c1……，替代逐条 countMatchGoods 的 N+1。
     */
    @Select("<script>SELECT " +
            "<foreach collection='keywords' item='kw' index='i' separator=', '>" +
            "SUM(CASE WHEN g.title LIKE CONCAT('%', #{kw}, '%') " +
            "OR g.description LIKE CONCAT('%', #{kw}, '%') THEN 1 ELSE 0 END) AS c${i}" +
            "</foreach> " +
            "FROM goods g WHERE g.is_deleted = 0 AND g.status = 'on_sale'</script>")
    Map<String, Object> countMatchGoodsBatch(@Param("keywords") List<String> keywords);
}
