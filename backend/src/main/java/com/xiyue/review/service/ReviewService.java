package com.xiyue.review.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xiyue.aunt.entity.Aunt;
import com.xiyue.aunt.mapper.AuntMapper;
import com.xiyue.common.exception.BusinessException;
import com.xiyue.common.result.ResultCode;
import com.xiyue.order.entity.ServiceOrder;
import com.xiyue.order.enums.OrderStatus;
import com.xiyue.order.mapper.ServiceOrderMapper;
import com.xiyue.review.dto.ReviewCreateRequest;
import com.xiyue.review.dto.ReviewResponse;
import com.xiyue.review.entity.Review;
import com.xiyue.review.mapper.ReviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 评价服务（阶段 4）。
 *
 * <p>规则（规范 §5.4、§7.9）：
 * <ul>
 *   <li>仅待评价(5)订单可评价，评价后订单变为已完成(6)；</li>
 *   <li>一个订单只能评价一次（uk_review_order 唯一索引兜底）；</li>
 *   <li>评价后更新阿姨评分（加权平均）与服务次数 +1；</li>
 *   <li>投诉处理的订单不更新阿姨评分（由投诉流程保证，评价流程不涉及）。</li>
 * </ul>
 *
 * <p>评分更新采用读-算-写（ADR-020）：MVP 并发量小可接受；后续可改 SQL 原子更新或 Redis 缓存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final ServiceOrderMapper orderMapper;
    private final AuntMapper auntMapper;

    /**
     * 创建评价（事务）。
     *
     * @param userId 当前登录用户 ID
     * @param req    评价请求
     * @return 评价响应
     */
    @Transactional(rollbackFor = Exception.class)
    public ReviewResponse create(Long userId, ReviewCreateRequest req) {
        // 1. 校验订单归属与状态
        ServiceOrder order = orderMapper.selectById(req.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (order.getStatus() != OrderStatus.PENDING_REVIEW.getCode()) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID,
                "仅待评价订单可评价，当前状态: " + OrderStatus.fromCode(order.getStatus()).getMessage());
        }

        // 2. 插入评价（uk_review_order 兜底防重复）
        Review review = new Review();
        review.setOrderId(order.getId());
        review.setUserId(userId);
        review.setRating(req.getRating());
        review.setContent(req.getContent());
        try {
            reviewMapper.insert(review);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "该订单已评价，不可重复评价");
        }

        // 3. 条件更新订单状态：待评价 → 已完成
        int rows = orderMapper.update(null,
            new LambdaUpdateWrapper<ServiceOrder>()
                .set(ServiceOrder::getStatus, OrderStatus.COMPLETED.getCode())
                .eq(ServiceOrder::getId, order.getId())
                .eq(ServiceOrder::getStatus, OrderStatus.PENDING_REVIEW.getCode())
        );
        if (rows != 1) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID,
                "评价失败，订单状态可能已变更（可能已投诉），请刷新后重试");
        }

        // 4. 更新阿姨评分与服务次数（读-算-写）
        updateAuntRating(order.getAuntId(), req.getRating());

        log.info("用户评价成功（userId={}, orderId={}, rating={}）", userId, order.getId(), req.getRating());
        return toResponse(review);
    }

    /**
     * 按订单 ID 查评价（任意登录用户可查）。
     *
     * @return 评价不存在返回 null
     */
    public ReviewResponse getByOrderId(Long orderId) {
        Review review = reviewMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, orderId)
        );
        return review == null ? null : toResponse(review);
    }

    /**
     * 更新阿姨评分（加权平均）与服务次数。
     *
     * <p>公式：newRating = (oldRating * oldCount + newRating) / (oldCount + 1)，保留 1 位小数。
     */
    private void updateAuntRating(Long auntId, Integer newRating) {
        Aunt aunt = auntMapper.selectById(auntId);
        if (aunt == null) {
            log.warn("评价更新阿姨评分时阿姨不存在（auntId={}），跳过", auntId);
            return;
        }
        int oldCount = aunt.getServiceCount();
        BigDecimal oldRating = aunt.getRating() != null ? aunt.getRating() : BigDecimal.ZERO;
        BigDecimal newAvg = oldRating.multiply(BigDecimal.valueOf(oldCount))
            .add(BigDecimal.valueOf(newRating))
            .divide(BigDecimal.valueOf(oldCount + 1), 1, RoundingMode.HALF_UP);
        aunt.setRating(newAvg);
        aunt.setServiceCount(oldCount + 1);
        auntMapper.updateById(aunt);
        log.info("阿姨评分更新（auntId={}, oldRating={}, oldCount={}, newRating={}, newAvg={}）",
            auntId, oldRating, oldCount, newRating, newAvg);
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
            .id(review.getId())
            .orderId(review.getOrderId())
            .userId(review.getUserId())
            .rating(review.getRating())
            .content(review.getContent())
            .createTime(review.getCreateTime())
            .build();
    }
}
