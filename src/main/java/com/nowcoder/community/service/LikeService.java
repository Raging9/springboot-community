package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;


    // 点赞
    public void like(int userId, int entityType, int entityId, int entityUserId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);

                //判断登录用户是否点赞了
                boolean isMember = operations.opsForSet().isMember(entityLikeKey, userId);

                //事物
                operations.multi();

                if (isMember) {
                    //如果点赞了
                    //取消点赞该实体
                    operations.opsForSet().remove(entityLikeKey, userId);
                    //同时该作者减少一个获赞数
                    operations.opsForValue().decrement(userLikeKey);
                } else {
                    //没点赞的话
                    //点赞该实体
                    operations.opsForSet().add(entityLikeKey, userId);
                    //同时该作者增加一个获赞数
                    operations.opsForValue().increment(userLikeKey);
                }
                return operations.exec();
            }
        });
    }

    // 查询某实体获赞的总数
    public long findEntityLikeCount(int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    // 查询某个用户获赞的总数
    public int findUserLikeCount(int userId) {
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        return count == null ? 0 : count.intValue();
    }

    // 查询某人对某实体的点赞状态
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
    }

}
