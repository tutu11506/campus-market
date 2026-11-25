package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.CampusTask;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

public interface CampusTaskMapper {

    // 发布任务
    @Insert("INSERT INTO campus_task(title, description, reward, category, location, deadline, publisher_id, status) " +
            "VALUES(#{title}, #{description}, #{reward}, #{category}, #{location}, #{deadline}, #{publisherId}, 'open')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CampusTask task);

    // 任务详情（含发布人信息）
    @Select("SELECT t.id, t.title, t.description, t.reward, t.category, t.location, t.deadline, " +
            "t.publisher_id AS publisherId, t.status, t.accepter_id AS accepterId, " +
            "t.accept_time AS acceptTime, t.complete_time AS completeTime, t.create_time AS createTime, " +
            "u.username AS publisherName, u.avatar AS publisherAvatar, " +
            "a.username AS accepterName, a.avatar AS accepterAvatar " +
            "FROM campus_task t " +
            "LEFT JOIN user u ON t.publisher_id = u.id " +
            "LEFT JOIN user a ON t.accepter_id = a.id " +
            "WHERE t.id = #{id} AND t.is_deleted = 0")
    Map<String, Object> findDetail(@Param("id") Integer id);

    // 任务大厅分页（待接单优先，再按发布时间倒序）
    @Select("<script>" +
            "SELECT t.id, t.title, t.description, t.reward, t.category, t.location, t.deadline, " +
            "t.publisher_id AS publisherId, t.status, t.accepter_id AS accepterId, " +
            "t.accept_time AS acceptTime, t.create_time AS createTime, " +
            "u.username AS publisherName, u.avatar AS publisherAvatar " +
            "FROM campus_task t LEFT JOIN user u ON t.publisher_id = u.id " +
            "WHERE t.is_deleted = 0 " +
            "<if test='status != null and status != \"\"'> AND t.status = #{status} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (t.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR t.description LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "ORDER BY CASE t.status WHEN 'open' THEN 0 WHEN 'accepted' THEN 1 ELSE 2 END, t.create_time DESC " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<Map<String, Object>> findPage(@Param("status") String status,
                                       @Param("keyword") String keyword,
                                       @Param("offset") int offset,
                                       @Param("pageSize") int pageSize);

    // 任务大厅总数
    @Select("<script>" +
            "SELECT COUNT(*) FROM campus_task t WHERE t.is_deleted = 0 " +
            "<if test='status != null and status != \"\"'> AND t.status = #{status} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (t.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR t.description LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "</script>")
    int countPage(@Param("status") String status, @Param("keyword") String keyword);

    // 我发布的任务
    @Select("SELECT t.id, t.title, t.description, t.reward, t.category, t.location, t.deadline, " +
            "t.publisher_id AS publisherId, t.status, t.accepter_id AS accepterId, " +
            "t.accept_time AS acceptTime, t.complete_time AS completeTime, t.create_time AS createTime, " +
            "u.username AS publisherName, a.username AS accepterName " +
            "FROM campus_task t " +
            "LEFT JOIN user u ON t.publisher_id = u.id " +
            "LEFT JOIN user a ON t.accepter_id = a.id " +
            "WHERE t.is_deleted = 0 AND t.publisher_id = #{userId} " +
            "ORDER BY t.create_time DESC LIMIT 50")
    List<Map<String, Object>> findByPublisher(@Param("userId") Integer userId);

    // 我接的任务
    @Select("SELECT t.id, t.title, t.description, t.reward, t.category, t.location, t.deadline, " +
            "t.publisher_id AS publisherId, t.status, t.accepter_id AS accepterId, " +
            "t.accept_time AS acceptTime, t.complete_time AS completeTime, t.create_time AS createTime, " +
            "u.username AS publisherName, u.avatar AS publisherAvatar " +
            "FROM campus_task t LEFT JOIN user u ON t.publisher_id = u.id " +
            "WHERE t.is_deleted = 0 AND t.accepter_id = #{userId} " +
            "ORDER BY t.accept_time DESC LIMIT 50")
    List<Map<String, Object>> findByAccepter(@Param("userId") Integer userId);

    // 任务基础状态（用于业务校验）
    @Select("SELECT publisher_id AS publisherId, accepter_id AS accepterId, status " +
            "FROM campus_task WHERE id = #{id} AND is_deleted = 0")
    Map<String, Object> findBasic(@Param("id") Integer id);

    // 接单：只有待接单且不是自己发布的才能接（并发下第二次更新影响行数为 0）
    @Update("UPDATE campus_task SET status = 'accepted', accepter_id = #{accepterId}, accept_time = NOW() " +
            "WHERE id = #{id} AND status = 'open' AND publisher_id <> #{accepterId} AND is_deleted = 0")
    int accept(@Param("id") Integer id, @Param("accepterId") Integer accepterId);

    // 发布人确认完成
    @Update("UPDATE campus_task SET status = 'completed', complete_time = NOW() " +
            "WHERE id = #{id} AND publisher_id = #{userId} AND status = 'accepted' AND is_deleted = 0")
    int finish(@Param("id") Integer id, @Param("userId") Integer userId);

    // 发布人取消（仅待接单状态）
    @Update("UPDATE campus_task SET status = 'cancelled' " +
            "WHERE id = #{id} AND publisher_id = #{userId} AND status = 'open' AND is_deleted = 0")
    int cancel(@Param("id") Integer id, @Param("userId") Integer userId);
}
