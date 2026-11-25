package com.example.campusmarket.mapper;

import com.example.campusmarket.entity.Message;
import com.example.campusmarket.vo.ConversationVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface MessageMapper {

    // 发送消息
    @Insert("INSERT INTO message(sender_id, receiver_id, content, msg_type, file_name, file_size, goods_id, auto_reply) " +
            "VALUES(#{senderId}, #{receiverId}, #{content}, #{msgType}, #{fileName}, #{fileSize}, #{goodsId}, " +
            "IFNULL(#{autoReply}, 0))")
    int insert(Message message);

    // 查询和某人的聊天记录
    @Select("SELECT m.*, u.username as sender_name, u.avatar as sender_avatar, " +
            "g.title as goods_title, g.cover_image as goods_cover, g.price as goods_price " +
            "FROM message m " +
            "LEFT JOIN user u ON m.sender_id = u.id " +
            "LEFT JOIN goods g ON m.goods_id = g.id " +
            "WHERE (m.sender_id = #{userId} AND m.receiver_id = #{otherId}) " +
            "   OR (m.sender_id = #{otherId} AND m.receiver_id = #{userId}) " +
            "ORDER BY m.create_time ASC")
    List<Message> findChatRecord(@Param("userId") Integer userId, @Param("otherId") Integer otherId);

    // 分页查询聊天记录：按时间倒序取最新一页，前端再反转成正序展示，便于“加载更多历史”
    @Select("SELECT m.*, u.username as sender_name, u.avatar as sender_avatar, " +
            "g.title as goods_title, g.cover_image as goods_cover, g.price as goods_price " +
            "FROM message m " +
            "LEFT JOIN user u ON m.sender_id = u.id " +
            "LEFT JOIN goods g ON m.goods_id = g.id " +
            "WHERE (m.sender_id = #{userId} AND m.receiver_id = #{otherId}) " +
            "   OR (m.sender_id = #{otherId} AND m.receiver_id = #{userId}) " +
            "ORDER BY m.create_time DESC LIMIT #{offset}, #{size}")
    List<Message> findChatRecordPage(@Param("userId") Integer userId,
                                     @Param("otherId") Integer otherId,
                                     @Param("offset") Integer offset,
                                     @Param("size") Integer size);

    // 统计和某人的聊天记录总数
    @Select("SELECT COUNT(*) FROM message " +
            "WHERE (sender_id = #{userId} AND receiver_id = #{otherId}) " +
            "   OR (sender_id = #{otherId} AND receiver_id = #{userId})")
    int countChatRecord(@Param("userId") Integer userId, @Param("otherId") Integer otherId);

    // 查询我的会话列表（和哪些人聊过天，取最新一条 + 未读数）
    // 性能优化：用 UNION ALL 分别走 sender_id / (receiver_id,is_read) 索引，单次聚合同步算出未读数，
    // 消除旧写法“每个会话一行相关子查询（N+1）+ GROUP BY 表达式全表扫 + filesort”的问题，
    // 数据量增大后耗时随单用户消息量伸缩，而不是随全表大小增长。
    @Select("SELECT " +
            "  agg.other_id, " +
            "  u.username AS other_name, " +
            "  u.avatar AS other_avatar, " +
            "  m.content AS last_content, " +
            "  m.msg_type AS msg_type, " +
            "  m.create_time AS last_time, " +
            "  agg.unread_count, " +
            "  m.goods_id AS goods_id, " +
            "  g.title AS goods_title, " +
            "  g.cover_image AS goods_cover " +
            "FROM ( " +
            "  SELECT other_id, MAX(id) AS last_id, SUM(unread) AS unread_count FROM ( " +
            "    SELECT receiver_id AS other_id, id, 0 AS unread FROM message WHERE sender_id = #{userId} " +
            "    UNION ALL " +
            "    SELECT sender_id AS other_id, id, (CASE WHEN is_read = 0 THEN 1 ELSE 0 END) AS unread FROM message WHERE receiver_id = #{userId} " +
            "  ) t GROUP BY other_id " +
            ") agg " +
            "JOIN message m ON m.id = agg.last_id " +
            "LEFT JOIN user u ON u.id = agg.other_id " +
            "LEFT JOIN goods g ON g.id = m.goods_id " +
            "ORDER BY m.create_time DESC")
    List<ConversationVO> findConversationList(Integer userId);

    // 标记消息为已读
    @Update("UPDATE message SET is_read = 1 " +
            "WHERE sender_id = #{otherId} AND receiver_id = #{userId} AND is_read = 0")
    int markAsRead(@Param("userId") Integer userId, @Param("otherId") Integer otherId);

    // 查询未读消息数量
    @Select("SELECT COUNT(*) FROM message WHERE receiver_id = #{userId} AND is_read = 0")
    int countUnread(Integer userId);

    /**
     * 查询需要机器人自动回复的消息：
     * - 对方超过 waitSeconds 秒没有回复（会话里没有对方的真实回复）
     * - 该会话还没有发过自动回复，避免反复打扰
     * - 只取每个方向最新的一条，防止一次补发多条
     * - 只处理最近 recentHours 小时内的消息，避免服务启动时给历史会话群发
     */
    @Select("SELECT m.* FROM message m " +
            "WHERE m.auto_reply = 0 " +
            "  AND m.create_time <= DATE_SUB(NOW(), INTERVAL #{waitSeconds} SECOND) " +
            "  AND m.create_time >= DATE_SUB(NOW(), INTERVAL #{recentHours} HOUR) " +
            "  AND NOT EXISTS (SELECT 1 FROM message r WHERE r.sender_id = m.receiver_id " +
            "       AND r.receiver_id = m.sender_id AND r.auto_reply = 0) " +
            "  AND NOT EXISTS (SELECT 1 FROM message a WHERE a.sender_id = m.receiver_id " +
            "       AND a.receiver_id = m.sender_id AND a.auto_reply = 1) " +
            "  AND NOT EXISTS (SELECT 1 FROM message n WHERE n.sender_id = m.sender_id " +
            "       AND n.receiver_id = m.receiver_id AND n.create_time > m.create_time AND n.auto_reply = 0) " +
            "ORDER BY m.create_time ASC LIMIT 20")
    List<Message> findPendingAutoReply(@Param("waitSeconds") int waitSeconds,
                                       @Param("recentHours") int recentHours);
}