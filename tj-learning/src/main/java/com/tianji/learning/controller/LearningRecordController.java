package com.tianji.learning.controller;


import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.service.ILearningRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 学习记录表 前端控制器
 * </p>
 *
 * @author author
 * @since 2026-08-07
 */
@RestController
@RequestMapping("/learning-records")
@Api(tags = "学习记录接口")
@RequiredArgsConstructor
public class LearningRecordController {
    private final ILearningRecordService learningRecordService;
    @GetMapping("/course/{courseId}")
    @ApiOperation("查询当前用户指定课程的学习进度")
    LearningLessonDTO queryLearningRecordByCourse(@PathVariable("courseId") Long courseId){
        return learningRecordService.queryLearningRecordByCourse(courseId);
    }
    @PostMapping
    public void addLearningRecord(@RequestBody LearningRecordFormDTO formDTO) {
        learningRecordService.addLearningRecord(formDTO);
    }
}
