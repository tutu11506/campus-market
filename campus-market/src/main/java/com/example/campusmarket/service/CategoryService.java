package com.example.campusmarket.service;

import com.example.campusmarket.common.TtlCache;
import com.example.campusmarket.entity.Category;
import com.example.campusmarket.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    /** 分类几乎不变，缓存 5 分钟；增删改后立即清空 */
    private static final String ALL_KEY = "all";

    private final TtlCache<List<Category>> cache = new TtlCache<>(5 * 60 * 1000L, 8);

    @Autowired
    private CategoryMapper categoryMapper;

    // 查询所有分类
    public List<Category> findAll() {
        List<Category> cached = cache.get(ALL_KEY);
        if (cached != null) {
            return cached;
        }
        List<Category> list = categoryMapper.findAll();
        cache.put(ALL_KEY, list);
        return list;
    }

    // 根据ID查询
    public Category findById(Integer id) {
        return categoryMapper.findById(id);
    }

    // 新增
    public String add(Category category) {
        categoryMapper.insert(category);
        cache.clear();
        return "添加成功";
    }

    // 修改
    public String update(Category category) {
        categoryMapper.update(category);
        cache.clear();
        return "修改成功";
    }

    // 删除
    public String delete(Integer id) {
        categoryMapper.delete(id);
        cache.clear();
        return "删除成功";
    }
}
