package com.example.campusmarket.service;

import com.example.campusmarket.entity.Goods;
import com.example.campusmarket.entity.GoodsExchange;
import com.example.campusmarket.mapper.ExchangeMapper;
import com.example.campusmarket.mapper.GoodsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 以物换物：不用现金，双方拿在售商品直接互换。
 */
@Service
public class ExchangeService {

    @Autowired
    private ExchangeMapper exchangeMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    public List<GoodsExchange> received(Integer uid) {
        return exchangeMapper.findReceived(uid);
    }

    public List<GoodsExchange> sent(Integer uid) {
        return exchangeMapper.findSent(uid);
    }

    /** 发起换物：拿自己的一件在售商品，申请换对方的商品 */
    public String apply(Integer uid, Integer goodsId, Integer offerGoodsId, String message) {
        if (uid == null) {
            return "请先登录";
        }
        if (goodsId == null || offerGoodsId == null) {
            return "请选择想换的商品和你拿出的商品";
        }
        Goods target = goodsMapper.findById(goodsId);
        if (target == null || !"on_sale".equals(target.getStatus())) {
            return "目标商品不存在或已下架";
        }
        boolean supported = "exchange".equals(target.getTradeMode())
                || (target.getExchangeSupported() != null && target.getExchangeSupported() == 1);
        if (!supported) {
            return "该商品暂不支持换物";
        }
        if (uid.equals(target.getSellerId())) {
            return "不能换自己的商品";
        }
        Goods offer = goodsMapper.findById(offerGoodsId);
        if (offer == null || !uid.equals(offer.getSellerId())) {
            return "请选择你自己发布的商品";
        }
        if (!"on_sale".equals(offer.getStatus())) {
            return "你选择的商品已下架或交易中";
        }
        if (goodsId.equals(offerGoodsId)) {
            return "不能和自己互换";
        }
        if (exchangeMapper.findIdByGoodsAndRequester(goodsId, uid) != null) {
            return "你已经申请过这件商品了";
        }
        String msg = message == null ? null : message.trim();
        if (msg != null && msg.length() > 200) {
            return "留言最多 200 字";
        }
        GoodsExchange e = new GoodsExchange();
        e.setGoodsId(goodsId);
        e.setRequesterId(uid);
        e.setOfferGoodsId(offerGoodsId);
        e.setMessage(msg);
        exchangeMapper.insert(e);
        return "换物申请已发送，等待对方确认";
    }

    /** 商品所有者接受换物：双方商品锁定，线下交换 */
    @Transactional(rollbackFor = Exception.class)
    public String accept(Integer uid, Integer id) {
        GoodsExchange e = exchangeMapper.findById(id);
        if (e == null) {
            return "换物申请不存在";
        }
        if (!uid.equals(e.getOwnerId())) {
            return "只有商品所有者可以接受换物";
        }
        if (!"pending".equals(e.getStatus())) {
            return "该申请已处理";
        }
        if (goodsMapper.lockIfOnSale(e.getGoodsId()) == 0) {
            return "商品已被拍下或下架，无法换物";
        }
        if (goodsMapper.lockIfOnSale(e.getOfferGoodsId()) == 0) {
            throw new IllegalStateException("对方商品已被拍下或下架，换物取消");
        }
        exchangeMapper.updateStatus(id, "accepted");
        exchangeMapper.rejectOthers(e.getGoodsId(), id);
        return "已接受换物，双方商品已锁定，线下交换后请确认完成";
    }

    public String reject(Integer uid, Integer id) {
        GoodsExchange e = exchangeMapper.findById(id);
        if (e == null) {
            return "换物申请不存在";
        }
        if (!uid.equals(e.getOwnerId())) {
            return "只有商品所有者可以拒绝";
        }
        if (!"pending".equals(e.getStatus())) {
            return "该申请已处理";
        }
        exchangeMapper.updateStatus(id, "rejected");
        return "已拒绝该换物申请";
    }

    public String cancel(Integer uid, Integer id) {
        GoodsExchange e = exchangeMapper.findById(id);
        if (e == null) {
            return "换物申请不存在";
        }
        if (!uid.equals(e.getRequesterId())) {
            return "只有发起人可以取消";
        }
        if (!"pending".equals(e.getStatus())) {
            return "该申请已处理";
        }
        exchangeMapper.updateStatus(id, "cancelled");
        return "已取消换物申请";
    }

    /** 双方各自确认完成，两件商品同时成交 */
    @Transactional(rollbackFor = Exception.class)
    public String finish(Integer uid, Integer id) {
        GoodsExchange e = exchangeMapper.findById(id);
        if (e == null) {
            return "换物申请不存在";
        }
        if (!"accepted".equals(e.getStatus())) {
            return "换物尚未达成或已完成";
        }
        if (uid.equals(e.getRequesterId())) {
            exchangeMapper.markRequesterDone(id);
        } else if (uid.equals(e.getOwnerId())) {
            exchangeMapper.markOwnerDone(id);
        } else {
            return "无权操作该换物";
        }
        GoodsExchange latest = exchangeMapper.findById(id);
        if (latest.getRequesterDone() != null && latest.getRequesterDone() == 1
                && latest.getOwnerDone() != null && latest.getOwnerDone() == 1) {
            exchangeMapper.updateStatus(id, "completed");
            goodsMapper.updateStatus(latest.getGoodsId(), "sold");
            goodsMapper.updateStatus(latest.getOfferGoodsId(), "sold");
            return "换物完成，两件商品均已成交";
        }
        return "已确认，等待对方确认完成";
    }
}
