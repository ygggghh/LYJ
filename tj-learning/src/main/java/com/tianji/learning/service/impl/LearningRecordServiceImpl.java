package com.tianji.learning.service.impl;

import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.exceptions.DbException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.LearningRecordFormDTO;
import com.tianji.learning.domain.enums.LessonStatus;
import com.tianji.learning.domain.enums.SectionType;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILeaningLessonService;
import com.tianji.learning.service.ILearningRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.utils.LearningRecordDelayTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import javax.validation.constraints.NotNull;
import java.util.List;



/**
 * <p>
 * 学习记录表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-08-07
 */
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl extends ServiceImpl<LearningRecordMapper, LearningRecord> implements ILearningRecordService {
    private final ILeaningLessonService lessonService;
    private final CourseClient courseClient;
    private final LearningRecordDelayTaskHandler taskHandler;
    @Override
    public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {
        Long userId = UserContext.getUser();
        LearningLesson lesson = lessonService.queryByUserIdAndCourseId(userId, courseId);
        if (lesson==null){
            return null;
        }
        //查询学习记录
        List<LearningRecord>records=lambdaQuery()
                .eq(LearningRecord::getLessonId, lesson.getId())
                .list();
        //封装结果
        LearningLessonDTO dto=new LearningLessonDTO();
        dto.setId(lesson.getId());
        dto.setLatestSectionId(lesson.getLatestSectionId());
        dto.setRecords(BeanUtils.copyList(records, LearningRecordDTO.class));
        return dto;
    }

    @Override
    @Transactional
    public void addLearningRecord(LearningRecordFormDTO formDTO) {
        //1.获取登录用户
        Long userId = UserContext.getUser();
        //2.处理学习记录，这个变量是用来看是否学习完成
        boolean finished = false;
        if (formDTO.getSectionType()== SectionType.VIDEO){
            //如果是视频的学习记录
            finished=handleVideoRecord(formDTO, userId);
        }
        else {
            finished=handleExamRecord(formDTO, userId);
        }
        //通过返回值，来判断是否小节是不是已经学完
        if (!finished){
            return;
        }
        //处理课表数据
        handleLearningLessonChanges(formDTO);
    }

    private void handleLearningLessonChanges(LearningRecordFormDTO formDTO) {
        //查询课表，为了查课程
        LearningLesson lesson = lessonService.getById(formDTO.getLessonId());
        if (lesson==null){
            throw new BizIllegalException("课程不存在");
        }
        boolean allLearned=false;
        //判断是否有新的完成小节
            //根据课表的课程id查课程信息
            CourseFullInfoDTO cInfo = courseClient.getCourseInfoById(lesson.getCourseId(),false,false);
            if (cInfo==null){
                throw new BizIllegalException("课程不存在");
            }
            //比较课程的节数，看是否学完了
            allLearned=lesson.getLearnedSections()+1>=cInfo.getSectionNum();

        //更新课表数据
        lessonService.lambdaUpdate()
                .set(lesson.getLearnedSections()==0,LearningLesson::getStatus, LessonStatus.LEARNING.getValue())
                .set(allLearned,LearningLesson::getStatus, LessonStatus.FINISHED.getValue())
                .setSql("learned_sections=learned_sections+1")
                .eq(LearningLesson::getId, lesson.getId())
                .update();


    }

    private boolean handleExamRecord(LearningRecordFormDTO formDTO, Long userId) {
        //一参加考试就可以新增学习记录，因此直接新增就行
        //将dto转成po
        LearningRecord record = BeanUtils.copyBean(formDTO, LearningRecord.class);
        //填充数据
        record.setUserId(userId);
        record.setFinished(true);
        record.setCreateTime(formDTO.getCommitTime());
        //存入数据库
        boolean success = save(record);
        if (!success){
            throw new DbException("新增考试记录失败");
        }
        return true;
    }

    private boolean handleVideoRecord(LearningRecordFormDTO formDTO, Long userId) {
        //查询旧的学习记录
        LearningRecord old=queryOldRecord(formDTO.getLessonId(),formDTO.getSectionId());
        //判断是否存在，也就是说有没有这条记录
        if (old==null){
            //不存在，新增
            LearningRecord record = BeanUtils.copyBean(formDTO, LearningRecord.class);
            //填充数据
            record.setUserId(userId);
            //存入数据库
            boolean success = save(record);
            if (!success){
                throw new DbException("新增学习记录失败");
            }
            //返回false
            return false;
        }
        //存在，则更新
        //1.看他进度是不是大于百分之50并且旧的学习记录是不是已经被标记学过了（学过了是会把学习记录表
        // 中的finished字段更新成1的）
        boolean finished=formDTO.getMoment()*2>formDTO.getDuration() && !old.getFinished();
        if(!finished){
            LearningRecord record=new LearningRecord();
            record.setMoment(formDTO.getMoment());
            record.setLessonId(formDTO.getLessonId());
            record.setSectionId(formDTO.getSectionId());
            record.setId(old.getId());
            record.setFinished(old.getFinished());
            taskHandler.addLearningRecordTask(record);
            return false;
        }
        //更新数据
        boolean success = lambdaUpdate()
                //更新
                .set(LearningRecord::getMoment, formDTO.getMoment())
                //如果finished字段为1，则更新为1，因为1代表已经学完了，就得把数据库的finished字段更新为1
                .set(LearningRecord::getFinished, true)
                //也是finished字段为1，把完成时间也更新
                .set(LearningRecord::getFinishTime, formDTO.getCommitTime())
                .eq(LearningRecord::getId, old.getId())
                .update();
        if (!success){
            throw new DbException("更新学习记录失败");
        }
        taskHandler.cleanRecordCache(formDTO.getLessonId(), formDTO.getSectionId());
        return true;
    }

    private LearningRecord queryOldRecord(@NotNull(message = "课表id不能为空") Long lessonId, @NotNull(message = "节的id不能为空") Long sectionId) {
        //查询缓存
        LearningRecord record = taskHandler.readRecordCache(lessonId, sectionId);
        if (record!=null){
            return record;
        }
        //查不到再查数据库
        record=lambdaQuery()
                .eq(LearningRecord::getLessonId,lessonId)
                .eq(LearningRecord::getSectionId,sectionId)
                .one();
        //写入缓存
        taskHandler.writeRecordCache(record);
        return record;
    }

}

