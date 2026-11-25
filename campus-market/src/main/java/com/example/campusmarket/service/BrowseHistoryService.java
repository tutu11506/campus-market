package com.example.campusmarket.service;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.SecurityUtils;
import com.example.campusmarket.entity.Goods;
import com.example.campusmarket.mapper.BrowseHistoryMapper;
import com.example.campusmarket.vo.BrowsedGoodsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BrowseHistoryService {

    @Autowired
    private BrowseHistoryMapper browseHistoryMapper;
    @Autowired
    private GoodsService goodsService;

    /**
     * 记录当前用户对某商品的浏览（幂等：同用户同商品只刷新时间/累加次数）。
     * 注意：不在这里递增 goods.view_count——该全局计数由商品详情公开接口 findById 负责，避免重复计数。
     * 返回 null 表示成功，否则为错误提示。
     */
    public String record(Integer goodsId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return "请先登录";
        }
        if (goodsId == null) {
            return "商品ID不能为空";
        }
        Goods goods = goodsService.getById(goodsId);   // 只读取商品，不增加浏览量
        if (goods == null || (goods.getIsDeleted() != null && goods.getIsDeleted() == 1)) {
            return "商品不存在或已删除";
        }
        browseHistoryMapper.upsert(userId, goodsId, goods.getSellerId());
        return null;
    }

    /** 当前用户在指定卖家店铺中浏览过的在售商品 */
    public List<Goods> shopBrowsedGoods(Integer sellerId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null || sellerId == null) {
            return List.of();
        }
        return browseHistoryMapper.findBrowsedGoodsBySeller(userId, sellerId);
    }

    /** 我的浏览历史（分页） */
    public PageResult<BrowsedGoodsVO> myHistory(Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        Integer userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return PageResult.of(List.of(), 0, p[0], p[1]);
        }
        long total = browseHistoryMapper.countMyHistory(userId);
        List<BrowsedGoodsVO> list = browseHistoryMapper.findMyHistory(userId, p[2], p[1]);
        return PageResult.of(list, total, p[0], p[1]);
    }
}
