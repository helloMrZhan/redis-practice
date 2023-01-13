package com.zjq.points.mapper;

import com.zjq.commons.model.pojo.UserPoints;
import com.zjq.commons.model.vo.UserPointsRankVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 积分 Mapper
 * @author zjq
 */
public interface UserPointsMapper {

    /**
     * 添加积分
     * @param userPoints  用户积分实体
     */
    @Insert("insert into t_user_points (fk_user_id, points, types, is_valid, create_date, update_date) " +
            " values (#{fkUserId}, #{points}, #{types}, 1, now(), now())")
    void save(UserPoints userPoints);

    /**
     * 查询积分排行榜 TOPN
     * @param top  前多少名
     * @return 排行榜集合
     */
    @Select("SELECT t1.fk_user_id AS id, " +
            " sum( t1.points ) AS total, " +
            " rank () over ( ORDER BY sum( t1.points ) DESC ) AS ranks," +
            " t2.nickname, t2.avatar_url " +
            " FROM t_user_points t1 LEFT JOIN t_users t2 ON t1.fk_user_id = t2.id " +
            " WHERE t1.is_valid = 1 AND t2.is_valid = 1 " +
            " GROUP BY t1.fk_user_id " +
            " ORDER BY total DESC LIMIT #{top}")
    List<UserPointsRankVO> findTopN(@Param("top") int top);


    /**
     * 根据用户 ID 查询当前用户的积分排名
     * @param userId 用户id
     * @return 用户积分实体
     */
    @Select("SELECT id, total, ranks, nickname, avatar_url FROM (" +
            " SELECT t1.fk_user_id AS id, " +
            " sum( t1.points ) AS total, " +
            " rank () over ( ORDER BY sum( t1.points ) DESC ) AS ranks," +
            " t2.nickname, t2.avatar_url " +
            " FROM t_user_points t1 LEFT JOIN t_users t2 ON t1.fk_user_id = t2.id " +
            " WHERE t1.is_valid = 1 AND t2.is_valid = 1 " +
            " GROUP BY t1.fk_user_id " +
            " ORDER BY total DESC ) r " +
            " WHERE id = #{userId}")
    UserPointsRankVO findUserRank(@Param("userId") int userId);

}
