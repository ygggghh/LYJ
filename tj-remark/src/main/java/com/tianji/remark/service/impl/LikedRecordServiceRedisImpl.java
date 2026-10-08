package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.remark.LikeTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.constants.RedisConstants;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE;

/**
 * <p>
 * 点赞记录表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-08-18
 */
@Service
@RequiredArgsConstructor
public class LikedRecordServiceRedisImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {
    private final RabbitMqHelper mqHelper;
    private final StringRedisTemplate redisTemplate;
    @Override
    public void addLikeRecord(LikeRecordFormDTO recordDTO) {
        //基于前端传过来的参数判断是点赞还是取消点赞
        boolean success=recordDTO.getLiked()?like(recordDTO):unlike(recordDTO);
        //如果执行失败直接返回
        if (!success){
            return;
        }
        //如果执行成功，统计点赞总数
        Long likeTimes=redisTemplate.opsForSet().size(RedisConstants.LIKES_BIZ_KEY_PREFIX+recordDTO.getBizId());
        if (likeTimes==null){
            return;
        }
        //缓存点赞总数到redis
        redisTemplate.opsForZSet().add(RedisConstants.LIKES_TIMES_KEY_PREFIX+recordDTO.getBizType()
        ,recordDTO.getBizId().toString()
        ,likeTimes);

    }

    private boolean unlike(LikeRecordFormDTO recordDTO) {
        Long userId= UserContext.getUser();
        //拼接key
        String key= RedisConstants.LIKES_BIZ_KEY_PREFIX+recordDTO.getBizId();
        //用redis的删除命令
        Long result = redisTemplate.opsForSet().remove(key, userId.toString());
        return result!=null&& result>0;
    }

    private boolean like(LikeRecordFormDTO recordDTO) {
        Long userId= UserContext.getUser();
        //拼接key
        String key= RedisConstants.LIKES_BIZ_KEY_PREFIX+recordDTO.getBizId();
        //用redis的新增命令
        Long result = redisTemplate.opsForSet().add(key, userId.toString());
        return result!=null&& result>0;
    }

    @Override
    public Set<Long> isBizLiked(List<Long> bizIds) {
        Long user = UserContext.getUser();
        List<Object> objects = redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                //因为只能用他给的RedisConnection connection，因此要把他转个类型，告诉他要发送的这些key是string类型
                StringRedisConnection src = (StringRedisConnection) connection;
                for (Long bizId : bizIds) {
                    String key = RedisConstants.LIKES_BIZ_KEY_PREFIX + bizId;
                    src.sIsMember(key, user.toString());
                }
                return null;
            }
        });
        //对返回的结果处理，如果是true就说明点赞记录存在也就是点赞过，放入set集合
        Set<Long>set=new HashSet<>();
        for (int i=0;i<objects.size();i++) {
            Boolean o = (Boolean) objects.get(i);
            if (o){
                set.add(bizIds.get(i));
            }
        }
        return set;
    }

    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {
        //读取并移除redis的点赞总数
        String key=RedisConstants.LIKES_TIMES_KEY_PREFIX+bizType;
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().popMin(key, maxBizSize);
        if (CollUtils.isEmpty(tuples)){
            return;
        }
        //数据转换
        List<LikeTimesDTO>list=new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String bizId=tuple.getValue();
            Double likedTimes=tuple.getScore();
            if (bizId==null||likedTimes==null){
                continue;
            }
            list.add(LikeTimesDTO.of(Long.valueOf(bizId),likedTimes.intValue()));
        }
        //发送mq消息
        mqHelper.send(LIKE_RECORD_EXCHANGE,
                StringUtils.format(LIKED_TIMES_KEY_TEMPLATE,bizType)
                ,list);
    }
}
