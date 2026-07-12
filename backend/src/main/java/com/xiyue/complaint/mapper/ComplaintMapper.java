package com.xiyue.complaint.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiyue.complaint.entity.Complaint;

/**
 * 投诉 Mapper。一个订单只能投诉一次由 uk_complaint_order 唯一索引保障。
 */
public interface ComplaintMapper extends BaseMapper<Complaint> {
}
