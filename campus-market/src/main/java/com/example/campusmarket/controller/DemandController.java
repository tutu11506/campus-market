package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.DemandPost;
import com.example.campusmarket.entity.DemandQuote;
import com.example.campusmarket.service.DemandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/demand")
public class DemandController {

    @Autowired
    private DemandService demandService;

    /** 求购列表（公开）；mine=1 时只看自己发布的 */
    @GetMapping("/posts")
    public Result<PageResult<DemandPost>> list(@RequestParam(required = false) String category,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(defaultValue = "false") boolean mine,
                                               @RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (mine && uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(demandService.list(category, keyword, mine ? uid : null, pageNum, pageSize));
    }

    /** 求购详情：含报价、系统匹配商品、我的报价 */
    @GetMapping("/posts/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Integer id) {
        return Result.success(demandService.detail(id, SecurityUtils.getCurrentUserId()));
    }

    /** 发布求购 */
    @PostMapping("/posts")
    public Result<DemandPost> create(@RequestBody Map<String, Object> body) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        DemandPost form = new DemandPost();
        form.setTitle(str(body, "title"));
        form.setCategory(str(body, "category"));
        form.setKeyword(str(body, "keyword"));
        form.setDescription(str(body, "description"));
        form.setBudgetMin(decimal(body, "budgetMin"));
        form.setBudgetMax(decimal(body, "budgetMax"));
        form.setTargetMajor(str(body, "targetMajor"));
        form.setTargetGrade(str(body, "targetGrade"));
        return Result.success(demandService.create(uid, form));
    }

    /** 卖家报价 */
    @PostMapping("/posts/{id}/quotes")
    public Result<DemandQuote> quote(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        BigDecimal price = decimal(body, "price");
        String message = str(body, "message");
        return Result.success(demandService.quote(uid, id, price, message));
    }

    /** 发布者选定报价（接单） */
    @PostMapping("/quotes/{id}/accept")
    public Result<String> accept(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(demandService.accept(uid, id));
    }

    /** 关闭自己的求购 */
    @PostMapping("/posts/{id}/close")
    public Result<String> close(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) {
            return Result.error("请先登录");
        }
        return Result.success(demandService.close(uid, id));
    }

    private String str(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private BigDecimal decimal(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return BigDecimal.valueOf(((Number) v).doubleValue());
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("金额格式不正确");
        }
    }
}
