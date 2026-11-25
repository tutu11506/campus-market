package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.UserCourse;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserCourseMapper {

    @Select("SELECT * FROM user_course WHERE user_id = #{userId} AND semester = #{semester} ORDER BY create_time DESC")
    List<UserCourse> findBySemester(@Param("userId") Integer userId, @Param("semester") String semester);

    @Select("SELECT DISTINCT semester FROM user_course WHERE user_id = #{userId} ORDER BY semester DESC")
    List<String> findSemesters(Integer userId);

    /** 用户所有课表（去重 course_name） */
    @Select("SELECT course_name FROM user_course WHERE user_id = #{userId} AND semester = #{semester} GROUP BY course_name")
    List<String> distinctCourseNames(@Param("userId") Integer userId, @Param("semester") String semester);

    @Select("SELECT COUNT(*) FROM user_course WHERE user_id = #{userId} AND course_name = #{courseName} AND semester = #{semester}")
    int countByUnique(@Param("userId") Integer userId,
                      @Param("courseName") String courseName,
                      @Param("semester") String semester);

    /** 单条插入：Service 层调用 countByUnique 先查重再插入，避免循环内大量 duplicate 异常 */
    @Insert("INSERT INTO user_course(user_id, course_name, teacher, semester, course_code, remark) " +
            "VALUES(#{userId}, #{courseName}, #{teacher}, #{semester}, #{courseCode}, #{remark})")
    int insert(UserCourse course);

    @Select("SELECT u.* FROM user_course u " +
            "WHERE u.user_id = #{userId} AND u.semester = #{semester} " +
            "AND u.course_name LIKE CONCAT('%', #{keyword}, '%')")
    List<UserCourse> findByKeyword(@Param("userId") Integer userId,
                                   @Param("semester") String semester,
                                   @Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM user_course WHERE user_id = #{userId} AND semester = #{semester}")
    int countSemester(@Param("userId") Integer userId, @Param("semester") String semester);

    @Select("DELETE FROM user_course WHERE user_id = #{userId} AND semester = #{semester}")
    int clearSemester(@Param("userId") Integer userId, @Param("semester") String semester);
}
