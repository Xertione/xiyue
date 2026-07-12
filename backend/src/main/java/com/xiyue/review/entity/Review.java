package com.xiyue.review.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户评价实体（阶段 4）。
 *
 * <p>一个订单只能评价一次（uk_review_order）。评价后订单变为已完成，并更新阿姨评分与服务次数。
 */
@Data
@TableName("review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long userId;

    /** 评分（1-5） */
    private Integer rating;

    /** 评价内容 */
    private String content;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
