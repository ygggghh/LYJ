package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.ICouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 优惠券的规则信息 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-09-13
 */
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {
    private final ICouponScopeService scopeService;

    @Override
    @Transactional
    public void saveCoupon(CouponFormDTO dto) {
        //保存优惠券
        Coupon coupon = BeanUtils.copyBean(dto, Coupon.class);
        save(coupon);
        //保存限定范围
        if (!dto.getSpecific()){
            return;
        }
        Long couponId = coupon.getId();
        List<Long> scopes = dto.getScopes();
        if (CollUtils.isEmpty(scopes)){
            throw new BadRequestException("限定范围不能为空");
        }
        List<CouponScope> collect = scopes.stream().map(bizId -> new CouponScope().setBizId(bizId).setCouponId(couponId)).collect(Collectors.toList());
        scopeService.saveBatch(collect);
    }

    @Override
    public PageDTO<CouponPageVO> queryCouponByPage(CouponQuery query) {
        Integer status=query.getStatus();
        String name=query.getName();
        Integer type=query.getType();
        Page<Coupon> page = lambdaQuery()
                .eq(type != null, Coupon::getDiscountType, type)
                .eq(status != null, Coupon::getStatus, status)
                .like(StringUtils.isNotBlank(name), Coupon::getName, name)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<Coupon>records=page.getRecords();
        if (CollUtils.isEmpty(records)){
            return null;
        }
        List<CouponPageVO> vo = BeanUtils.copyList(records, CouponPageVO.class);
        return PageDTO.of(page,vo);
    }

    @Override
    public void beginIssus(CouponIssueFormDTO dto) {
        //查询优惠券
        Coupon coupon=getById(dto.getId());
        //查询优惠券状态
        if(coupon.getStatus()!= CouponStatus.DRAFT&&coupon.getStatus()!=CouponStatus.PAUSE){
            throw new BizIllegalException("优惠券状态错误");
        }
        //判断是否立刻发放
        LocalDateTime issueBeginTime=dto.getIssueBeginTime();
        LocalDateTime now=LocalDateTime.now();
        boolean isBegin=issueBeginTime==null||!issueBeginTime.isAfter(now);
        //更新优惠券
        Coupon c=BeanUtils.copyBean(dto,Coupon.class);
        if (isBegin){
            c.setStatus(CouponStatus.ISSUING);
            c.setIssueBeginTime(now);
        }else{
            c.setStatus(CouponStatus.UN_ISSUE);
        }
        updateById(c);
    }
}
