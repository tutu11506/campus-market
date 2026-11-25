package com.example.campusmarket.controller;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.Result;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.Textbook;
import com.example.campusmarket.entity.TextbookMaterial;
import com.example.campusmarket.entity.TextbookWarning;
import com.example.campusmarket.entity.UserCourse;
import com.example.campusmarket.service.TextbookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/textbooks")
public class TextbookController {

    @Autowired
    private TextbookService textbookService;

    /* ==================== 教材主表（公开浏览） ==================== */

    @GetMapping
    public Result<PageResult<Textbook>> list(@RequestParam(required = false) String subject,
                                              @RequestParam(required = false) Integer teacherFlag,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(textbookService.list(subject, teacherFlag, keyword, pageNum, pageSize));
    }

    @GetMapping("/subjects")
    public Result<List<Map<String, Object>>> subjects() {
        return Result.success(textbookService.subjects());
    }

    @GetMapping("/{id}")
    public Result<Textbook> detail(@PathVariable Integer id) {
        return Result.success(textbookService.detail(id));
    }

    /** 登录发布新教材（手动） */
    @PostMapping
    public Result<Textbook> create(@RequestBody Textbook form) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("请先登录");
        return Result.success(textbookService.create(uid, form));
    }

    @PostMapping("/{id}/update")
    public Result<Textbook> update(@PathVariable Integer id, @RequestBody Textbook form) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("请先登录");
        form.setId(id);
        return Result.success(textbookService.update(uid, form));
    }

    /** 标记/取消老师指定版本 */
    @PostMapping("/{id}/teacher-mark")
    public Result<Textbook> teacherMark(@PathVariable Integer id,
                                         @RequestBody Map<String, Object> body) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("请先登录");
        int flag = ((Number) body.getOrDefault("flag", 1)).intValue();
        String note = (String) body.get("note");
        return Result.success(textbookService.markTeacher(uid, id, flag, note));
    }

    /* ==================== 避雷 ==================== */

    @GetMapping("/{id}/warnings")
    public Result<List<TextbookWarning>> warnings(@PathVariable Integer id) {
        return Result.success(textbookService.warnings(id));
    }

    @PostMapping("/{id}/warnings")
    public Result<TextbookWarning> addWarning(@PathVariable Integer id,
                                               @RequestBody Map<String, String> body) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("请先登录");
        return Result.success(textbookService.addWarning(uid, id,
                body.get("reason"), body.get("detail")));
    }

    /* ==================== 课表（需登录） ==================== */

    @GetMapping("/courses/semesters")
    public Result<List<String>> mySemesters() {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("请先登录");
        return Result.success(textbookService.semesters(uid));
    }

    @GetMapping("/courses")
    public Result<List<UserCourse>> myCourses(@RequestParam String semester) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("请先登录");
        return Result.success(textbookService.myCourses(uid, semester));
    }

    @PostMapping("/courses/import")
    public Result<Integer> importCourses(@RequestBody Map<String, Object> body) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("请先登录");
        String semester = (String) body.get("semester");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> items = (List<Map<String, String>>) body.get("items");
        boolean replace = body.get("replaceExisting") != null && ((Boolean) body.get("replaceExisting"));
        return Result.success(textbookService.importCourses(uid, semester, items, replace));
    }

    @GetMapping("/courses/match")
    public Result<List<Map<String, Object>>> match(@RequestParam String semester) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("请先登录");
        return Result.success(textbookService.matchTextbooks(uid, semester));
    }

    /* ==================== 笔记附件 ==================== */

    /** 公开：教材/闲置/租赁下的附件列表 */
    @GetMapping("/materials")
    public Result<List<TextbookMaterial>> materialsByOwner(@RequestParam String ownerType,
                                                            @RequestParam Integer ownerId) {
        return Result.success(textbookService.materialsByOwner(ownerType, ownerId));
    }

    @GetMapping("/materials/{id}")
    public Result<TextbookMaterial> viewMaterial(@PathVariable Integer id) {
        return Result.success(textbookService.view(id));
    }

    @PostMapping("/materials/{id}/download")
    public Result<TextbookMaterial> downloadMaterial(@PathVariable Integer id) {
        return Result.success(textbookService.download(id));
    }

    @PostMapping("/materials")
    public Result<TextbookMaterial> addMaterial(@RequestBody TextbookMaterial form) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("请先登录");
        return Result.success(textbookService.addMaterial(uid, form));
    }

    @GetMapping("/materials/my")
    public Result<PageResult<TextbookMaterial>> myMaterials(@RequestParam(defaultValue = "1") Integer pageNum,
                                                             @RequestParam(defaultValue = "20") Integer pageSize) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("请先登录");
        return Result.success(textbookService.myUploads(uid, pageNum, pageSize));
    }

    @PostMapping("/materials/{id}/delete")
    public Result<String> deleteMaterial(@PathVariable Integer id) {
        Integer uid = SecurityUtils.getCurrentUserId();
        if (uid == null) return Result.error("请先登录");
        textbookService.deleteMaterial(uid, id);
        return Result.success("已删除");
    }
}
