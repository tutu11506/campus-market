package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.Category;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface CategoryMapper {

    // 查询所有分类（按排序）
    @Select("SELECT * FROM category ORDER BY sort ASC")
    List<Category> findAll();

    // 根据ID查询
    @Select("SELECT * FROM category WHERE id = #{id}")
    Category findById(Integer id);

    // 新增分类
    @Insert("INSERT INTO category(name, icon, sort) VALUES(#{name}, #{icon}, #{sort})")
    int insert(Category category);

    // 修改分类
    @Update("UPDATE category SET name = #{name}, icon = #{icon}, sort = #{sort} WHERE id = #{id}")
    int update(Category category);

    // 删除分类
    @Delete("DELETE FROM category WHERE id = #{id}")
    int delete(Integer id);
}