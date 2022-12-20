package com.zjq.feeds.mapper;


import com.zjq.commons.model.pojo.Feeds;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Set;

/**
 * Feed逻辑Mapper层
 * @author zjq
 */
public interface FeedsMapper {

    /**
     * 添加 Feed
     * @param feeds
     * @return
     */
    @Insert("insert into t_feeds (content, fk_user_id, praise_amount, " +
            " comment_amount, fk_restaurant_id, create_date, update_date, is_valid) " +
            " values (#{content}, #{fkUserId}, #{praiseAmount}, #{commentAmount}, #{fkRestaurantId}, " +
            " now(), now(), 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(Feeds feeds);

    /**
     * 查询 Feed
     * @param id
     * @return
     */
    @Select("select id, content, fk_user_id, praise_amount, " +
            " comment_amount, fk_restaurant_id, create_date, update_date, is_valid " +
            " from t_feeds where id = #{id} and is_valid = 1")
    Feeds findById(@Param("id") Integer id);

    /**
     * 逻辑删除 Feed
     * @param id
     * @return
     */
    @Update("update t_feeds set is_valid = 0 where id = #{id} and is_valid = 1")
    int delete(@Param("id") Integer id);

    /**
     * 根据用户 ID 查询 Feed
     * @param userId
     * @return
     */
    @Select("select id, content, update_date from t_feeds " +
            " where fk_user_id = #{userId} and is_valid = 1")
    List<Feeds> findByUserId(@Param("userId") Integer userId);

    /**
     * 根据多主键查询 Feed
     * @param feedIds
     * @return
     */
    @Select("<script> " +
            " select id, content, fk_user_id, praise_amount, " +
            " comment_amount, fk_restaurant_id, create_date, update_date, is_valid " +
            " from t_feeds where is_valid = 1 and id in " +
            " <foreach item=\"id\" collection=\"feedIds\" open=\"(\" separator=\",\" close=\")\">" +
            "   #{id}" +
            " </foreach> order by id desc" +
            " </script>")
    List<Feeds> findFeedsByIds(@Param("feedIds") Set<Integer> feedIds);

}
