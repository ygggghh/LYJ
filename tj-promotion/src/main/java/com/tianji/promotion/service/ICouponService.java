package com.tianji.promotion.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponVO;

import javax.validation.Valid;
import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 服务类
 * </p>
 *
 * @author author
 * @since 2026-09-13
 */
public interface ICouponService extends IService<Coupon> {

    void saveCoupon(@Valid CouponFormDTO formDTO);

    PageDTO<CouponPageVO> queryCouponByPage(CouponQuery query);

    void beginIssus(@Valid CouponIssueFormDTO dto);

    List<CouponVO> queryIssuingCoupons();
}
