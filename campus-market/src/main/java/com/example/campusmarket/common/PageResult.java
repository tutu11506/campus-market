package com.example.campusmarket.common;

import lombok.Data;

import java.util.List;

/**
 * 统一分页返回结构。
 *
 * 字段固定为 list / total / pageNum(1-based) / pageSize / pages，
 * 与前端约定一致，避免各接口用 Map 返回导致字段漂移。
 * 注意：SQL 层只认 offset/size，pageNum 到 offset 的换算只在 Service 层做一次，
 * 严禁把 pageNum 当偏移量传给 Mapper（避免翻页错乱）。
 */
@Data
public class PageResult<T> {

    private List<T> list;
    private long total;
    private int pageNum;
    private int pageSize;
    private int pages;

    public static <T> PageResult<T> of(List<T> list, long total, int pageNum, int pageSize) {
        PageResult<T> r = new PageResult<>();
        r.setList(list);
        r.setTotal(total);
        r.setPageNum(pageNum);
        r.setPageSize(pageSize);
        r.setPages(pageSize > 0 ? (int) ((total + pageSize - 1) / pageSize) : 0);
        return r;
    }

    /**
     * 归一化分页参数：pageNum 兜底为 1，pageSize 默认 10 且不超过 maxSize。
     * 返回 [pageNum, pageSize, offset]，offset = (pageNum - 1) * pageSize。
     * 各 Service 统一调用，避免重复写钳制逻辑。
     */
    public static int[] normalize(Integer pageNum, Integer pageSize, int maxSize) {
        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        if (ps > maxSize) {
            ps = maxSize;
        }
        int offset = (pn - 1) * ps;
        return new int[]{pn, ps, offset};
    }
}
