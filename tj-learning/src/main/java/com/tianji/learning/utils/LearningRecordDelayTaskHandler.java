package com.tianji.learning.utils;

import com.tianji.common.utils.JsonUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILeaningLessonService;
import groovy.util.logging.Slf4j;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.DelayQueue;

@lombok.extern.slf4j.Slf4j
@Data
@Slf4j
@RequiredArgsConstructor
@Component
public class LearningRecordDelayTaskHandler {
    private final StringRedisTemplate redisTemplate;
    private final DelayQueue<DelayTask<RecordTaskData>>queue=new DelayQueue<>();
    private final LearningRecordMapper recordMapper;
    private final ILeaningLessonService lessonService;
    private final static String RECORD_KEY_TEMPLATE="learning:record:{}";
    private static volatile boolean begin=true;
    public void addLearningRecordTask(LearningRecord record){
        //写入缓存
        writeRecordCache(record);
        //提交延迟任务到延迟队列
        queue.add(new DelayTask<>(new RecordTaskData(record),Duration.ofSeconds(20)));
    }
    @PostConstruct
    public void init(){
        CompletableFuture.runAsync(this::handleDelayTask);
    }
    public void destroy(){
        begin=false;
        log.debug("延迟任务停止");
    }
    //从延迟队列取出数据处理
    public void handleDelayTask(){
        try {//获取延迟任务
            while (begin){
                DelayTask<RecordTaskData> task = queue.take();
                RecordTaskData data=task.getData();
                //查询redis缓存
                LearningRecord record = readRecordCache(data.lessonId, data.sectionId);
                if (record==null){
                    continue;
                }
                //比较moment值，如果不一样就continue
                if (!Objects.equals(data.getMoment(),record.getMoment())){
                    continue;
                }
                //一样时更新redis和数据库表
                record.setFinished(null);
                recordMapper.updateById(record);

                LearningLesson lesson=new LearningLesson();
                lesson.setId(data.getLessonId());
                lesson.setLatestSectionId(data.getSectionId());
                lesson.setLatestLearnTime(LocalDateTime.now());
                lessonService.updateById(lesson);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    public void writeRecordCache(LearningRecord record) {
        log.debug("更新学习记录的缓存数据");
        try {
            //转json
            String json= JsonUtils.toJsonStr(new RecordCacheData(record));
            //写入redis
            String key= StringUtils.format(RECORD_KEY_TEMPLATE,record.getLessonId());
            redisTemplate.opsForHash().put(key,record.getSectionId().toString(),json);
            //添加过期时间
            redisTemplate.expire(key, Duration.ofMinutes(1));
        } catch (Exception e) {
            log.error("更新学习缓存记录异常",e);
        }
    }
    //读取学习记录缓存
    public LearningRecord readRecordCache(Long lessonId,Long sectionId){
        try {
            String key= StringUtils.format(RECORD_KEY_TEMPLATE,lessonId);
            Object cacheData=redisTemplate.opsForHash().get(key,sectionId.toString());
            if (cacheData==null){
                return null;
            }
            return JsonUtils.toBean(cacheData.toString(),LearningRecord.class);
        } catch (Exception e) {
            log.error("缓存读取异常",e);
            return null;
        }
    }
    //删除缓存
    public void cleanRecordCache(Long lessonId,Long sectionId){
        String key= StringUtils.format(RECORD_KEY_TEMPLATE,lessonId);
        redisTemplate.opsForHash().delete(key,sectionId.toString());
    }
    @Data
    @NoArgsConstructor
    private static class RecordCacheData{
        private Long id;
        private Integer moment;
        private Boolean finished;

        public RecordCacheData(LearningRecord record) {
            this.id = record.getId();
            this.moment = record.getMoment();
            this.finished = record.getFinished();
        }
    }
    @Data
    @NoArgsConstructor
    private static class RecordTaskData{
        public RecordTaskData(LearningRecord record) {
            this.lessonId = record.getLessonId();
            this.sectionId = record.getSectionId();
            this.moment = record.getMoment();
        }

        private Long lessonId;
        private Long sectionId;
        private Integer moment;
    }
}
