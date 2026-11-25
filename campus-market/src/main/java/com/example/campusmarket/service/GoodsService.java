package com.example.campusmarket.service;

import com.example.campusmarket.common.PageResult;
import com.example.campusmarket.common.TtlCache;
import com.example.campusmarket.entity.Goods;
import com.example.campusmarket.mapper.GoodsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private CampusFeatureService campusFeatureService;

    /**
     * 商品列表是首页、市场、分类页最热的读接口，匿名请求缓存 15 秒；
     * 商品新增/修改/上下架/删除时立即清空，保证卖家刚发布的商品马上可见。
     */
    private final TtlCache<PageResult<Goods>> listCache = new TtlCache<>(15_000L, 300);

    // 当前登录买家的学号（用于 enrich 价格关系标签）
    private String currentBuyerStudentNo() {
        try {
            Integer uid = com.example.campusmarket.common.SecurityUtils.getCurrentUserId();
            return campusFeatureService.findStudentNo(uid);
        } catch (Exception e) {
            return null;
        }
    }

    // 发布商品
    public String add(Goods goods) {
        normalizeTradeOptions(goods);
        applyRaiseCouponPricing(goods);
        goodsMapper.insert(goods);
        listCache.clear();
        return "发布成功";
    }

    /**
     * 一键复制商品：复制自己已发布的商品信息，生成 count 件新的在售商品。
     * 适合同款多件（如多本相同教材）快速上新，只需指定份数。
     * 内容与原商品一致且已过发布审核，这里不再重复做违禁品扫描。
     */
    public List<Integer> copy(Integer sellerId, Integer goodsId, Integer count) {
        Goods src = goodsMapper.findById(goodsId);
        if (src == null) {
            throw new IllegalArgumentException("商品不存在或已被删除");
        }
        if (!sellerId.equals(src.getSellerId())) {
            throw new com.example.campusmarket.common.ForbiddenException("只能复制自己发布的商品");
        }
        int n = count == null ? 1 : count;
        if (n < 1 || n > 10) {
            throw new IllegalArgumentException("复制数量需在 1-10 之间");
        }
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Goods copy = new Goods();
            copy.setTitle(src.getTitle());
            copy.setDescription(src.getDescription());
            copy.setOriginalPrice(src.getOriginalPrice());
            copy.setImages(src.getImages());
            copy.setCoverImage(src.getCoverImage());
            copy.setCategoryId(src.getCategoryId());
            copy.setTextbookId(src.getTextbookId());
            copy.setSellerId(sellerId);
            copy.setLocation(src.getLocation());
            copy.setTradeMode(src.getTradeMode());
            copy.setCanMail(src.getCanMail());
            copy.setCanDeliver(src.getCanDeliver());
            copy.setExchangeSupported(src.getExchangeSupported());
            copy.setExchangeWant(src.getExchangeWant());
            copy.setRiskLevel(src.getRiskLevel());
            copy.setRiskReason(src.getRiskReason());
            if (src.getBasePrice() != null) {
                // 用基础价回填，避免把平台加价再叠加一次
                copy.setPrice(src.getBasePrice());
                add(copy);
            } else {
                // 历史数据没有基础价：原样保留价格，不再重新加价
                copy.setPrice(src.getPrice());
                copy.setBasePrice(src.getBasePrice());
                copy.setListPrice(src.getListPrice());
                copy.setCouponAmount(src.getCouponAmount());
                normalizeTradeOptions(copy);
                goodsMapper.insert(copy);
            }
            ids.add(copy.getId());
        }
        listCache.clear();
        return ids;
    }

    /** 交易方式与配送标签归一化：换物模式自动带上“支持换物” */
    private void normalizeTradeOptions(Goods goods) {
        boolean exchange = "exchange".equals(goods.getTradeMode());
        goods.setTradeMode(exchange ? "exchange" : "cash");
        goods.setCanMail(goods.getCanMail() != null && goods.getCanMail() == 1 ? 1 : 0);
        goods.setCanDeliver(goods.getCanDeliver() != null && goods.getCanDeliver() == 1 ? 1 : 0);
        boolean supported = exchange || (goods.getExchangeSupported() != null && goods.getExchangeSupported() == 1);
        goods.setExchangeSupported(supported ? 1 : 0);
        String want = goods.getExchangeWant() == null ? null : goods.getExchangeWant().trim();
        if (want != null && want.isEmpty()) {
            want = null;
        }
        if (want != null && want.length() > 100) {
            want = want.substring(0, 100);
        }
        goods.setExchangeWant(supported ? want : null);
    }

    /**
     * 涨价返券规则：卖家填基础价 P，平台按档位加价 M。
     * 标价 = P + M，优惠券 = M / 2，券后价（买家实付）= P + M / 2。
     */
    private void applyRaiseCouponPricing(Goods goods) {
        if (goods.getPrice() == null) return;
        BigDecimal base = goods.getPrice().setScale(2, RoundingMode.HALF_UP);
        int markup = markupOf(base);
        BigDecimal listPrice = base.add(BigDecimal.valueOf(markup));
        BigDecimal coupon = BigDecimal.valueOf(markup / 2);
        goods.setBasePrice(base);
        goods.setListPrice(listPrice);
        goods.setCouponAmount(coupon);
        goods.setPrice(listPrice.subtract(coupon));
    }

    /** 按基础价分档加价：档位越高加价越多，优惠券取加价的一半 */
    public static int markupOf(BigDecimal base) {
        if (base == null) return 0;
        double v = base.doubleValue();
        if (v < 50) return 6;      // 券 3 元
        if (v < 200) return 16;    // 券 8 元
        if (v < 1000) return 40;   // 券 20 元
        return 100;                // 券 50 元
    }

    // 商品详情（公开访问，浏览量 +1）
    public Goods findById(Integer id) {
        goodsMapper.addViewCount(id);
        Goods goods = goodsMapper.findById(id);
        campusFeatureService.enrichOne(goods, currentBuyerStudentNo());
        enrichSellerProfile(goods);
        return goods;
    }

    /**
     * 给详情页补卖家信用档案：信用分、历史成交、在售件数、注册天数、等级标签。
     * 这是校园场景特有的"熟人社会式"信任建设 —— 校内的交易对象看得见、找得到。
     */
    private void enrichSellerProfile(Goods goods) {
        if (goods == null || goods.getSellerId() == null) return;
        try {
            Map<String, Object> profile = goodsMapper.findSellerProfile(goods.getSellerId());
            if (profile == null) return;
            int credit = toInt(profile.get("credit"));
            int soldCount = toInt(profile.get("soldCount"));
            int onSaleCount = toInt(profile.get("onSaleCount"));
            int joinDays = toInt(profile.get("joinDays"));
            goods.setSellerCredit(credit);
            goods.setSellerSoldCount(soldCount);
            goods.setSellerOnSaleCount(onSaleCount);
            goods.setSellerJoinDays(joinDays);
            goods.setSellerLevel(sellerLevel(credit, soldCount, joinDays));
        } catch (Exception e) {
            // 档案补全失败不影响详情主流程
        }
    }

    /** 卖家等级：信用分是主指标，成交单数与注册时长做加分 */
    private String sellerLevel(int credit, int soldCount, int joinDays) {
        if (credit >= 110 || (credit >= 100 && soldCount >= 10)) return "金牌卖家";
        if (credit >= 95 || (credit >= 90 && soldCount >= 3) || joinDays >= 180) return "可信卖家";
        if (credit >= 80) return "普通卖家";
        return "谨慎交易";
    }

    private int toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // 内部只读获取（鉴权/下单复用，不增加浏览量，避免写操作污染计数）
    public Goods getById(Integer id) {
        return goodsMapper.findById(id);
    }

    // 分页查询商品列表（支持分类、关键词、价格区间、卖家、交易方式和排序）
    public PageResult<Goods> findList(Integer categoryId, String keyword,
                                       BigDecimal minPrice, BigDecimal maxPrice,
                                       Integer sellerId,
                                       String sort, Integer pageNum, Integer pageSize,
                                       String location, Integer canMail, Integer canDeliver,
                                       Integer exchangeSupported) {
        // pageNum 兜底为 1、pageSize 默认 10 且不超过 50，offset 在此统一换算
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        // 全文检索使用 BOOLEAN MODE，先清掉运算符字符，避免用户输入被当成检索语法
        String kw = normalizeSearchKeyword(keyword);
        // 登录用户会附加“价格关系”等个性化标签，结果因人而异，只缓存匿名访问
        String studentNo = currentBuyerStudentNo();
        String cacheKey = null;
        if (studentNo == null) {
            cacheKey = String.join("|", String.valueOf(categoryId), String.valueOf(kw),
                    String.valueOf(minPrice), String.valueOf(maxPrice), String.valueOf(sellerId),
                    String.valueOf(sort), String.valueOf(p[0]), String.valueOf(p[1]),
                    String.valueOf(location), String.valueOf(canMail),
                    String.valueOf(canDeliver), String.valueOf(exchangeSupported));
            PageResult<Goods> cached = listCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }
        List<Goods> list = goodsMapper.findList(categoryId, kw, minPrice, maxPrice, sellerId, sort, p[2], p[1], location,
                canMail, canDeliver, exchangeSupported);
        int total = goodsMapper.count(categoryId, kw, minPrice, maxPrice, sellerId, location,
                canMail, canDeliver, exchangeSupported);
        // enrich 校园专属标签
        campusFeatureService.enrichGoods(list, studentNo);
        PageResult<Goods> result = PageResult.of(list, total, p[0], p[1]);
        if (cacheKey != null) {
            listCache.put(cacheKey, result);
        }
        return result;
    }

    // 分页查询我发布的商品
    public PageResult<Goods> findMyGoods(Integer sellerId, Integer pageNum, Integer pageSize) {
        int[] p = PageResult.normalize(pageNum, pageSize, 50);
        List<Goods> list = goodsMapper.findBySellerIdPage(sellerId, p[2], p[1]);
        int total = goodsMapper.countBySellerId(sellerId);
        return PageResult.of(list, total, p[0], p[1]);
    }

    // 修改商品
    public String update(Goods goods) {
        applyRaiseCouponPricing(goods);
        goodsMapper.update(goods);
        listCache.clear();
        return "修改成功";
    }

    // 修改状态（上下架；卖家仅能在 on_sale / off_shelf 间切换，sold 由订单流程驱动）
    public String updateStatus(Integer id, String status) {
        if (!"on_sale".equals(status) && !"off_shelf".equals(status)) {
            return "非法的商品状态";
        }
        goodsMapper.updateStatus(id, status);
        listCache.clear();
        return "状态更新成功";
    }

    // 删除商品
    public String delete(Integer id) {
        goodsMapper.delete(id);
        listCache.clear();
        return "删除成功";
    }

    /**
     * 清洗搜索关键词：去掉直接传给全文检索 BOOLEAN MODE 会被当作语法的字符，
     * 清洗后为空则视为不带关键词。
     */
    private String normalizeSearchKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String kw = keyword.replaceAll("[+\\-*\"~<>()@]", " ").trim();
        return kw.isEmpty() ? null : kw;
    }
}
