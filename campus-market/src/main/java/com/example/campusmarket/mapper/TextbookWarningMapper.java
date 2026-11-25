package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.TextbookWarning;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TextbookWarningMapper {

    @Select("SELECT w.*, u.username AS user_name, u.student_no AS user_student_no " +
            "FROM textbook_warning w JOIN user u ON w.user_id = u.id " +
            "WHERE w.textbook_id = #{textbookId} AND w.is_valid = 1 " +
            "ORDER BY w.create_time DESC LIMIT #{offset}, #{pageSize}")
    List<TextbookWarning> findByTextbook(@Param("textbookId") Integer textbookId,
                                         @Param("offset") int offset,
                                         @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM textbook_warning WHERE textbook_id = #{textbookId} AND is_valid = 1")
    long countByTextbook(Integer textbookId);

    @Select("SELECT * FROM textbook_warning WHERE id = #{id} AND is_valid = 1")
    TextbookWarning findById(Integer id);

    @Insert("INSERT INTO textbook_warning(textbook_id, user_id, reason, detail) " +
            "VALUES(#{textbookId}, #{userId}, #{reason}, #{detail})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TextbookWarning warning);
}
