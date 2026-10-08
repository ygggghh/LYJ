package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.query.ReplyPageQuery;
import com.tianji.learning.domain.vo.ReplyVO;
import com.tianji.learning.service.IInteractionReplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 互动问题的回答或评论 前端控制器
 * </p>
 *
 * @author author
 * @since 2026-08-13
 */
@RestController
@RequestMapping("/admin/replies")
@Api(tags = "评论回答有关接口")
@RequiredArgsConstructor
public class InteractionReplyController {
    private final IInteractionReplyService replyService;
    @ApiOperation("新增回答或评论")
    @PostMapping
    public void saveReply(@RequestBody ReplyDTO replyDTO){
        replyService.saveReply(replyDTO);
    }
    @GetMapping("page")
    @ApiOperation("分页查询回答，评论")
    public PageDTO<ReplyVO>queryReplyPage(ReplyPageQuery query){
        return replyService.queryReplyPage(query);
    }
}
