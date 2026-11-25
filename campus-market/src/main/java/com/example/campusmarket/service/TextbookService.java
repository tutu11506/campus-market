package com.example.campusmarket.service;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.entity.Textbook;
import com.example.campusmarket.entity.TextbookMaterial;
import com.example.campusmarket.entity.TextbookWarning;
import com.example.campusmarket.entity.UserCourse;
import com.example.campusmarket.mapper.TextbookMapper;
import com.example.campusmarket.mapper.TextbookMaterialMapper;
import com.example.campusmarket.mapper.TextbookWarningMapper;
import com.example.campusmarket.mapper.UserCourseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 教材专区：教材主表管理 + 课表导入 + 版本避雷 + 笔记附件。
 *
 * 课表匹配逻辑：用户导入课表后，从 course_name 提取学科关键词，
 * 去 textbook.subject / title 里模糊匹配，命中的 textbook 聚合对应 subject。
 * 匹配后再查 Goods(category_id=1) 和 RentalItem(rental_type=textbook) 里
 * 关联该 textbook_id 或标题/学科命中的在售/可租项，标记"已有"或"还差"。
 */
@Service
public class TextbookService {

    private static final Set<String> VALID_WARN_REASONS =
            Set.of("why_not_allowed", "errors", "too_difficult", "outdated", "other");
    private static final Set<String> VALID_DELIVERY_MODES = Set.of("free", "bonus", "paid");
    private static final Set<String> VALID_OWNER_TYPES = Set.of("textbook", "goods", "rental");
    private static final Set<String> VALID_FILE_TYPES = Set.of("pdf", "ppt", "doc", "zip", "image");

    @Autowired
    private TextbookMapper textbookMapper;

    @Autowired
    private TextbookWarningMapper warningMapper;

    @Autowired
    private UserCourseMapper courseMapper;

    @Autowired
    private TextbookMaterialMapper materialMapper;

    /* ==================== 教材主表 ==================== */

    public PageResult<Textbook> list(String subject, Integer teacherFlag, String keyword,
                                    Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 20);
        return PageResult.of(
                textbookMapper.findList(trim(subject), teacherFlag, trim(keyword), p[2], p[1]),
                textbookMapper.countList(trim(subject), teacherFlag, trim(keyword)),
                p[0], p[1]);
    }

    public Textbook detail(Integer id) {
        Textbook t = textbookMapper.findById(id);
        if (t == null) {
            throw new IllegalArgumentException("教材不存在");
        }
        return t;
    }

    public List<Map<String, Object>> subjects() {
        return textbookMapper.subjects();
    }

    public Textbook create(Integer uid, Textbook form) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        if (form == null) {
            throw new IllegalArgumentException("请填写教材信息");
        }
        Textbook t = new Textbook();
        t.setIsbn(trim(form.getIsbn()) == null ? "" : form.getIsbn().trim());
        if (t.getIsbn() != null && !t.getIsbn().isEmpty() && textbookMapper.findByIsbn(t.getIsbn()) != null) {
            throw new IllegalArgumentException("该 ISBN 的教材已存在，请直接复用");
        }
        t.setTitle(required(form.getTitle(), 120, "请填写书名主标题"));
        t.setSubtitle(optional(form.getSubtitle(), 120));
        t.setAuthor(optional(form.getAuthor(), 120));
        t.setPublisher(optional(form.getPublisher(), 80));
        t.setEdition(optional(form.getEdition(), 40));
        t.setSubject(optional(form.getSubject(), 40));
        t.setGrade(optional(form.getGrade(), 40));
        t.setCoverImage(optional(form.getCoverImage(), 255));
        t.setTeacherFlag(form.getTeacherFlag() == null ? 0 : form.getTeacherFlag());
        t.setTeacherNote(optional(form.getTeacherNote(), 255));

        textbookMapper.insert(t);
        return textbookMapper.findById(t.getId());
    }

    public Textbook update(Integer uid, Textbook form) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        Textbook existing = detail(form.getId());
        Textbook patch = new Textbook();
        patch.setId(existing.getId());
        patch.setTitle(required(form.getTitle(), 120, "请填写书名主标题"));
        patch.setSubtitle(optional(form.getSubtitle(), 120));
        patch.setAuthor(optional(form.getAuthor(), 120));
        patch.setPublisher(optional(form.getPublisher(), 80));
        patch.setEdition(optional(form.getEdition(), 40));
        patch.setSubject(optional(form.getSubject(), 40));
        patch.setGrade(optional(form.getGrade(), 40));
        patch.setCoverImage(optional(form.getCoverImage(), 255));
        patch.setTeacherFlag(form.getTeacherFlag() == null ? existing.getTeacherFlag() : form.getTeacherFlag());
        patch.setTeacherNote(optional(form.getTeacherNote(), 255));

        if (textbookMapper.update(patch) == 0) {
            throw new IllegalArgumentException("教材不存在或已隐藏");
        }
        return textbookMapper.findById(existing.getId());
    }

    public Textbook markTeacher(Integer uid, Integer id, int flag, String note) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        detail(id); // exists
        if (textbookMapper.updateTeacherMark(id, flag, optional(note, 255)) == 0) {
            throw new IllegalArgumentException("标记失败");
        }
        return textbookMapper.findById(id);
    }

    /* ==================== 避雷 ==================== */

    public List<TextbookWarning> warnings(Integer textbookId) {
        detail(textbookId);
        return warningMapper.findByTextbook(textbookId, 0, 30);
    }

    public TextbookWarning addWarning(Integer uid, Integer textbookId, String reason, String detail) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        if (!VALID_WARN_REASONS.contains(reason)) {
            throw new IllegalArgumentException("避雷原因不正确");
        }
        detail(textbookId);
        TextbookWarning w = new TextbookWarning();
        w.setTextbookId(textbookId);
        w.setUserId(uid);
        w.setReason(reason);
        w.setDetail(optional(detail, 400));
        warningMapper.insert(w);
        textbookMapper.incrWarnCount(textbookId);
        return w;
    }

    /* ==================== 课表 ==================== */

    public List<String> semesters(Integer uid) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        return courseMapper.findSemesters(uid);
    }

    public List<UserCourse> myCourses(Integer uid, String semester) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        String sem = required(semester, 20, "请选择学期");
        return courseMapper.findBySemester(uid, sem);
    }

    /**
     * 批量导入课表：支持课程名/老师/课代码的 JSON 数组形式。
     * 同一 userId + courseName + semester 不重复插入。
     */
    public int importCourses(Integer uid, String semester, List<Map<String, String>> items, boolean replaceExisting) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        String sem = required(semester, 20, "请选择学期，如 2026-2027-1");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("课表为空");
        }
        int inserted = 0;
        for (Map<String, String> row : items) {
            String course = trim(row.get("courseName"));
            if (course == null || course.length() > 80) continue;
            if (replaceExisting && courseMapper.countByUnique(uid, course, sem) > 0) {
                continue;
            }
            if (!replaceExisting && courseMapper.countByUnique(uid, course, sem) > 0) {
                continue;
            }
            UserCourse uc = new UserCourse();
            uc.setUserId(uid);
            uc.setCourseName(course);
            uc.setTeacher(optional(row.get("teacher"), 40));
            uc.setSemester(sem);
            uc.setCourseCode(optional(row.get("courseCode"), 20));
            uc.setRemark(optional(row.get("remark"), 120));
            courseMapper.insert(uc);
            inserted++;
        }
        return inserted;
    }

    /**
     * 一键匹配：给定学期，列出课表 → 匹配 textbook.subject/title → 查在售闲置+可租 → 标记有/缺。
     * 返回每个匹配到的 textbook 及其"goodsCount / rentalCount"；没匹配到 textbook 的课程也单独返回，
     * 用于"还差哪些书"。
     */
    public List<Map<String, Object>> matchTextbooks(Integer uid, String semester) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        String sem = required(semester, 20, "请选择学期");
        List<String> courseNames = courseMapper.distinctCourseNames(uid, sem);
        List<Map<String, Object>> result = new ArrayList<>();

        for (String cn : courseNames) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("courseName", cn);

            // 1. subject 关键词匹配
            String[] tokens = cn.replaceAll("\\s+", "").split("[,，、/（）()\\-]+");
            List<String> matchedSubjects = new ArrayList<>();
            List<String> matchedTitles = new ArrayList<>();
            for (String token : tokens) {
                if (token.length() < 2) continue;
                List<Textbook> bySubject = textbookMapper.findBySubjectKeyword(token);
                for (Textbook t : bySubject) {
                    if (!matchedSubjects.contains(t.getSubject())) matchedSubjects.add(t.getSubject());
                }
                List<Textbook> byTitle = textbookMapper.findByTitleKeyword(token);
                for (Textbook t : byTitle) {
                    if (!matchedTitles.contains(t.getTitle())) matchedTitles.add(t.getTitle());
                }
            }
            row.put("matchedSubjects", matchedSubjects);
            row.put("matchedTitles", matchedTitles);

            // 2. 关联 textbook 列表（按 subject 或 title 聚合，再用 findById 补聚合字段）
            Set<Integer> seen = new HashSet<>();
            List<Integer> tIds = new ArrayList<>();
            for (String subj : matchedSubjects) {
                List<Textbook> hit = textbookMapper.findBySubjectKeyword(subj);
                for (Textbook t : hit) {
                    if (seen.add(t.getId())) tIds.add(t.getId());
                }
            }
            for (String title : matchedTitles) {
                List<Textbook> hit = textbookMapper.findByTitleKeyword(title);
                for (Textbook t : hit) {
                    if (seen.add(t.getId())) tIds.add(t.getId());
                }
            }

            // 用 findById（带子查询聚合）替换 goodsCount/rentalCount
            List<Textbook> tBooks = new ArrayList<>();
            for (Integer id : tIds) {
                Textbook full = textbookMapper.findById(id);
                if (full != null) tBooks.add(full);
            }

            // 3. 标记有/缺：goodsCount + rentalCount 任一 > 0 即"已有"
            List<Map<String, Object>> bookDetails = new ArrayList<>();
            for (Textbook t : tBooks) {
                Map<String, Object> b = new LinkedHashMap<>();
                b.put("textbookId", t.getId());
                b.put("title", t.getTitle());
                b.put("subtitle", t.getSubtitle());
                b.put("edition", t.getEdition());
                b.put("author", t.getAuthor());
                b.put("isbn", t.getIsbn());
                b.put("publisher", t.getPublisher());
                b.put("teacherFlag", t.getTeacherFlag());
                b.put("teacherNote", t.getTeacherNote());
                b.put("warnCount", t.getWarnCount());
                b.put("goodsCount", t.getGoodsCount() == null ? 0 : t.getGoodsCount());
                b.put("rentalCount", t.getRentalCount() == null ? 0 : t.getRentalCount());
                b.put("statusLabel",
                        ((t.getGoodsCount() == null ? 0 : t.getGoodsCount()) > 0
                                || (t.getRentalCount() == null ? 0 : t.getRentalCount()) > 0) ? "有" : "缺");
                bookDetails.add(b);
            }
            row.put("books", bookDetails);
            row.put("matched", !bookDetails.isEmpty());
            row.put("missing", bookDetails.isEmpty());
            result.add(row);
        }
        return result;
    }

    /* ==================== 笔记附件 ==================== */

    public List<TextbookMaterial> materialsByOwner(String ownerType, Integer ownerId) {
        if (!VALID_OWNER_TYPES.contains(ownerType)) {
            throw new IllegalArgumentException("附件关联类型不正确");
        }
        return materialMapper.findByOwner(ownerType, ownerId);
    }

    public TextbookMaterial addMaterial(Integer uid, TextbookMaterial form) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        if (form == null) {
            throw new IllegalArgumentException("请填写资料信息");
        }
        if (!VALID_OWNER_TYPES.contains(form.getOwnerType())) {
            throw new IllegalArgumentException("附件关联类型不正确");
        }
        if (form.getOwnerId() == null) {
            throw new IllegalArgumentException("请选择关联的教材/闲置/租赁");
        }
        String delivery = form.getDeliveryMode() == null ? "free" : form.getDeliveryMode();
        if (!VALID_DELIVERY_MODES.contains(delivery)) {
            throw new IllegalArgumentException("资料交付模式不正确");
        }
        String fileType = form.getFileType();
        if (fileType == null || !VALID_FILE_TYPES.contains(fileType)) {
            throw new IllegalArgumentException("资料类型不正确");
        }
        if (form.getFileUrl() == null || form.getFileUrl().length() > 500) {
            throw new IllegalArgumentException("请上传资料文件");
        }
        if ("paid".equals(delivery) && (form.getPrice() == null || form.getPrice().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("单独售卖资料需填写价格");
        }
        TextbookMaterial m = new TextbookMaterial();
        m.setOwnerType(form.getOwnerType());
        m.setOwnerId(form.getOwnerId());
        m.setUploaderId(uid);
        m.setTitle(required(form.getTitle(), 100, "请填写资料标题"));
        m.setDescription(optional(form.getDescription(), 300));
        m.setFileType(fileType);
        m.setFileUrl(form.getFileUrl());
        m.setFileSize(form.getFileSize());
        m.setDeliveryMode(delivery);
        m.setPrice(form.getPrice() == null ? BigDecimal.ZERO : form.getPrice());

        materialMapper.insert(m);
        return materialMapper.findById(m.getId());
    }

    public TextbookMaterial view(Integer id) {
        TextbookMaterial m = materialMapper.findById(id);
        if (m == null) {
            throw new IllegalArgumentException("资料不存在");
        }
        materialMapper.incrViews(id);
        return m;
    }

    public TextbookMaterial download(Integer id) {
        TextbookMaterial m = materialMapper.findById(id);
        if (m == null) {
            throw new IllegalArgumentException("资料不存在");
        }
        materialMapper.incrDownloads(id);
        return m;
    }

    public PageResult<TextbookMaterial> myUploads(Integer uid, Integer pageNum, Integer pageSize) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        int[] p = PageResult.normalize(pageNum, pageSize, 20);
        return PageResult.of(
                materialMapper.findMyUploads(uid, p[2], p[1]),
                materialMapper.countMyUploads(uid),
                p[0], p[1]);
    }

    public void deleteMaterial(Integer uid, Integer id) {
        if (uid == null) {
            throw new IllegalArgumentException("请先登录");
        }
        if (materialMapper.softDelete(id, uid) == 0) {
            throw new IllegalArgumentException("资料不存在或无权限删除");
        }
    }

    /* ==================== 内部工具 ==================== */

    private String trim(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    private String required(String s, int max, String msg) {
        String v = trim(s);
        if (v == null || v.length() > max) {
            throw new IllegalArgumentException(msg);
        }
        return v;
    }

    private String optional(String s, int max) {
        String v = trim(s);
        if (v == null) return null;
        if (v.length() > max) throw new IllegalArgumentException("字段过长");
        return v;
    }
}
