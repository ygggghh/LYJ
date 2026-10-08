package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tianji.api.dto.remark.LikeTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

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
//@Service
@RequiredArgsConstructor
public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {
    private final RabbitMqHelper mqHelper;

    @Override
    public void addLikeRecord(LikeRecordFormDTO recordDTO) {
        //基于前端传过来的参数判断是点赞还是取消点赞
        boolean success=recordDTO.getLiked()?like(recordDTO):unlike(recordDTO);
        //如果执行失败直接返回
        if (!success){
            return;
        }
        //如果执行成功，统计点赞总数
        Integer likeTimes=lambdaQuery()
                .eq(LikedRecord::getBizId,recordDTO.getBizId())
                .count();
        //向MQ发送消息，包括是那个回答或评论的点赞以及点赞的总数,到时其他业务收到的时候可以知道是哪个评论或回答
        mqHelper.send(LIKE_RECORD_EXCHANGE,
                //这里用了一个组装的办法，根据recordDTO.getBizType()变成不同的KEY，到时有用
                StringUtils.format(LIKED_TIMES_KEY_TEMPLATE,recordDTO.getBizType())
        , LikeTimesDTO.of(recordDTO.getBizId(),likeTimes));
    }

    private boolean unlike(LikeRecordFormDTO recordDTO) {
        return remove(new QueryWrapper<LikedRecord>().lambda()
                .eq(LikedRecord::getUserId, UserContext.getUser())
                .eq(LikedRecord::getBizId, recordDTO.getBizId()));
    }

    private boolean like(LikeRecordFormDTO recordDTO) {
        Long userId= UserContext.getUser();
        Integer count = lambdaQuery()
                .eq(LikedRecord::getUserId, userId)
                .eq(LikedRecord::getBizId, recordDTO.getBizId())
                .count();
        //如果数据库有点赞过的记录，直接结束
        if (count>0){
            return false;
        }
        LikedRecord record=new LikedRecord();
        record.setUserId(userId);
        record.setBizId(recordDTO.getBizId());
        record.setBizType(recordDTO.getBizType());
        save(record);
        return true;
    }

    @Override
    public Set<Long> isBizLiked(List<Long> bizIds) {
        Long user = UserContext.getUser();
        List<LikedRecord> list = lambdaQuery()
                .in(LikedRecord::getBizId, bizIds)
                .eq(LikedRecord::getUserId, user)
                .list();
        return list.stream().map(LikedRecord::getBizId).collect(Collectors.toSet());
    }

    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {

    }
}
