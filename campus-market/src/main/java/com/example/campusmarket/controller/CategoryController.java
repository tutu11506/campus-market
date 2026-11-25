package com.example.campusmarket.controller;

import com.example.campusmarket.common.Result;
import com.example.campusmarket.entity.Category;
import com.example.campusmarket.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    // 查询所有分类
    @GetMapping("/list")
    public Result<List<Category>> findAll() {
        return Result.success(categoryService.findAll());
    }

    // 根据ID查询
    @GetMapping("/{id}")
    public Result<Category> findById(@PathVariable Integer id) {
        return Result.success(categoryService.findById(id));
    }

    // 新增分类
    @PostMapping("/add")
    public Result<String> add(@RequestBody Category category) {
        return Result.success(categoryService.add(category));
    }

    // 修改分类
    @PutMapping("/update")
    public Result<String> update(@RequestBody Category category) {
        return Result.success(categoryService.update(category));
    }

    // 删除分类
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        return Result.success(categoryService.delete(id));
    }
}