package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.TextbookMaterial;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface TextbookMaterialMapper {

    @Select("SELECT m.*, u.username AS uploader_name " +
            "FROM textbook_material m JOIN user u ON m.uploader_id = u.id " +
            "WHERE m.owner_type = #{ownerType} AND m.owner_id = #{ownerId} AND m.status = 1 " +
            "ORDER BY CASE m.delivery_mode WHEN 'free' THEN 0 WHEN 'bonus' THEN 1 ELSE 2 END, m.create_time DESC")
    List<TextbookMaterial> findByOwner(@Param("ownerType") String ownerType,
                                       @Param("ownerId") Integer ownerId);

    @Select("SELECT COUNT(*) FROM textbook_material WHERE owner_type = #{ownerType} AND owner_id = #{ownerId} AND status = 1")
    int countByOwner(@Param("ownerType") String ownerType, @Param("ownerId") Integer ownerId);

    @Select("SELECT * FROM textbook_material WHERE id = #{id} AND status = 1")
    TextbookMaterial findById(Integer id);

    @Select("SELECT * FROM textbook_material WHERE owner_type = #{ownerType} AND owner_id = #{ownerId} " +
            "AND delivery_mode = 'bonus' AND status = 1")
    List<TextbookMaterial> findBonusByOwner(@Param("ownerType") String ownerType, @Param("ownerId") Integer ownerId);

    @Insert("INSERT INTO textbook_material(owner_type, owner_id, uploader_id, title, description, " +
            "file_type, file_url, file_size, delivery_mode, price) " +
            "VALUES(#{ownerType}, #{ownerId}, #{uploaderId}, #{title}, #{description}, " +
            "#{fileType}, #{fileUrl}, #{fileSize}, #{deliveryMode}, #{price})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TextbookMaterial material);

    @Update("UPDATE textbook_material SET views = views + 1 WHERE id = #{id}")
    int incrViews(Integer id);

    @Update("UPDATE textbook_material SET downloads = downloads + 1 WHERE id = #{id}")
    int incrDownloads(Integer id);

    @Update("UPDATE textbook_material SET status = 0 WHERE id = #{id} AND uploader_id = #{uid}")
    int softDelete(@Param("id") Integer id, @Param("uid") Integer uid);

    @Select("SELECT m.* FROM textbook_material m WHERE m.uploader_id = #{uid} " +
            "ORDER BY m.create_time DESC LIMIT #{offset}, #{pageSize}")
    List<TextbookMaterial> findMyUploads(@Param("uid") Integer uid,
                                         @Param("offset") int offset,
                                         @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM textbook_material WHERE uploader_id = #{uid}")
    long countMyUploads(Integer uid);
}
