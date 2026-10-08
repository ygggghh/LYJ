package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.enums.PointsRecordType;
import com.tianji.learning.domain.po.PointsRecord;
import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.mapper.PointsRecordMapper;
import com.tianji.learning.service.IPointsRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-08-28
 */
@Service
@RequiredArgsConstructor
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements IPointsRecordService {
    private final StringRedisTemplate redisTemplate;

    @Override
    public void addPointsRecord(Long userId, int points, PointsRecordType type) {
        int maxPoints=type.getMaxPoints();
        int realPoints=points;
        LocalDateTime now=LocalDateTime.now();
        //看是否有上限
        if (maxPoints>0){
            LocalDateTime begin= DateUtils.getDayStartTime(now);
            LocalDateTime end=DateUtils.getDayEndTime(now);
            //查询用户以这种方式获得了多少积分了
            int currentPoints=queryUserPointsByTypeAndDate(userId,type,begin,end);
            //看一下是否上限
            if (currentPoints>=maxPoints){
                return;
            }
            if (currentPoints+points>maxPoints){
                realPoints=maxPoints-currentPoints;
            }
        }
        PointsRecord p=new PointsRecord();
        p.setPoints(realPoints);
        p.setUserId(userId);
        p.setType(type);
        save(p);
        //累加积分
        String key= RedisConstants.POINTS_BOARD_KEY_PREFIX+now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        redisTemplate.opsForZSet().incrementScore(key,userId.toString(),realPoints);
    }

    private int queryUserPointsByTypeAndDate(Long userId, PointsRecordType type, LocalDateTime begin, LocalDateTime end) {
        QueryWrapper<PointsRecord>wrapper=new QueryWrapper<>();
        wrapper.lambda()
                .eq(PointsRecord::getUserId,userId)
                .eq(type!=null,PointsRecord::getType,type)
                .between(begin!=null&&end!=null,PointsRecord::getCreateTime,begin,end);
        Integer points=getBaseMapper().queryUserPointsByTypeAndDate(wrapper);
        return points==null?0:points;
    }

    @Override
    public List<PointsStatisticsVO> queryMyPointsToday() {
        Long userId = UserContext.getUser();
        LocalDateTime now=LocalDateTime.now();
        LocalDateTime begin = DateUtils.getDayStartTime(now);
        LocalDateTime end = DateUtils.getDayEndTime(now);
        QueryWrapper<PointsRecord>wrapper=new QueryWrapper<>();
        wrapper.lambda()
                .eq(PointsRecord::getUserId,userId)
                .between(PointsRecord::getCreateTime,begin,end);
        List<PointsRecord>list=getBaseMapper().queryUserPointsByDate(wrapper);
        if (CollUtils.isEmpty(list)){
            return null;
        }
        List<PointsStatisticsVO>vos=new ArrayList<>(list.size());
        for (PointsRecord p : list) {
            PointsStatisticsVO vo=new PointsStatisticsVO();
            vo.setType(p.getType().getDesc());
            vo.setMaxPoints(p.getType().getMaxPoints());
            vo.setPoints(p.getPoints());
            vos.add(vo);
        }
        return vos;
    }
}
