package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.enums.QuestionStatus;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.tianji.learning.service.IInteractionReplyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动问题的回答或评论 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-08-13
 */
@Service
@RequiredArgsConstructor
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {
    private final IInteractionQuestionService questionService;
    private final UserClient userClient;

    @Override
    public void saveReply(ReplyDTO replyDTO) {
        Long userId= UserContext.getUser();
        if (userId==null){
            throw new BadRequestException("用户未登录");
        }
        //拿到问题
        InteractionQuestion question=questionService.getById(replyDTO.getQuestionId());
        if (question==null){
            throw new BadRequestException("问题不存在");
        }

        InteractionReply reply=new InteractionReply();
        reply= BeanUtils.copyBean(replyDTO, InteractionReply.class);
        reply.setUserId(userId);
        reply.setLikedTimes(0);
        reply.setReplyTimes(0);
        reply.setCreateTime(LocalDateTime.now());
        reply.setUpdateTime(LocalDateTime.now());
        save(reply);
        //判断是否是回答
        if (replyDTO.getAnswerId()==null){
            Long id=reply.getId();
            question.setLatestAnswerId(id);
            question.setAnswerTimes(question.getAnswerTimes()+1);
            //如果是学生回答，就应该把状态转成未查看
            if (replyDTO.getIsStudent()){
                question.setStatus(QuestionStatus.UN_CHECK);
            }
            questionService.updateById(question);
        }else{
            //查出评论的是哪个回答
            InteractionReply rl=getById(reply.getAnswerId());
            rl.setReplyTimes(rl.getReplyTimes()+1);
            updateById(rl);
        }
    }

    @Override
    public PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery query) {
        if (query==null){
            return null;
        }
        if (query.getAnswerId()==null&&query.getQuestionId()==null){
            throw new BadRequestException("问题id和回答id不能同时为空");
        }
        //判断是查询回答还是评论
        if (query.getQuestionId()!=null&&(query.getAnswerId()==null||query.getAnswerId()==0)){
            //分页查询回答信息
            Page<InteractionReply> page = lambdaQuery()
                    .eq(InteractionReply::getQuestionId, query.getQuestionId())
                    .page(query.toMpPageDefaultSortByCreateTimeDesc());
            List<InteractionReply>records=page.getRecords();
            if (CollUtils.isEmpty(records)){
                return null;
            }
            List<ReplyVO>voList=new ArrayList<>(records.size());
            //查用户的信息
            List<Long>userIds=new ArrayList<>();
            for (InteractionReply q : records) {
                userIds.add(q.getUserId());
            }
            List<UserDTO>users=userClient.queryUserByIds(userIds);
            Map<Long, UserDTO> userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
            for (InteractionReply record : records) {
                ReplyVO vo = BeanUtils.copyBean(record, ReplyVO.class);
                UserDTO user=userMap.get(record.getUserId());
                if (user!=null) {
                    vo.setUserIcon(user.getIcon());
                    vo.setUserName(user.getName());
                    vo.setUserId(user.getId());
                    vo.setUserType(user.getType());
                    voList.add(vo);
                }
            }
            return PageDTO.of(page,voList);
        }else if (query.getQuestionId()==null&&query.getAnswerId()!=null){
            Page<InteractionReply> page = lambdaQuery()
                    .eq(InteractionReply::getAnswerId, query.getAnswerId())
                    .page(query.toMpPageDefaultSortByCreateTimeDesc());
            List<InteractionReply>records=page.getRecords();
            if (CollUtils.isEmpty(records)){
                return null;
            }
            List<ReplyVO>voList=new ArrayList<>(records.size());
            //查用户的信息
            List<Long>userIds=new ArrayList<>();
            for (InteractionReply q : records) {
                userIds.add(q.getUserId());
            }
            List<UserDTO>users=userClient.queryUserByIds(userIds);
            Map<Long, UserDTO> userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
            for (InteractionReply record : records) {
                ReplyVO vo = BeanUtils.copyBean(record, ReplyVO.class);
                UserDTO user=userMap.get(record.getUserId());
                if (user!=null) {
                    vo.setUserIcon(user.getIcon());
                    vo.setUserName(user.getName());
                    vo.setUserId(user.getId());
                    vo.setUserType(user.getType());
                }
                voList.add(vo);
            }
            return PageDTO.of(page,voList);
        }

return null;
    }
}
