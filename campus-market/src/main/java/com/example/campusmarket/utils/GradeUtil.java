package com.example.campusmarket.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * 校园身份推断工具。
 * 从学号推断年级、学期身份（学长/学弟、是否毕业季清仓期等）。
 * 核心假设：学号前 4 位 = 入学年份（例：20240001 → 2024 级大一新生）。
 *
 * 这些信息闲鱼/转转永远拿不到 —— 这是校园专属差异化的根基。
 */
public final class GradeUtil {

    private GradeUtil() {
    }

    /** 本科一般四年，可调整（部分专业五年） */
    public static final int UNDERGRAD_YEARS = 4;

    /**
     * 从学号提取入学年份。
     * 学号前 4 位必须是数字，否则返回 -1（表示无法推断）。
     */
    public static int enrollYear(String studentNo) {
        if (studentNo == null || studentNo.length() < 4) return -1;
        try {
            String prefix = studentNo.substring(0, 4);
            return Integer.parseInt(prefix);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * 把 Date 转 LocalDate（默认时区 Asia/Shanghai）。
     */
    public static LocalDate toLocalDate(Date d) {
        if (d == null) return null;
        return d.toInstant().atZone(ZoneId.of("Asia/Shanghai")).toLocalDate();
    }

    /**
     * 当前学期阶段（用于判断是否处于开学季/毕业季窗口）。
     * 返回值：
     *   "summer_new"   — 8月中旬 ~ 9月中旬（秋季开学季）
     *   "spring_new"   — 1月中旬 ~ 2月中旬（春季开学季）
     *   "graduation"   — 5月中旬 ~ 6月中旬（毕业清仓季）
     *   "normal"       — 普通时期
     */
    public static String currentPhase() {
        LocalDate today = LocalDate.now();
        int m = today.getMonthValue();
        int d = today.getDayOfMonth();

        // 秋季开学季：8.15 ~ 9.15
        if ((m == 8 && d >= 15) || (m == 9 && d <= 15)) return "summer_new";
        // 春季开学季：1.15 ~ 2.15
        if ((m == 1 && d >= 15) || (m == 2 && d <= 15)) return "spring_new";
        // 毕业季：5.15 ~ 6.15
        if ((m == 5 && d >= 15) || (m == 6 && d <= 15)) return "graduation";

        return "normal";
    }

    /**
     * 学期窗口描述（给前端展示用）。
     */
    public static String phaseDescription(String phase) {
        switch (phase) {
            case "summer_new": return "🎒 秋季开学季";
            case "spring_new": return "📘 春季开学季";
            case "graduation":  return "🎓 毕业清仓季";
            default:            return "";
        }
    }

    /**
     * 判断某卖家是否处于"毕业清仓"状态。
     * 条件：
     *   1) 能从学号推断入学年份；
     *   2) 当前日期 ≥ 入学年份 + 4 年 + 1月（大四下学期及以后）。
     *
     * 例：学号 2021xxxx → 2021+4 = 2025 → 2025 年 2 月起视为毕业季清货期。
     */
    public static boolean isGraduationSale(String sellerStudentNo) {
        int year = enrollYear(sellerStudentNo);
        if (year < 2000 || year > 2100) return false;
        LocalDate today = LocalDate.now();
        // 大四下学期及以后 = 入学年份 + 4 年后的 2 月 1 日起
        LocalDate graduationThreshold = LocalDate.of(year + UNDERGRAD_YEARS, 2, 1);
        return !today.isBefore(graduationThreshold);
    }

    /**
     * 判断买家是否是卖家的"学弟学妹"（年级至少低 2 级）。
     * 年级差 ≥ 2 才有"学长学姐"意义（差 1 级太接近，没那种情感驱动）。
     *
     * 返回值含义：
     *   "senior_sale"  — 卖家是学长，正在毕业清仓，优惠合理
     *   "peer_sale"    — 同届/差一级，普通二手
     *   "junior_sale"  — 卖家反而更小，一般不会清仓
     */
    public static String priceRelation(String buyerStudentNo, String sellerStudentNo) {
        int buyerYear = enrollYear(buyerStudentNo);
        int sellerYear = enrollYear(sellerStudentNo);
        if (buyerYear < 0 || sellerYear < 0) return "peer_sale";

        int diff = buyerYear - sellerYear;   // 正数 = 买家是学弟
        if (diff >= 2) return "senior_sale";  // 买家学弟，卖家学长 → 毕业清仓价
        if (diff <= -2) return "junior_sale"; // 卖家学弟
        return "peer_sale";
    }
}
