package com.tianji.learning.service.impl;

import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BooleanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.vo.SignResultVO;
import com.tianji.learning.mq.message.SignInMessage;
import com.tianji.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SignRecordServiceImpl implements ISignRecordService {
    private final StringRedisTemplate redisTemplate;
    private final RabbitMqHelper mqHelper;
    @Override
    public SignResultVO addSignRecords() {
        //1.签到
        //1.1获取用户信息
        Long userId = UserContext.getUser();
        //1.2获取当前日期
        LocalDateTime now = LocalDateTime.now();
        //1.3拼接key
        String key= RedisConstants.SIGN_RECORD_KEY_PREFIX+userId+now.format(DateUtils.SIGN_DATE_SUFFIX_FORMATTER);
        //1.4计算offset,也就是计算下标，如果今天是1号，那对于redis的这个结果下标就是0
        int offset=now.getDayOfMonth()-1;
        //1.5保存签到信息,也就是给那个下标的值设置成1，他会返回一个值，如果他原本是0，给他设置成1后会返回0，如果他原本是1
        //给他设置1后会返回1，因此可以用来防止重复签到
        Boolean exits = redisTemplate.opsForValue().setBit(key, offset, true);
        if (BooleanUtils.isTrue(exits)){
            throw new BizIllegalException("不允许重复签到");
        }
        //2.计算连续签到天数
        int signDays=countSignDays(key,now.getDayOfMonth());
        //3.算积分
        int rewardPoints=0;
        switch (signDays){
            case 7:rewardPoints=10;
            break;
            case 14:rewardPoints=20;
            break;
            case 28:rewardPoints=40;
            break;
        }
        mqHelper.send(
                MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.SIGN_IN,
                SignInMessage.of(userId,rewardPoints+1)
        );
        SignResultVO vo=new SignResultVO();
        vo.setSignDays(signDays);
        vo.setRewardPoints(rewardPoints);
        return vo;
    }

    private int countSignDays(String key, int len) {
        //1.获取本月第一天到今天为止的所有签到记录
        List<Long> result = redisTemplate.opsForValue()
                .bitField(key, BitFieldSubCommands.create().get(
                        BitFieldSubCommands.BitFieldType.unsigned(len)).valueAt(0));
        if (CollUtils.isEmpty(result)){
            return 0;
        }
        int num=result.get(0).intValue();
        int count=0;
        while ((num&1)==1){
            count++;
            num>>>=1;
        }
        return count;
    }
}
