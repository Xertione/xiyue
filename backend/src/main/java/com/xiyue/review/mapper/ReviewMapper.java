package com.xiyue.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiyue.review.entity.Review;

/**
 * 评价 Mapper。一个订单只能评价一次由 uk_review_order 唯一索引保障。
 */
public interface ReviewMapper extends BaseMapper<Review> {
}
