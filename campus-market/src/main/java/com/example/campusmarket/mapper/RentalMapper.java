package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.RentalItem;
import com.example.campusmarket.entity.RentalOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RentalMapper {

    String ITEM_COLUMNS = "SELECT i.*, u.username AS owner_name, u.student_no AS owner_student_no " +
            "FROM rental_item i JOIN user u ON i.owner_id = u.id ";

    String ORDER_COLUMNS = "SELECT o.*, i.title AS item_title, i.cover_image AS item_cover, " +
            "i.category AS category, i.price_unit AS price_unit, " +
            "ur.username AS renter_name, uo.username AS owner_name, " +
            "DATEDIFF(o.end_date, o.start_date) + 1 AS days, " +
            "CASE WHEN o.status IN ('renting', 'returning') AND DATEDIFF(CURDATE(), o.end_date) > 0 " +
            "     THEN DATEDIFF(CURDATE(), o.end_date) ELSE 0 END AS overdue_days, " +
            "CASE WHEN o.status IN ('renting', 'returning') " +
            "     THEN GREATEST(DATEDIFF(o.end_date, CURDATE()), 0) ELSE 0 END AS remaining_days " +
            "FROM rental_order o " +
            "JOIN rental_item i ON o.item_id = i.id " +
            "JOIN user ur ON o.renter_id = ur.id " +
            "JOIN user uo ON o.owner_id = uo.id ";

    /* ==================== 出租物品 ==================== */

    @Select("<script>" + ITEM_COLUMNS +
            "WHERE i.status IN ('available', 'rented') " +
            "<if test='type != null and type != \"\" and type == \"short\"'> AND i.rental_type IN ('daily', 'weekly') </if>" +
            "<if test='type != null and type != \"\" and type != \"short\"'> AND i.rental_type = #{type} </if>" +
            "<if test='category != null and category != \"\"'> AND i.category = #{category} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (i.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR i.description LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "ORDER BY (i.status = 'available') DESC, i.create_time DESC " +
            "LIMIT #{offset}, #{pageSize}</script>")
    List<RentalItem> findItems(@Param("type") String type,
                               @Param("category") String category,
                               @Param("keyword") String keyword,
                               @Param("offset") int offset,
                               @Param("pageSize") int pageSize);

    @Select("<script>SELECT COUNT(*) FROM rental_item i " +
            "WHERE i.status IN ('available', 'rented') " +
            "<if test='type != null and type != \"\" and type == \"short\"'> AND i.rental_type IN ('daily', 'weekly') </if>" +
            "<if test='type != null and type != \"\" and type != \"short\"'> AND i.rental_type = #{type} </if>" +
            "<if test='category != null and category != \"\"'> AND i.category = #{category} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (i.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR i.description LIKE CONCAT('%', #{keyword}, '%')) </if></script>")
    long countItems(@Param("type") String type,
                    @Param("category") String category,
                    @Param("keyword") String keyword);

    @Select(ITEM_COLUMNS + "WHERE i.id = #{id}")
    RentalItem findItemById(Integer id);

    @Insert("INSERT INTO rental_item(title, description, rental_type, category, textbook_id, price, price_unit, deposit, " +
            "cover_image, location, owner_id, auto_offline_date) " +
            "VALUES(#{title}, #{description}, #{rentalType}, #{category}, #{textbookId}, #{price}, #{priceUnit}, #{deposit}, " +
            "#{coverImage}, #{location}, #{ownerId}, #{autoOfflineDate})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertItem(RentalItem item);

    /** 原子锁定：只有可租状态才能被下单，避免并发重复出租 */
    @Update("UPDATE rental_item SET status = 'rented' WHERE id = #{id} AND status = 'available'")
    int lockItem(Integer id);

    /** 归还后释放：若已过自动下架日期（假期寄存）则直接下架 */
    @Update("UPDATE rental_item SET status = CASE " +
            "WHEN auto_offline_date IS NOT NULL AND auto_offline_date <= CURDATE() THEN 'offline' " +
            "ELSE 'available' END " +
            "WHERE id = #{id} AND status = 'rented'")
    int releaseItem(Integer id);

    /** 假期寄存：到达开学/下架日期后自动下架空闲物品 */
    @Update("UPDATE rental_item SET status = 'offline' " +
            "WHERE rental_type = 'storage' AND status = 'available' " +
            "AND auto_offline_date IS NOT NULL AND auto_offline_date <= CURDATE()")
    int offlineExpiredStorage();

    /* ==================== 租赁订单 ==================== */

    @Insert("INSERT INTO rental_order(order_no, item_id, renter_id, owner_id, rental_type, start_date, end_date, " +
            "rent_amount, deposit, status, remark, handover_note) " +
            "VALUES(#{orderNo}, #{itemId}, #{renterId}, #{ownerId}, #{rentalType}, #{startDate}, #{endDate}, " +
            "#{rentAmount}, #{deposit}, #{status}, #{remark}, #{handoverNote})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOrder(RentalOrder order);

    @Select(ORDER_COLUMNS + "WHERE o.id = #{id}")
    RentalOrder findOrderById(Integer id);

    @Select("<script>" + ORDER_COLUMNS + "WHERE o.renter_id = #{userId} " +
            "<if test='status != null and status != \"\"'> AND o.status = #{status} </if>" +
            "ORDER BY o.create_time DESC LIMIT #{offset}, #{pageSize}</script>")
    List<RentalOrder> findRenterOrders(@Param("userId") Integer userId,
                                       @Param("status") String status,
                                       @Param("offset") int offset,
                                       @Param("pageSize") int pageSize);

    @Select("<script>SELECT COUNT(*) FROM rental_order o WHERE o.renter_id = #{userId} " +
            "<if test='status != null and status != \"\"'> AND o.status = #{status} </if></script>")
    long countRenterOrders(@Param("userId") Integer userId, @Param("status") String status);

    @Select("<script>" + ORDER_COLUMNS + "WHERE o.owner_id = #{userId} " +
            "<if test='status != null and status != \"\"'> AND o.status = #{status} </if>" +
            "ORDER BY o.create_time DESC LIMIT #{offset}, #{pageSize}</script>")
    List<RentalOrder> findOwnerOrders(@Param("userId") Integer userId,
                                      @Param("status") String status,
                                      @Param("offset") int offset,
                                      @Param("pageSize") int pageSize);

    @Select("<script>SELECT COUNT(*) FROM rental_order o WHERE o.owner_id = #{userId} " +
            "<if test='status != null and status != \"\"'> AND o.status = #{status} </if></script>")
    long countOwnerOrders(@Param("userId") Integer userId, @Param("status") String status);

    @Update("UPDATE rental_order SET status = 'renting', confirm_time = NOW() " +
            "WHERE id = #{id} AND owner_id = #{uid} AND status = 'pending'")
    int confirmOrder(@Param("id") Integer id, @Param("uid") Integer uid);

    @Update("UPDATE rental_order SET status = 'returning', return_apply_time = NOW() " +
            "WHERE id = #{id} AND renter_id = #{uid} AND status = 'renting'")
    int applyReturn(@Param("id") Integer id, @Param("uid") Integer uid);

    @Update("UPDATE rental_order SET status = 'completed', return_time = NOW() " +
            "WHERE id = #{id} AND owner_id = #{uid} AND status = 'returning'")
    int completeOrder(@Param("id") Integer id, @Param("uid") Integer uid);

    @Update("UPDATE rental_order SET status = 'cancelled' " +
            "WHERE id = #{id} AND status = 'pending' AND (renter_id = #{uid} OR owner_id = #{uid})")
    int cancelOrder(@Param("id") Integer id, @Param("uid") Integer uid);

    /* ==================== 管理端 ==================== */

    @Select("<script>" + ORDER_COLUMNS + "WHERE 1 = 1 " +
            "<if test='status != null and status != \"\"'> AND o.status = #{status} </if>" +
            "<if test='type != null and type != \"\" and type == \"short\"'> AND o.rental_type IN ('daily', 'weekly') </if>" +
            "<if test='type != null and type != \"\" and type != \"short\"'> AND o.rental_type = #{type} </if>" +
            "<if test='overdue'> AND o.status IN ('renting', 'returning') AND o.end_date &lt; CURDATE() </if>" +
            "ORDER BY CASE WHEN o.status IN ('renting', 'returning') AND o.end_date &lt; CURDATE() THEN 0 " +
            "WHEN o.status = 'pending' THEN 1 WHEN o.status = 'returning' THEN 2 ELSE 3 END, o.create_time DESC " +
            "LIMIT #{offset}, #{pageSize}</script>")
    List<RentalOrder> findAdminOrders(@Param("status") String status,
                                      @Param("type") String type,
                                      @Param("overdue") boolean overdue,
                                      @Param("offset") int offset,
                                      @Param("pageSize") int pageSize);

    @Select("<script>SELECT COUNT(*) FROM rental_order o WHERE 1 = 1 " +
            "<if test='status != null and status != \"\"'> AND o.status = #{status} </if>" +
            "<if test='type != null and type != \"\" and type == \"short\"'> AND o.rental_type IN ('daily', 'weekly') </if>" +
            "<if test='type != null and type != \"\" and type != \"short\"'> AND o.rental_type = #{type} </if>" +
            "<if test='overdue'> AND o.status IN ('renting', 'returning') AND o.end_date &lt; CURDATE() </if></script>")
    long countAdminOrders(@Param("status") String status,
                          @Param("type") String type,
                          @Param("overdue") boolean overdue);

    /* ==================== 到期/逾期提醒 ==================== */

    @Select(ORDER_COLUMNS + "WHERE o.status IN ('renting', 'returning') AND o.due_reminded = 0 " +
            "AND o.end_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 2 DAY) " +
            "ORDER BY o.end_date ASC LIMIT 100")
    List<RentalOrder> findDueSoonOrders();

    @Update("UPDATE rental_order SET due_reminded = 1 WHERE id = #{id}")
    int markDueReminded(Integer id);

    @Select(ORDER_COLUMNS + "WHERE o.status IN ('renting', 'returning') AND o.overdue_reminded = 0 " +
            "AND o.end_date < CURDATE() ORDER BY o.end_date ASC LIMIT 100")
    List<RentalOrder> findOverdueOrders();

    @Update("UPDATE rental_order SET overdue_reminded = 1 WHERE id = #{id}")
    int markOverdueReminded(Integer id);

    /** 站内提醒使用平台客服账号发送，账号不存在时跳过提醒 */
    @Select("SELECT id FROM user WHERE student_no = 'KEFU0001' LIMIT 1")
    Integer findCustomerServiceId();

    /* 供 Service 计算租期使用（预留，避免依赖客户端传值） */
    default long inclusiveDays(LocalDate start, LocalDate end) {
        return end.toEpochDay() - start.toEpochDay() + 1;
    }
}
