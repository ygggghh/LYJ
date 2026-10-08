package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.IdAndNumDTO;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.*;
import com.tianji.learning.domain.dto.LearningPlanDTO;
import com.tianji.learning.domain.enums.LessonStatus;
import com.tianji.learning.domain.enums.PlanStatus;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.domain.vo.LearningPlanPageVO;
import com.tianji.learning.domain.vo.LearningPlanVO;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILeaningLessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson>implements ILeaningLessonService {
    private final CourseClient courseClient;
    private final LearningRecordMapper learningRecordMapper;
    private final CatalogueClient catalogueClient;

    @Override
    @Transactional
    public void addUserLessons(Long userId, List<Long> courseIds) {
        //查询课程有效期
        List<CourseSimpleInfoDTO> cInfoList = courseClient.getSimpleInfoList(courseIds);
        if (CollUtils.isEmpty(cInfoList)){
            log.error("课程不存在");
            return;
        }
        //循环遍历所有购买的课程的购买信息，把购买的课程信息填入课程的信息
        List<LearningLesson>list=new ArrayList<>(cInfoList.size());
        for (CourseSimpleInfoDTO cInfo : cInfoList){
            //获取过期时间set进课程实体类里
            LearningLesson lesson = new LearningLesson();
            Integer validDuration = cInfo.getValidDuration();
            if (validDuration!=null&&validDuration>0){
                LocalDateTime now=LocalDateTime.now();
                lesson.setCreateTime(now);
                lesson.setExpireTime(now.plusMonths(validDuration));
            }
            //填充课程id和所属用户id,然后放入集合
            lesson.setCourseId(cInfo.getId());
            lesson.setUserId(userId);
            list.add(lesson);
        }
        //用mybatisplus的批量新增方法把课程的信息保存到数据库
        saveBatch(list);
    }

    @Override
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        //1.获取当前用户
        Long userId = UserContext.getUser();
        //2.分页查询
        Page<LearningLesson> page=lambdaQuery()
                .eq(LearningLesson::getUserId,userId)
                .page(query.toMpPage("latest_learn_time",false));
        List<LearningLesson>records=page.getRecords();
        if (CollUtils.isEmpty(records)){
            return PageDTO.empty(page);
        }
        //3.查询课程信息
        //3.1获取课程id
        Map<Long, CourseSimpleInfoDTO> cMap = queryCourseSimpleInfoList(records);
        //4.把两个查到的信息组合到vo里返回
        List<LearningLessonVO>list=new ArrayList<>(records.size());
        //4.1循环遍历，把从数据库查到的那个po把与vo相同的属性名的信息复制过去
        for (LearningLesson r : records){
            LearningLessonVO vo= BeanUtils.copyBean(r, LearningLessonVO.class);
            //4.2然后因为说现在这个list已经是有课程的id了，而课程的信息又在cInfoList里，这样子我得去for循环
            //去一个个对比，然后填充进去，效率太低了，因此牛逼的来了，我把课程弄成一个map，key是课程id
            //值是课程信息，这样子我只需要有课程的id就能立马拿到课程信息填充到list里
            CourseSimpleInfoDTO cInfo = cMap.get(r.getCourseId());
            //取出课程信息后把需要的给他填充进vo里,然后放进list
            vo.setCourseName(cInfo.getName());
            vo.setCourseCoverUrl(cInfo.getCoverUrl());
            vo.setSections(cInfo.getSectionNum());
            list.add(vo);
        }
        //最后把前端需要的都返回回去

        return PageDTO.of(page,list);
    }

    private Map<Long, CourseSimpleInfoDTO> queryCourseSimpleInfoList(List<LearningLesson> records) {
        Set<Long> cIds = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        //3.2根据课程id查询课程信息
        List<CourseSimpleInfoDTO>cInfoList = courseClient.getSimpleInfoList(cIds);
        if (CollUtils.isEmpty(cInfoList)){
            throw new BadRequestException("课程不存在");
        }
        //对应4.2的操作
        Map<Long, CourseSimpleInfoDTO> cMap = cInfoList.stream()
                .collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        return cMap;
    }

    @Override
    public LearningLessonVO queryMyCurrentLesson() {
        Long userId = UserContext.getUser();
        //查正在学习的课程信息
        LearningLesson lesson = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getStatus, LessonStatus.LEARNING.getValue())
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 1")
                .one();
        if (lesson == null) {
            return null;
        }
        //拷贝相同字段到LearningLessonVO
        LearningLessonVO vo =BeanUtils.copyBean(lesson, LearningLessonVO.class);
        //查询课程的详细信息
        CourseFullInfoDTO cInfo=courseClient.getCourseInfoById(lesson.getCourseId(), false,false);
        vo.setCourseName(cInfo.getName());
        vo.setCourseCoverUrl(cInfo.getCoverUrl());
        vo.setSections(cInfo.getSectionNum());
        //统计一下课表里总课程
        Integer courseAmount=lambdaQuery()
                .eq(LearningLesson::getUserId,userId)
                .count();
        vo.setCourseAmount(courseAmount);
        //查最后一次学习的小节的各自信息
        List<CataSimpleInfoDTO>cataInfos=catalogueClient.batchQueryCatalogue(CollUtils.singletonList(lesson.getLatestSectionId()));
        if(!CollUtils.isEmpty(cataInfos)){
            CataSimpleInfoDTO cataInfo=cataInfos.get(0);
            vo.setLatestSectionName(cataInfo.getName());
            vo.setLatestSectionIndex(cataInfo.getCIndex());
        }
        return vo;
    }

    @Override
    public LearningLessonVO queryLessonByCourseId(Integer courseId) {
        Long userId = UserContext.getUser();
        LearningLesson lesson=lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .one();
        if (lesson == null) {
            return null;
        }
        return BeanUtils.copyBean(lesson, LearningLessonVO.class);
    }

    @Override
    public void deleteByCourseId(Long userId,Long courseId) {
        if (userId==null){
        userId = UserContext.getUser();}
        LearningLesson lesson=lambdaQuery()
                .eq(LearningLesson::getUserId,userId)
                .eq(LearningLesson::getCourseId,courseId)
                .one();
        if (lesson!=null){
            removeById(lesson.getId());
        }
    }

    @Override
    public Long isLessonValid(Long courseId) {
        Long userId=UserContext.getUser();
        if (userId==null){
            return null;
        }
        LearningLesson lesson=lambdaQuery()
                .eq(LearningLesson::getUserId,userId)
                .eq(LearningLesson::getCourseId,courseId)
                .one();
        if (lesson==null){
        return null;}
        return lesson.getId();
    }

    @Override
    public Integer countLearningLessonByCourse(Long courseId) {
        long count=lambdaQuery()
                .eq(LearningLesson::getCourseId,courseId)
                .in(LearningLesson::getStatus,
                        LessonStatus.NOT_BEGIN.getValue(),
                        LessonStatus.LEARNING.getValue(),
                        LessonStatus.FINISHED.getValue())
                .count();
        return (int)count;
    }

    @Override
    public LearningLesson queryByUserIdAndCourseId(Long userId, Long courseId) {
        return lambdaQuery()
                .eq(LearningLesson::getUserId,userId)
                .eq(LearningLesson::getCourseId,courseId)
                .one();
    }

    @Override
    public void createLearningPlans(LearningPlanDTO planDTO) {
        Long courseId=planDTO.getCourseId();
        Integer freq=planDTO.getFreq();
        Long userId=UserContext.getUser();
        LearningLesson lesson=lambdaQuery()
                .eq(LearningLesson::getCourseId,courseId)
                .eq(LearningLesson::getUserId,userId)
                .one();
        AssertUtils.isNotNull(lesson,"课程信息不存在");
        LearningLesson lesson1=new LearningLesson();
        lesson1.setWeekFreq(planDTO.getFreq());
        lesson1.setId(lesson.getId());
        if(lesson.getPlanStatus()== PlanStatus.NO_PLAN){
            lesson1.setPlanStatus(PlanStatus.PLAN_RUNNING);
        }
        updateById(lesson1);
    }

    @Override
    public LearningPlanPageVO queryMyPlans(PageQuery pageQuery) {
        LearningPlanPageVO result=new LearningPlanPageVO();
        Long userId = UserContext.getUser();
        //获取本周的开始和结束时间
        LocalDate now=LocalDate.now();
        LocalDateTime begin= DateUtils.getWeekBeginTime(now);
        LocalDateTime end=DateUtils.getWeekEndTime(now);
        //查询本周学习了多少小节
        Integer weekFinished = learningRecordMapper.selectCount(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getFinished, true)
                .gt(LearningRecord::getFinishTime, begin)
                .lt(LearningRecord::getFinishTime, end)
        );
        result.setWeekFinished(weekFinished);
        //统计本周总的计划学习小节
        Integer weekTotalPlan=getBaseMapper().queryTotalPlan(userId);
        result.setWeekTotalPlan(weekTotalPlan);
        //分页查询数据
        Page<LearningLesson> p = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING)
                .in(LearningLesson::getStatus, LessonStatus.NOT_BEGIN, LessonStatus.LEARNING)
                .page(pageQuery.toMpPage("latest_learn_time", false));
        List<LearningLesson>records=p.getRecords();
        if (CollUtils.isEmpty(records)){
            return result;
        }
        //查询对应课程的信息
        Map<Long, CourseSimpleInfoDTO> cMap = queryCourseSimpleInfoList(records);
        //统计每一个课程本周学习的小节数量
        List<IdAndNumDTO>list=learningRecordMapper.countLearnedSections(userId,begin,end);
        Map<Long, Integer> countMap = IdAndNumDTO.toMap(list);
        //组装数据
        List<LearningPlanVO>voList=new ArrayList<>();
        for (LearningLesson r:records){
            //拷贝属性到vo
            LearningPlanVO vo=BeanUtils.copyBean(r,LearningPlanVO.class);
            //填充详细信息
            CourseSimpleInfoDTO cInfo=cMap.get(r.getCourseId());
            if (cInfo!=null){
                vo.setCourseName(cInfo.getName());
                vo.setSections(cInfo.getSectionNum());
            }
            vo.setWeekLearnedSections(countMap.getOrDefault(r.getId(),0));
            voList.add(vo);
        }
        return result.pageInfo(p.getTotal(),p.getPages(),voList);
    }
}
