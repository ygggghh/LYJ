package com.tianji.learning.controller;


import com.tianji.learning.domain.vo.PointsStatisticsVO;
import com.tianji.learning.service.IPointsRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 学习积分记录，每个月底清零 前端控制器
 * </p>
 *
 * @author author
 * @since 2026-08-28
 */
@RestController
@RequestMapping("/points")
@Api(tags = "积分相关接口")
@RequiredArgsConstructor
public class PointsRecordController {
    private final IPointsRecordService recordService;
    @GetMapping("today")
    @ApiOperation("查询我的今日积分情况")
    public List<PointsStatisticsVO>queryMyPointsToday(){
        return recordService.queryMyPointsToday();
    }
}
