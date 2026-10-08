package com.tianji.learning.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tianji.learning.domain.enums.LessonStatus;
import com.tianji.learning.domain.enums.PlanStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("learning_lesson")
//implements Serializable 就是让这个类的对象可以被打包成字节流，用来：
//
//存到 Redis
//
//在微服务之间传输
//
//缓存到磁盘
public class LearningLesson implements Serializable {
    private static final long serialVersionUID = 1L;
    //主键
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    //学员id
    private Long userId;
    //课程id
    private Long courseId;
    //课程状态，0-未学习，1-学习中，2-已学完，3-已失效
    private LessonStatus status;
    //每周学习频率
    private Integer weekFreq;
    //计划状态，0-没有计划，1-计划进行中
    private PlanStatus planStatus;
    //已学习小节数量
    private Integer learnedSections;
    //最后一次学习的小节id
    private Long latestSectionId;
    //最后一次学习时间
    private LocalDateTime latestLearnTime;
    //创建时间
    private LocalDateTime createTime;
    //过期时间
    private LocalDateTime expireTime;
    //更新 时间
    private LocalDateTime updateTime;
}
