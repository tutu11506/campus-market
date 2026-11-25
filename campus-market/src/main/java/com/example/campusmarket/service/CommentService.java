package com.example.campusmarket.service;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.ForbiddenException;
import com.example.campusmarket.entity.Comment;
import com.example.campusmarket.entity.Order;
import com.example.campusmarket.mapper.CommentMapper;
import com.example.campusmarket.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private OrderMapper orderMapper;

    // 发布评价
    public String add(Comment comment) {
        if (comment == null || comment.getOrderId() == null) {
            return "参数错误";
        }
        // 评分与内容校验
        if (comment.getRating() == null || comment.getRating() < 1 || comment.getRating() > 5) {
            return "评分必须在 1-5 之间";
        }
        if (comment.getContent() != null && comment.getContent().length() > 500) {
            return "评价内容不能超过 500 字";
        }
        // 1. 查询订单信息
        Order order = orderMapper.findById(comment.getOrderId());
        if (order == null) {
            return "订单不存在";
        }

        // 2. 只有已完成的订单才能评价
        if (!"completed".equals(order.getStatus())) {
            return "订单未完成，无法评价";
        }

        // 2.1 只有该订单的买卖双方才能评价，且各自只能评一次（互评）
        boolean isBuyer = order.getBuyerId().equals(comment.getFromUserId());
        boolean isSeller = order.getSellerId().equals(comment.getFromUserId());
        if (!isBuyer && !isSeller) {
            throw new ForbiddenException("无权评价该订单");
        }

        // 3. 不能重复评价（按「订单 + 评价人」去重，买卖双方各一次）
        Comment exist = commentMapper.findByOrderIdAndFromUserId(comment.getOrderId(), comment.getFromUserId());
        if (exist != null) {
            return "该订单已评价过";
        }

        // 4. 设置商品ID与被评价人：买家评卖家，卖家评买家
        comment.setGoodsId(order.getGoodsId());
        comment.setToUserId(isBuyer ? order.getSellerId() : order.getBuyerId());

        commentMapper.insert(comment);
        return "评价成功";
    }

    // 分页查询商品的评价
    public PageResult<Comment> findByGoodsId(Integer goodsId, Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        List<Comment> list = commentMapper.findByGoodsIdPage(goodsId, p[2], p[1]);
        return PageResult.of(list, commentMapper.countByGoodsId(goodsId), p[0], p[1]);
    }

    // 分页查询我收到的评价
    public PageResult<Comment> findMyComments(Integer userId, Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        List<Comment> list = commentMapper.findByToUserIdPage(userId, p[2], p[1]);
        return PageResult.of(list, commentMapper.countByToUserId(userId), p[0], p[1]);
    }
}