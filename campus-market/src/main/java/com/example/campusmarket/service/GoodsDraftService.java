package com.example.campusmarket.service;

import com.example.campusmarket.common.ForbiddenException;
import com.example.campusmarket.entity.GoodsDraft;
import com.example.campusmarket.mapper.GoodsDraftMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品草稿箱：发布页自动保存，支持多份草稿，随时继续编辑。
 */
@Service
public class GoodsDraftService {

    @Autowired
    private GoodsDraftMapper draftMapper;

    public List<GoodsDraft> list(Integer userId) {
        return draftMapper.findByUser(userId);
    }

    public GoodsDraft get(Integer userId, Integer id) {
        GoodsDraft draft = draftMapper.findByIdAndUser(id, userId);
        if (draft == null) {
            throw new IllegalArgumentException("草稿不存在或已被删除");
        }
        return draft;
    }

    /** 新建或更新草稿，返回草稿 ID 与最近保存时间 */
    public Map<String, Object> save(Integer userId, GoodsDraft draft) {
        sanitize(draft);
        draft.setUserId(userId);
        if (draft.getId() != null) {
            if (draftMapper.findByIdAndUser(draft.getId(), userId) == null) {
                throw new ForbiddenException("无权修改该草稿");
            }
            draftMapper.update(draft);
        } else {
            draftMapper.insert(draft);
        }
        GoodsDraft saved = draftMapper.findByIdAndUser(draft.getId(), userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", draft.getId());
        data.put("updateTime", saved == null ? null : saved.getUpdateTime());
        return data;
    }

    public String delete(Integer userId, Integer id) {
        if (draftMapper.deleteByIdAndUser(id, userId) == 0) {
            throw new IllegalArgumentException("草稿不存在或已被删除");
        }
        return "草稿已删除";
    }

    /** 草稿允许不完整，只做长度与空值归一化，避免自动保存时报错 */
    private void sanitize(GoodsDraft d) {
        d.setTitle(cut(trim(d.getTitle()), 100));
        d.setDescription(cut(trim(d.getDescription()), 2000));
        d.setLocation(cut(trim(d.getLocation()), 100));
        d.setExchangeWant(cut(trim(d.getExchangeWant()), 100));
        d.setTradeMode("exchange".equals(d.getTradeMode()) ? "exchange" : "cash");
        d.setCanMail(d.getCanMail() != null && d.getCanMail() == 1 ? 1 : 0);
        d.setCanDeliver(d.getCanDeliver() != null && d.getCanDeliver() == 1 ? 1 : 0);
        d.setExchangeSupported(d.getExchangeSupported() != null && d.getExchangeSupported() == 1 ? 1 : 0);
        d.setCoverImage(cut(trim(d.getCoverImage()), 255));
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }

    private String cut(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
