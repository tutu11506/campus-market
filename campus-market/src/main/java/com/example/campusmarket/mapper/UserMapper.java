package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.User;
import com.example.campusmarket.vo.UserStatsVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;


import java.util.List;
import java.util.Map;

public interface UserMapper {

    // 根据用户名查询
    @Select("SELECT * FROM user WHERE username = #{username} ")
    User findByUsername(String username);

    // 根据学号查询（注册时校验唯一性）
    @Select("SELECT * FROM user WHERE student_no = #{studentNo}")
    User findByStudentNo(String studentNo);

    // 用户总数（管理员统计用，避免加载全表）
    @Select("SELECT COUNT(*) FROM user")
    int countAll();

    // 根据ID查询
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(Integer id);

    // 新增用户（注册）
    @Insert("INSERT INTO user(student_no, username, password, phone) " +
            "VALUES(#{studentNo}, #{username}, #{password}, #{phone})")
    int insert(User user);

    // 修改个人信息（只更新传入的字段，避免用 null 覆盖原值）
    @Update("<script>UPDATE user <set>" +
            "<if test='username != null'>username = #{username},</if>" +
            "<if test='phone != null'>phone = #{phone},</if>" +
            "<if test='avatar != null'>avatar = #{avatar},</if>" +
            "</set> WHERE id = #{id}</script>")
    int update(User user);

    // 查询所有用户（管理员用）
    @Select("SELECT * FROM user ORDER BY id DESC")
    List<User> findAll();

    // 分页查询所有用户（管理员用）
    @Select("SELECT * FROM user ORDER BY id DESC LIMIT #{offset}, #{size}")
    List<User> findAllPage(@Param("offset") Integer offset, @Param("size") Integer size);

    // 按昵称或学号模糊搜索用户（不含封禁账号），结果先按昵称、再按学号排序，分页
    @Select("SELECT * FROM user WHERE (username LIKE CONCAT('%', #{kw}, '%') OR student_no LIKE CONCAT('%', #{kw}, '%')) " +
            "AND status <> 'ban' ORDER BY username ASC, student_no ASC LIMIT #{offset}, #{size}")
    List<User> searchByKeyword(@Param("kw") String kw,
                               @Param("offset") Integer offset, @Param("size") Integer size);

    // 搜索用户总数（用于分页 total）
    @Select("SELECT COUNT(*) FROM user WHERE (username LIKE CONCAT('%', #{kw}, '%') OR student_no LIKE CONCAT('%', #{kw}, '%')) " +
            "AND status <> 'ban'")
    int countSearch(@Param("kw") String kw);

    // 修改用户状态（封禁/解封）
    @Update("UPDATE user SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") String status);

    // 更新密码（加密后的）
    @Update("UPDATE user SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Integer id, @Param("password") String password);

    // 近 7 天每日注册人数（管理后台看板）
    @Select("SELECT DATE_FORMAT(create_time, '%m-%d') AS date, COUNT(*) AS count " +
            "FROM user WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "GROUP BY DATE_FORMAT(create_time, '%m-%d') ORDER BY DATE_FORMAT(create_time, '%m-%d')")
    List<Map<String, Object>> countRegistrationsByDay();

    // 个人中心数据概览：发布 / 在售 / 已售 / 收藏 / 收到的评价
    @Select("SELECT " +
            "(SELECT COUNT(*) FROM goods WHERE seller_id = #{userId} AND is_deleted = 0) AS published, " +
            "(SELECT COUNT(*) FROM goods WHERE seller_id = #{userId} AND is_deleted = 0 AND status = 'on_sale') AS onSale, " +
            "(SELECT COUNT(*) FROM goods WHERE seller_id = #{userId} AND is_deleted = 0 AND status = 'sold') AS sold, " +
            "(SELECT COUNT(*) FROM favorite f JOIN goods g ON f.goods_id = g.id " +
            " WHERE f.user_id = #{userId} AND g.is_deleted = 0) AS favorites, " +
            "(SELECT COUNT(*) FROM comment WHERE to_user_id = #{userId}) AS reviews")
    UserStatsVO findStats(@Param("userId") Integer userId);
}