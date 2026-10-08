package com.tianji.learning.service.impl;

import com.google.protobuf.ListValue;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.constants.RedisConstants;
import com.tianji.learning.domain.po.PointsBoard;
import com.tianji.learning.domain.query.PointsBoardQuery;
import com.tianji.learning.domain.vo.PointsBoardItemVO;
import com.tianji.learning.domain.vo.PointsBoardVO;
import com.tianji.learning.mapper.PointsBoardMapper;
import com.tianji.learning.service.IPointsBoardService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.catalina.LifecycleState;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 学霸天梯榜 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-08-28
 */
@Service
@RequiredArgsConstructor
public class PointsBoardServiceImpl extends ServiceImpl<PointsBoardMapper, PointsBoard> implements IPointsBoardService {
    private final StringRedisTemplate redisTemplate;
    private final UserClient userClient;

    @Override
    public PointsBoardVO queryPointsBoardBySeason(PointsBoardQuery query) {
        //判断是否是当前赛季
        Long season = query.getSeason();
        Boolean isCurrent=season==null||season==0;
        //查询我的积分和排名
        LocalDateTime now=LocalDateTime.now();
        String key= RedisConstants.POINTS_BOARD_KEY_PREFIX+now.format(DateUtils.POINTS_BOARD_SUFFIX_FORMATTER);
        PointsBoard myBoard=isCurrent?queryMyCurrentBoard(key):queryMyHistoryBoard(season);
        List<PointsBoard>list=isCurrent?queryCurrentBoardList(key,query.getPageNo(),query.getPageSize()):queryHistoryBoardList(query);
        //封装vo
        PointsBoardVO vo=new PointsBoardVO();
        //先封装我的排名和积分
        if (myBoard!=null){
            vo.setRank(myBoard.getRank());
            vo.setPoints(myBoard.getPoints());
        }
        if (CollUtils.isEmpty(list)){
            return vo;
        }
        //封装积分榜单
        List<PointsBoardItemVO>items=new ArrayList<>(list.size());
        //查出榜单上的用户信息
        Set<Long> userIds = list.stream().map(PointsBoard::getUserId).collect(Collectors.toSet());
        List<UserDTO> users = userClient.queryUserByIds(userIds);
        Map<Long,String>userMap=new HashMap<>(userIds.size());
        if (CollUtils.isNotEmpty(users)){
        userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, UserDTO::getName));}
        //封装榜单信息
        for (PointsBoard p : list) {
            PointsBoardItemVO vo1=new PointsBoardItemVO();
            vo1.setRank(p.getRank());
            vo1.setRank(p.getRank());
            vo1.setName(userMap.get(p.getUserId()));
            items.add(vo1);
        }
        vo.setBoardList(items);
        return vo;
    }

    @Override
    public void createPointsBoardTableBySeason(Integer season) {
        getBaseMapper().createPointsBoardTable("points_board_"+season);
    }

    private List<PointsBoard> queryHistoryBoardList(PointsBoardQuery query) {
        return null;
    }

    private List<PointsBoard> queryCurrentBoardList(String key, @Min(value = 1, message = "页码不能小于1") Integer pageNo, @Min(value = 1, message = "每页查询数量不能小于1") Integer pageSize) {
        //计算分页
        int from=(pageNo-1)*pageSize;
        //查询积分排名
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, from, from + pageSize - 1);
        if (CollUtils.isEmpty(tuples)){
            return null;
        }
        //对查询出来的积分排名进行封装
        List<PointsBoard>list=new ArrayList<>(tuples.size());
        int num=from+1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            PointsBoard p=new PointsBoard();
            String userId=tuple.getValue();
            Double score = tuple.getScore();
            if (userId==null||score==null){
                continue;
            }
            p.setUserId(Long.valueOf(userId));
            p.setPoints(score.intValue());
            p.setRank(num++);
            list.add(p);
        }
        return list;
    }

    private PointsBoard queryMyHistoryBoard(Long season) {
        return null;
    }

    private PointsBoard queryMyCurrentBoard(String key) {
        //绑定key
        BoundZSetOperations<String, String> ops = redisTemplate.boundZSetOps(key);
        //查询积分和排名
        Long userId = UserContext.getUser();
        Double points = ops.score(key);
        Long rank = ops.reverseRank(userId);
        //封装
        PointsBoard p=new PointsBoard();
        p.setPoints(points==null?0:points.intValue());
        p.setRank(rank==null?0:rank.intValue()+1);
        return p;
    }
}
