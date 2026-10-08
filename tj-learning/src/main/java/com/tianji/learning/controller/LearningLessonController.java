package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.service.ILeaningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/lessons")
@Api(tags = "我的课程相关接口")
@RequiredArgsConstructor
@Slf4j
public class LearningLessonController {
    private final ILeaningLessonService lessonService;
    @GetMapping("/page")
    @ApiOperation("分页查询我的课表")
    public PageDTO<LearningLessonVO> page(PageQuery query) {
        return lessonService.queryMyLessons(query);
    }
    @GetMapping("/now")
    @ApiOperation("查询最近正在学习的课程")
    public LearningLessonVO queryMyCurrentLesson() {
        log.info("查询最近正在学习的课程");
        return lessonService.queryMyCurrentLesson();
    }
    @GetMapping("/{courseId}")
    @ApiOperation("根据id查询指定课程学习状态")
    public LearningLessonVO queryLessonByCourseId( @ApiParam(value = "课程id" ,example = "1")@PathVariable Integer courseId) {
        return lessonService.queryLessonByCourseId(courseId);
    }
    @DeleteMapping("/{courseId}")
    @ApiOperation("删除课表中的某课程")
    public void deleteByCourseId(@PathVariable Long courseId) {
        lessonService.deleteByCourseId(null,courseId);
    }
    @GetMapping("/{courseId}/valid")
    @ApiOperation("校验指定课程是否是课表的有效课程")
    public Long isLessonValid(@PathVariable Long courseId){
        return lessonService.isLessonValid(courseId);
    }
    @GetMapping("/lessons/{courseId}/count")
    Integer countLearningLessonByCourse(@PathVariable("courseId") Long courseId){
        return lessonService.countLearningLessonByCourse(courseId);
    }
    @PostMapping("/plans")
    @ApiOperation("创建学习计划")
    public void createLearningPlans(@Valid @RequestBody LearningPlanDTO planDTO){
        lessonService.createLearningPlans(planDTO);
    }
    @ApiOperation("查询我的学习计划")
    @GetMapping("/plans")
    public LearningPlanPageVO queryMyPlans(PageQuery pageQuery){
        return lessonService.queryMyPlans(pageQuery);
    }
}
