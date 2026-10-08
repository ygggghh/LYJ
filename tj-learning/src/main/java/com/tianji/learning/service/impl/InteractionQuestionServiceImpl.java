package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CategoryClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.enums.QuestionStatus;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-08-13
 */
@Service
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {
    private final InteractionReplyMapper replyMapper;
    private final UserClient userClient;
    private final CourseClient courseClient;
    private final SearchClient searchClient;
    private final CatalogueClient catalogueClient;
    private final CategoryClient categoryClient;
    private final CategoryCache categoryCache;
    private final int num=1;

    @Override
    public void saveQuestion(QuestionFormDTO questionDTO) {
        Long userId= UserContext.getUser();
        InteractionQuestion question= BeanUtils.copyBean(questionDTO,InteractionQuestion.class);
        question.setUserId(userId);
        save(question);
    }

    @Override
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query) {
        Long courseId= query.getCourseId();
        Long sectionId= query.getSectionId();
        if(courseId==null&&sectionId==null){
            throw new BadRequestException("课程id和小节id不能为空");
        }
        Page<InteractionQuestion> page = lambdaQuery()
                .select(InteractionQuestion.class,info -> !info.getProperty().equals("description"))
                .eq(query.getOnlyMine(), InteractionQuestion::getUserId, UserContext.getUser())
                .eq(courseId != null, InteractionQuestion::getCourseId, courseId)
                .eq(sectionId != null, InteractionQuestion::getSectionId, sectionId)
                .eq(InteractionQuestion::getHidden, false)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion>records=page.getRecords();
        if (CollUtils.isEmpty(records)){
            return null;
        }
        //根据id查询提问者和最近一次回答的信息
        Set<Long>userIds=new HashSet<>();
        Set<Long>answerIds=new HashSet<>();
        //得到问题当中提问者的id和最近一次回答的id
        for (InteractionQuestion q : records) {
            if (!q.getAnonymity()){
                userIds.add(q.getUserId());
            }
            answerIds.add(q.getLatestAnswerId());
        }
        answerIds.remove(null);
        //为了使得到时能够根据问题的那张表的最近一次回答的信息的id直接查出来最近一次回答的信息，所以把replies转成map
        //key是回答的id，value是具体内容，到时直接查id就能直接得到内容
        Map<Long,InteractionReply>replyMap=new HashMap<>(answerIds.size());
        if (CollUtils.isNotEmpty(answerIds)) {
            //根据id查最近一次回答的信息
            List<InteractionReply> replies = replyMapper.selectBatchIds(answerIds);
            for (InteractionReply reply : replies) {
                replyMap.put(reply.getId(),reply);
                if (!reply.getAnonymity()){
                    userIds.add(reply.getUserId());
                }
            }
        }
        //根据id查询用户的信息（提问者）
        userIds.remove(null);
        Map<Long,UserDTO>userMap=new HashMap<>(userIds.size());
        if (CollUtils.isNotEmpty(userIds)){
            List<UserDTO>users=userClient.queryUserByIds(userIds);
            //collect可以把一个集合转成另外一个集合，list转成map
            userMap=users.stream().collect(Collectors.toMap(UserDTO::getId,u->u));
        }
        //封装vo
        List<QuestionVO>voList=new ArrayList<>(records.size());
        for (InteractionQuestion record : records) {
            //从数据库查出来的问答信息record封装进vo
            QuestionVO vo = BeanUtils.copyBean(record, QuestionVO.class);
            //从数据库查出来的提问者的信息封装进vo，匿名的不用封装
            if (!record.getAnonymity()){
                UserDTO userDTO=userMap.get(record.getUserId());
                if (userDTO!=null){
                    vo.setUserId(userDTO.getId());
                    vo.setUserName(userDTO.getName());
                }
            }
            //从数据库查出开的回答者的信息封装进vo
            InteractionReply reply=replyMap.get(record.getLatestAnswerId());
            if (reply!=null){
                vo.setLatestReplyContent(reply.getContent());
                if (!reply.getAnonymity()){
                    UserDTO userDTO1=userMap.get(reply.getUserId());
                    vo.setLatestReplyUser(userDTO1.getName());
                }
            }
            voList.add(vo);
        }
        return PageDTO.of(page,voList);
    }

    @Override
    public QuestionVO queryQuestionById(Long id) {
        InteractionQuestion question=getById(id);
        if (question==null||question.getHidden()){
            return null;
        }
        UserDTO userDTO=null;
        if (!question.getAnonymity()){
            userDTO=userClient.queryUserById(question.getUserId());
        }
        QuestionVO vo=BeanUtils.copyBean(question,QuestionVO.class);
        if(userDTO!=null){
            vo.setUserName(userDTO.getName());
            vo.setUserIcon(userDTO.getIcon());
        }
        return vo;
    }

    @Override
    public PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query) {
        //根据课程名称查课程id
        List<Long>courseIds=null;
        if (StringUtils.isNotBlank(query.getCourseName())) {
            courseIds = searchClient.queryCoursesIdByName(query.getCourseName());
            if (CollUtils.isEmpty(courseIds)){
                return null;
            }
        }
        //根据前端的传过来的参数去查表数据
        Integer status=query.getStatus();
        LocalDateTime begin=query.getBeginTime();
        LocalDateTime end=query.getEndTime();
        Page<InteractionQuestion> page = lambdaQuery()
                .in(courseIds != null, InteractionQuestion::getCourseId, courseIds)
                .eq(status != null, InteractionQuestion::getStatus, status)
                .gt(begin != null, InteractionQuestion::getCreateTime, begin)
                .lt(end != null, InteractionQuestion::getCreateTime, end)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if (CollUtils.isEmpty(records)){
            return null;
        }
        //准备vo需要的其他数据，先拿到各种数据的id集合
        Set<Long>userIds=new HashSet<>();
        Set<Long>cIds=new HashSet<>();
        Set<Long>cataIds=new HashSet<>();
        for (InteractionQuestion q : records) {
            userIds.add(q.getUserId());
            cIds.add(q.getCourseId());
            cataIds.add(q.getChapterId());
            cataIds.add(q.getSectionId());
        }
        //查用户信息
        List<UserDTO> users = userClient.queryUserByIds(userIds);
        Map<Long,UserDTO>userMap=new HashMap<>(users.size());
        if (CollUtils.isNotEmpty(users)) {
            userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
        }
        //查课程信息
        List<CourseSimpleInfoDTO> cInfos = courseClient.getSimpleInfoList(cIds);
        Map<Long,CourseSimpleInfoDTO>cInfoMap=new HashMap<>(cInfos.size());
        if (CollUtils.isNotEmpty(cInfos)) {
            cInfoMap = cInfos.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, u -> u));
        }
        //查询章节
        List<CataSimpleInfoDTO>catas=catalogueClient.batchQueryCatalogue(cataIds);
        Map<Long,String>cataMap=new HashMap<>(catas.size());
        if (CollUtils.isNotEmpty(catas)) {
            cataMap = catas.stream().collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        }
        //封装vo
        List<QuestionAdminVO>voList=new ArrayList<>();
        for (InteractionQuestion q : records) {
            QuestionAdminVO vo = BeanUtils.copyBean(q, QuestionAdminVO.class);
            voList.add(vo);
            UserDTO userDTO=userMap.get(q.getUserId());
            if (userDTO!=null){
                vo.setUserName(userDTO.getName());
            }
            CourseSimpleInfoDTO cInfo=cInfoMap.get(q.getCourseId());
            if (cInfo!=null){
                vo.setCategoryName(categoryCache.getCategoryNames(cInfo.getCategoryIds()));
                vo.setCourseName(cInfo.getName());
            }
            vo.setChapterName(cataMap.getOrDefault(q.getChapterId(),""));
            vo.setSectionName(cataMap.getOrDefault(q.getSectionId(),""));
        }
        return PageDTO.of(page,voList);
    }

    @Override
    @Transactional
    public QuestionAdminVO queryQuestionByIdAdmin(Long id) {
        InteractionQuestion question=getById(id);
        if (question==null){
            return null;
        }
        question.setStatus(QuestionStatus.CHECKED);
        updateById(question);
        QuestionAdminVO vo=BeanUtils.copyBean(question, QuestionAdminVO.class);
        UserDTO user=userClient.queryUserById(question.getUserId());
        if (user!=null){
            vo.setUserName(user.getName());
            vo.setUserIcon(user.getIcon());
        }
        //查课程老师信息
        CourseFullInfoDTO cInfo=courseClient.getCourseInfoById(question.getCourseId(),false,true);
        if (cInfo!=null){
            //课程名字
            vo.setCourseName(cInfo.getName());
            //分类信息
            vo.setCategoryName(categoryCache.getCategoryNames(cInfo.getCategoryIds()));
            //查老师信息，用stream流把老师名字拼接起来
            List<UserDTO> teachers = userClient.queryUserByIds(cInfo.getTeacherIds());
            if (CollUtils.isNotEmpty(teachers)){
                vo.setTeacherName(teachers.stream().map(UserDTO::getName).collect(Collectors.joining("/")));
            }
        }
        //查询章节信息,把所有章节都查出来，因为传过来的问题id是能够查数据库查到是哪个章节的id的，所以只要把对应课程
        //的所有章节查出来，然后再根据id找就行
        List<CataSimpleInfoDTO>cataSimpleInfoDTOS=catalogueClient.batchQueryCatalogue(
                List.of(question.getChapterId(),question.getSectionId())
        );
        Map<Long,String>cataMap=new HashMap<>(cataSimpleInfoDTOS.size());
        if (CollUtils.isNotEmpty(cataSimpleInfoDTOS)){
            cataMap=cataSimpleInfoDTOS.stream().collect(Collectors.toMap(CataSimpleInfoDTO::getId,CataSimpleInfoDTO::getName));
        }
        //根据id找到章节的名字
        vo.setChapterName(cataMap.getOrDefault(question.getChapterId(),""));
        vo.setSectionName(cataMap.getOrDefault(question.getSectionId(),""));

        return vo;
    }
}
