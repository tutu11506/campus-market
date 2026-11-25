-- 商品搜索：为 title + description 建 ngram 全文索引，
-- 让关键词搜索走索引而不是 LIKE '%kw%' 全表扫描。
-- ngram 分词器对中文按 2 字切词，因此关键词长度 < 2 时仍回退 LIKE（见 GoodsMapper）。
ALTER TABLE `goods` ADD FULLTEXT INDEX `ft_goods_search` (`title`, `description`) WITH PARSER ngram;
