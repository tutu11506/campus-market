package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.entity.Category;
import com.example.campusmarket.entity.Goods;
import com.example.campusmarket.service.CampusFeatureService;
import com.example.campusmarket.service.CategoryService;
import com.example.campusmarket.service.GoodsService;
import com.example.campusmarket.utils.GradeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页聚合接口：分类、轮播位、四大类封面、首屏商品一次返回，
 * 把首屏原本 6 个请求合并成 1 个。
 */
@RestController
@RequestMapping("/home")
public class HomeController {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CampusFeatureService campusFeatureService;

    @GetMapping("/init")
    public Result<Map<String, Object>> init(@RequestParam(defaultValue = "24") Integer pageSize) {
        int size = Math.min(Math.max(pageSize == null ? 24 : pageSize, 1), 50);

        List<Category> categories = categoryService.findAll();
        PageResult<Goods> goodsPage = goodsService.findList(null, null, null, null, null, "new", 1, size, null, null, null, null);
        List<Goods> banners = goodsService.findList(null, null, null, null, null, "views", 1, 12, null, null, null, null).getList();

        // 右侧四个大类封面：取该分类下浏览量最高商品的图
        Map<Integer, String> covers = new HashMap<>();
        int count = 0;
        for (Category category : categories) {
            if (count >= 4) {
                break;
            }
            count++;
            List<Goods> one = goodsService.findList(category.getId(), null, null, null, null, "views", 1, 1, null, null, null, null).getList();
            if (!one.isEmpty()) {
                Goods goods = one.get(0);
                String cover = goods.getCoverImage();
                if (cover == null || cover.isEmpty()) {
                    cover = goods.getImages();
                }
                if (cover != null && !cover.isEmpty()) {
                    covers.put(category.getId(), cover);
                }
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("categories", categories);
        data.put("banners", banners);
        data.put("covers", covers);
        data.put("goods", goodsPage);
        return Result.success(data);
    }

    /**
     * 校园专题接口 —— 返回当前学期阶段 + 对应专题商品。
     * 大平台不会关心学期时间窗口，这是校园专属差异化。
     *
     * 返回结构：
     * {
     *   phase: "graduation" | "summer_new" | "spring_new" | "normal",
     *   phaseDesc: "🎓 毕业清仓季" | "🎒 秋季开学季" | "📘 春季开学季" | "",
     *   goods: [ Goods, Goods, ... ],   // 专题商品（最多 6 件）
     *   highlight: String               // 给前端的一句话提示
     * }
     */
    @GetMapping("/campus-topic")
    public Result<Map<String, Object>> campusTopic() {
        String phase = GradeUtil.currentPhase();
        String phaseDesc = GradeUtil.phaseDescription(phase);

        Map<String, Object> result = new HashMap<>();
        result.put("phase", phase);
        result.put("phaseDesc", phaseDesc);

        // 不同阶段推不同分类的商品
        Integer focusCategoryId = null;
        String highlight = "";

        switch (phase) {
            case "summer_new":     // 秋季开学 → 教材 + 数码（新生报到季）
                focusCategoryId = 1;
                highlight = "新生报到季 · 学长学姐低价转教材 & 数码好物";
                break;
            case "spring_new":     // 春季开学 → 教材为主
                focusCategoryId = 1;
                highlight = "新学期教材特惠 · 大四学长清仓甩卖";
                break;
            case "graduation":     // 毕业季 → 生活用品 + 全品类毕业清仓
                focusCategoryId = null;
                highlight = "🎓 毕业清仓大甩卖 · 学长学姐带不走的好东西";
                break;
            default:
                focusCategoryId = null;
                highlight = "";
        }

        // 取专题商品（最多 6 件）
        PageResult<Goods> page = goodsService.findList(
                focusCategoryId, null, null, null, null, "new", 1, 6, null, null, null, null);
        List<Goods> goods = page != null && page.getList() != null ? page.getList() : List.of();

        result.put("goods", goods);
        result.put("highlight", highlight);
        return Result.success(result);
    }
}
