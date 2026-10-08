package com.tianji.learning.handler;

import com.tianji.learning.service.IPointsBoardSeasonService;
import com.tianji.learning.service.IPointsBoardService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PointsBoardPersistentHandler {
    private final IPointsBoardSeasonService seasonService;
    private final IPointsBoardService pointsBoardService;
    @XxlJob("createTableJob")
    public void createPointsBoardTableOfLastSeason(){
        //获取上个月时间
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        //查询赛季id
        Integer season=seasonService.querySeasonByTime(time);
        if (season==null){
            return;
        }
        //创建表
        pointsBoardService.createPointsBoardTableBySeason(season);
    }
}
