package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.PointExchange;
import com.example.campusmarket.entity.PointReward;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RewardMapper {

    /* ==================== PointReward ==================== */

    @Select("SELECT * FROM point_reward WHERE status = 1 ORDER BY sort_order ASC, id ASC")
    List<PointReward> listActive();

    @Select("SELECT * FROM point_reward WHERE id = #{id}")
    PointReward findById(Integer id);

    @Update("UPDATE point_reward SET total_redeemed = total_redeemed + 1 " +
            "WHERE id = #{id} AND (stock IS NULL OR stock - total_redeemed > 0)")
    int consumeStock(Integer id);

    /* ==================== PointExchange ==================== */

    @Insert("INSERT INTO point_exchange(user_id, reward_id, reward_snapshot, points_cost) " +
            "VALUES(#{userId}, #{rewardId}, #{rewardSnapshot}, #{pointsCost})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertExchange(PointExchange e);

    @Select("SELECT e.*, r.name AS reward_name, r.icon AS reward_icon, r.type AS reward_type, u.username " +
            "FROM point_exchange e LEFT JOIN point_reward r ON r.id = e.reward_id " +
            "LEFT JOIN user u ON u.id = e.user_id " +
            "WHERE e.user_id = #{userId} ORDER BY e.created_at DESC LIMIT #{offset}, #{pageSize}")
    List<PointExchange> findMyExchanges(@Param("userId") Integer userId, @Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM point_exchange WHERE user_id = #{userId}")
    long countMyExchanges(Integer userId);
}
