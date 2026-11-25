package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.Textbook;
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
public interface TextbookMapper {

    String COLUMNS = "SELECT t.*, " +
            "(SELECT COUNT(*) FROM goods g WHERE g.textbook_id = t.id AND g.status = 'on_sale' AND g.is_deleted = 0) AS goods_count, " +
            "(SELECT COUNT(*) FROM rental_item r WHERE r.textbook_id = t.id AND r.status = 'available') AS rental_count, " +
            "(SELECT ROUND(MIN(g.price), 0) FROM goods g WHERE g.textbook_id = t.id AND g.status = 'on_sale' AND g.is_deleted = 0) AS goods_min_price, " +
            "(SELECT ROUND(MIN(r.price), 0) FROM rental_item r WHERE r.textbook_id = t.id AND r.status = 'available') AS rental_min_price " +
            "FROM textbook t ";

    @Select("<script>" + COLUMNS +
            "WHERE t.status = 1 " +
            "<if test='subject != null and subject != \"\"'> AND t.subject = #{subject} </if>" +
            "<if test='teacherFlag != null'> AND t.teacher_flag = #{teacherFlag} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (t.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR t.isbn LIKE CONCAT('%', #{keyword}, '%') OR t.author LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "ORDER BY (t.teacher_flag = 1) DESC, t.warn_count ASC, t.create_time DESC " +
            "LIMIT #{offset}, #{pageSize}</script>")
    List<Textbook> findList(@Param("subject") String subject,
                            @Param("teacherFlag") Integer teacherFlag,
                            @Param("keyword") String keyword,
                            @Param("offset") int offset,
                            @Param("pageSize") int pageSize);

    @Select("<script>SELECT COUNT(*) FROM textbook t WHERE t.status = 1 " +
            "<if test='subject != null and subject != \"\"'> AND t.subject = #{subject} </if>" +
            "<if test='teacherFlag != null'> AND t.teacher_flag = #{teacherFlag} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (t.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR t.isbn LIKE CONCAT('%', #{keyword}, '%') OR t.author LIKE CONCAT('%', #{keyword}, '%')) </if></script>")
    long countList(@Param("subject") String subject,
                   @Param("teacherFlag") Integer teacherFlag,
                   @Param("keyword") String keyword);

    @Select(COLUMNS + "WHERE t.id = #{id}")
    Textbook findById(Integer id);

    @Select("SELECT * FROM textbook WHERE isbn = #{isbn} AND status = 1 LIMIT 1")
    Textbook findByIsbn(String isbn);

    @Select("SELECT * FROM textbook WHERE id IN <foreach item='id' collection='ids' open='(' close=')' separator=','>#{id}</foreach> AND status = 1")
    List<Textbook> findByIds(@Param("ids") List<Integer> ids);

    /** 按课程关键词（模糊匹配 subject 或 title），用于课表匹配 */
    @Select("SELECT * FROM textbook WHERE status = 1 AND subject LIKE CONCAT('%', #{keyword}, '%') LIMIT 10")
    List<Textbook> findBySubjectKeyword(String keyword);

    @Select("SELECT * FROM textbook WHERE status = 1 AND title LIKE CONCAT('%', #{keyword}, '%') LIMIT 10")
    List<Textbook> findByTitleKeyword(String keyword);

    @Insert("INSERT INTO textbook(isbn, title, subtitle, author, publisher, edition, subject, grade, " +
            "cover_image, teacher_flag, teacher_note) " +
            "VALUES(#{isbn}, #{title}, #{subtitle}, #{author}, #{publisher}, #{edition}, #{subject}, #{grade}, " +
            "#{coverImage}, #{teacherFlag}, #{teacherNote})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Textbook textbook);

    @Update("UPDATE textbook SET title = #{title}, subtitle = #{subtitle}, author = #{author}, " +
            "publisher = #{publisher}, edition = #{edition}, subject = #{subject}, grade = #{grade}, " +
            "cover_image = #{coverImage}, teacher_flag = #{teacherFlag}, teacher_note = #{teacherNote} " +
            "WHERE id = #{id} AND status = 1")
    int update(Textbook textbook);

    @Update("UPDATE textbook SET teacher_flag = #{flag}, teacher_note = #{note} WHERE id = #{id}")
    int updateTeacherMark(@Param("id") Integer id,
                          @Param("flag") int flag,
                          @Param("note") String note);

    /** 避雷留言数 +1 */
    @Update("UPDATE textbook SET warn_count = warn_count + 1 WHERE id = #{id}")
    int incrWarnCount(Integer id);

    /** 全部学科列表（用于前端分类筛选） */
    @Select("SELECT subject AS label, COUNT(*) AS count FROM textbook WHERE status = 1 AND subject IS NOT NULL AND subject != '' GROUP BY subject ORDER BY count DESC")
    List<Map<String, Object>> subjects();
}
