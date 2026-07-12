package com.xiyue.complaint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户投诉实体（阶段 4，简化版）。
 *
 * <p>一个订单只能投诉一次（uk_complaint_order）。仅待评价订单可投诉；
 * 管理员处理后订单变为已完成，不再允许评价，也不更新阿姨评分（规范 §7.9）。
 */
@Data
@TableName("complaint")
public class Complaint {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long userId;

    /** 投诉原因 */
    private String reason;

    /** 处理状态：PENDING 待处理 / HANDLED 已处理 */
    private String status;

    /** 管理员处理备注 */
    private String handleRemark;

    /** 处理时间 */
    private LocalDateTime handleTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
