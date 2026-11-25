package com.example.campusmarket.service;

import com.example.campusmarket.entity.Goods;
import com.example.campusmarket.entity.User;
import com.example.campusmarket.mapper.UserMapper;
import com.example.campusmarket.utils.GradeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 校园差异化特征服务。
 * 给商品列表批量注入"年级阶梯价 / 毕业清仓"等校园专属属性，
 * 这些信息闲鱼/转转永远算不出来 —— 它们没有学号、没有学期。
 */
@Service
public class CampusFeatureService {

    @Autowired
    private UserMapper userMapper;

    /**
     *  enrich 一个商品列表：给每个 Goods 注入 graduationSale / priceRelation / priceTag。
     *  buyerStudentNo 为 null 表示未登录 —— 只算卖家自己的毕业清仓状态。
     */
    public void enrichGoods(List<Goods> goodsList, String buyerStudentNo) {
        if (goodsList == null) return;
        for (Goods g : goodsList) {
            enrichOne(g, buyerStudentNo);
        }
    }

    /**
     * enrich 单个商品。
     */
    public void enrichOne(Goods g, String buyerStudentNo) {
        if (g == null) return;

        // 卖家是否毕业清仓期
        boolean graduation = GradeUtil.isGraduationSale(g.getSellerStudentNo());
        g.setGraduationSale(graduation);

        // 与当前买家的年级关系
        if (buyerStudentNo != null && !buyerStudentNo.isEmpty()) {
            String relation = GradeUtil.priceRelation(buyerStudentNo, g.getSellerStudentNo());
            g.setPriceRelation(relation);

            // 拼价格标签
            String tag = buildPriceTag(graduation, relation);
            g.setPriceTag(tag);
        } else {
            // 未登录只显示毕业清仓标
            g.setPriceTag(graduation ? "🎓 毕业清仓" : null);
        }
    }

    private String buildPriceTag(boolean graduation, String relation) {
        if (graduation && "senior_sale".equals(relation)) {
            return "🎓 学长学姐特惠";  // 最强差异化：毕业清仓 + 买家是学弟学妹
        }
        if (graduation) {
            return "🎓 毕业清仓";
        }
        if ("junior_sale".equals(relation)) {
            return "🏫 同校好货";
        }
        return null;
    }

    /**
     * 通过 userId 拿 studentNo（用于 enrich 时关联当前买家身份）。
     */
    public String findStudentNo(Integer userId) {
        if (userId == null) return null;
        User u = userMapper.findById(userId);
        return u != null ? u.getStudentNo() : null;
    }
}
