package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.DonationOrganization;
import com.example.campusmarket.entity.DonationRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface DonationMapper {

    String RECORD_COLUMNS = "SELECT r.*, u.username AS donor_name, u.student_no AS donor_student_no, " +
            "o.name AS organization_name, a.username AS receiver_name " +
            "FROM donation_record r " +
            "LEFT JOIN user u ON r.user_id = u.id " +
            "LEFT JOIN donation_org o ON r.org_id = o.id " +
            "LEFT JOIN user a ON r.received_by = a.id ";

    @Select("SELECT * FROM donation_org WHERE is_active = 1 ORDER BY id")
    List<DonationOrganization> findActiveOrganizations();

    @Select("SELECT * FROM donation_org WHERE id = #{id} AND is_active = 1")
    DonationOrganization findActiveOrganizationById(Integer id);

    @Insert("INSERT INTO donation_record(user_id, org_id, item_type, item_name, quantity, condition_level, " +
            "handover_method, handover_detail, contact, remark) " +
            "VALUES(#{userId}, #{orgId}, #{itemType}, #{itemName}, #{quantity}, #{conditionLevel}, " +
            "#{handoverMethod}, #{handoverDetail}, #{contact}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRecord(DonationRecord record);

    @Select(RECORD_COLUMNS + "WHERE r.id = #{id}")
    DonationRecord findRecordById(Integer id);

    @Select(RECORD_COLUMNS + "WHERE r.user_id = #{userId} ORDER BY r.create_time DESC LIMIT #{offset}, #{pageSize}")
    List<DonationRecord> findMyRecords(@Param("userId") Integer userId,
                                       @Param("offset") int offset,
                                       @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM donation_record WHERE user_id = #{userId}")
    long countMyRecords(Integer userId);

    @Update("UPDATE donation_record SET status = 'cancelled' " +
            "WHERE id = #{id} AND user_id = #{userId} AND status = 'pending'")
    int cancelRecord(@Param("id") Integer id, @Param("userId") Integer userId);

    @Select("<script>" + RECORD_COLUMNS + "WHERE 1 = 1 " +
            "<if test='status != null and status != \"\"'> AND r.status = #{status} </if> " +
            "<if test='itemType != null and itemType != \"\"'> AND r.item_type = #{itemType} </if> " +
            "ORDER BY CASE r.status WHEN 'pending' THEN 0 WHEN 'received' THEN 1 ELSE 2 END, r.create_time DESC " +
            "LIMIT #{offset}, #{pageSize}</script>")
    List<DonationRecord> findAdminRecords(@Param("status") String status,
                                          @Param("itemType") String itemType,
                                          @Param("offset") int offset,
                                          @Param("pageSize") int pageSize);

    @Select("<script>SELECT COUNT(*) FROM donation_record r WHERE 1 = 1 " +
            "<if test='status != null and status != \"\"'> AND r.status = #{status} </if> " +
            "<if test='itemType != null and itemType != \"\"'> AND r.item_type = #{itemType} </if></script>")
    long countAdminRecords(@Param("status") String status, @Param("itemType") String itemType);

    @Update("UPDATE donation_record SET status = 'received', received_quantity = #{receivedQuantity}, " +
            "received_by = #{adminId}, received_time = NOW() " +
            "WHERE id = #{id} AND status = 'pending' AND #{receivedQuantity} <= quantity")
    int receiveRecord(@Param("id") Integer id,
                      @Param("receivedQuantity") Integer receivedQuantity,
                      @Param("adminId") Integer adminId);

    @Select("SELECT item_type AS itemType, COUNT(*) AS recordCount, " +
            "COALESCE(SUM(quantity), 0) AS registeredQuantity, " +
            "COALESCE(SUM(CASE WHEN status = 'received' THEN received_quantity ELSE 0 END), 0) AS receivedQuantity " +
            "FROM donation_record WHERE status IN ('pending', 'received') GROUP BY item_type")
    List<Map<String, Object>> summarizeByType();

    @Select("SELECT COUNT(DISTINCT user_id) FROM donation_record WHERE status IN ('pending', 'received')")
    long countDonors();

    @Select("SELECT COUNT(*) FROM donation_record WHERE status IN ('pending', 'received')")
    long countValidRecords();

    @Select("SELECT o.id AS orgId, o.name AS organizationName, " +
            "COALESCE(SUM(CASE WHEN r.item_type = 'clothes' THEN r.quantity ELSE 0 END), 0) AS registeredClothes, " +
            "COALESCE(SUM(CASE WHEN r.item_type = 'books' THEN r.quantity ELSE 0 END), 0) AS registeredBooks, " +
            "COALESCE(SUM(CASE WHEN r.status = 'received' AND r.item_type = 'clothes' THEN r.received_quantity ELSE 0 END), 0) AS receivedClothes, " +
            "COALESCE(SUM(CASE WHEN r.status = 'received' AND r.item_type = 'books' THEN r.received_quantity ELSE 0 END), 0) AS receivedBooks " +
            "FROM donation_org o LEFT JOIN donation_record r ON o.id = r.org_id AND r.status IN ('pending', 'received') " +
            "WHERE o.is_active = 1 GROUP BY o.id, o.name ORDER BY o.id")
    List<Map<String, Object>> summarizeByOrganization();
}
