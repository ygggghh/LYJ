package com.tianji.learning.mq;

import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.service.ILeaningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@RequiredArgsConstructor
public class LessonChangeListener {
    private final ILeaningLessonService lessonService;
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "learning.lesson.pay.queue",durable = "true"),
            exchange = @Exchange(value = MqConstants.Exchange.ORDER_EXCHANGE,type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_PAY_KEY
    ))
    public void ListenLessonPay(OrderBasicDTO order){
        if (order== null||order.getUserId()==null|| CollUtils.isEmpty(order.getCourseIds())){
            log.error("接收MQ消息有误，订单数据为空");
            return;
        }
        log.debug("用户{}的课程{}支付成功",order.getUserId(),order.getCourseIds());
        lessonService.addUserLessons(order.getUserId(),order.getCourseIds());
    }
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "learning.lesson.refund.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.ORDER_EXCHANGE,
                    type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_REFUND_KEY
    ))
    public void ListenLessonRefund(OrderBasicDTO order){
        if (order==null||order.getUserId()==null||CollUtils.isEmpty(order.getCourseIds())){
            log.error("接收MQ消息有误，订单数据为空");
            return;
        }
        log.debug("用户{}的课程{}退款成功",order.getUserId(),order.getCourseIds());
        lessonService.deleteByCourseId(order.getUserId(),order.getCourseIds().get(0));
    }
}
