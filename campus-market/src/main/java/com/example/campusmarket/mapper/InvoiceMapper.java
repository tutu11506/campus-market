package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.Invoice;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InvoiceMapper {

    @Select("SELECT * FROM invoice WHERE order_id = #{orderId} LIMIT 1")
    Invoice findByOrderId(@Param("orderId") Integer orderId);

    @Select("SELECT i.*, g.title AS goodsTitle FROM invoice i " +
            "LEFT JOIN `order` o ON i.order_id = o.id " +
            "LEFT JOIN goods g ON o.goods_id = g.id WHERE i.id = #{id}")
    Invoice findDetailById(@Param("id") Integer id);

    @Select("SELECT i.*, g.title AS goodsTitle FROM invoice i " +
            "LEFT JOIN `order` o ON i.order_id = o.id " +
            "LEFT JOIN goods g ON o.goods_id = g.id " +
            "WHERE i.user_id = #{uid} ORDER BY i.create_time DESC LIMIT #{offset}, #{pageSize}")
    List<Invoice> findByUser(@Param("uid") Integer uid,
                             @Param("offset") int offset,
                             @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM invoice WHERE user_id = #{uid}")
    int countByUser(@Param("uid") Integer uid);

    @Insert("INSERT INTO invoice(order_id, order_no, user_id, title_type, title, tax_no, email, amount, " +
            "invoice_no, status, issue_time) VALUES(#{orderId}, #{orderNo}, #{userId}, #{titleType}, #{title}, " +
            "#{taxNo}, #{email}, #{amount}, #{invoiceNo}, #{status}, #{issueTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Invoice invoice);
}
