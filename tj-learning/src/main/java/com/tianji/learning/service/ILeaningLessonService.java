package com.tianji.learning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;

import javax.validation.Valid;
import java.util.List;

public interface ILeaningLessonService extends IService<LearningLesson> {
    void addUserLessons(Long userId, List<Long> courseIds);

    PageDTO<LearningLessonVO> queryMyLessons(PageQuery query);


    LearningLessonVO queryMyCurrentLesson();

    LearningLessonVO queryLessonByCourseId(Integer courseId);

    void deleteByCourseId(Long userId,Long courseId);

    Long isLessonValid(Long courseId);

    Integer countLearningLessonByCourse(Long courseId);

    LearningLesson queryByUserIdAndCourseId(Long userId, Long courseId);

    void createLearningPlans(@Valid LearningPlanDTO planDTO);

    LearningPlanPageVO queryMyPlans(PageQuery pageQuery);
}
