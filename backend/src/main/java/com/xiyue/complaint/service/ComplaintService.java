package com.xiyue.complaint.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiyue.common.exception.BusinessException;
import com.xiyue.common.result.PageResponse;
import com.xiyue.common.result.ResultCode;
import com.xiyue.complaint.dto.ComplaintCreateRequest;
import com.xiyue.complaint.dto.ComplaintHandleRequest;
import com.xiyue.complaint.dto.ComplaintResponse;
import com.xiyue.complaint.entity.Complaint;
import com.xiyue.complaint.mapper.ComplaintMapper;
import com.xiyue.order.entity.ServiceOrder;
import com.xiyue.order.enums.OrderStatus;
import com.xiyue.order.mapper.ServiceOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 投诉服务（阶段 4，简化版）。
 *
 * <p>规则（规范 §5.4、§7.9）：
 * <ul>
 *   <li>仅待评价(5)订单可投诉一次；投诉后订单变为投诉中(8)；</li>
 *   <li>一个订单只能投诉一次（uk_complaint_order 唯一索引兜底）；</li>
 *   <li>管理员处理后订单变为已完成(6)，不再允许评价，也不更新阿姨评分；</li>
 *   <li>投诉处理使用状态条件更新，防止重复处理。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintMapper complaintMapper;
    private final ServiceOrderMapper orderMapper;

    /**
     * 创建投诉（事务）。
     */
    @Transactional(rollbackFor = Exception.class)
    public ComplaintResponse create(Long userId, ComplaintCreateRequest req) {
        ServiceOrder order = orderMapper.selectById(req.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (order.getStatus() != OrderStatus.PENDING_REVIEW.getCode()) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID,
                "仅待评价订单可投诉，当前状态: " + OrderStatus.fromCode(order.getStatus()).getMessage());
        }

        Complaint complaint = new Complaint();
        complaint.setOrderId(order.getId());
        complaint.setUserId(userId);
        complaint.setReason(req.getReason());
        complaint.setStatus("PENDING");
        try {
            complaintMapper.insert(complaint);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "该订单已投诉，不可重复投诉");
        }

        // 条件更新订单状态：待评价 → 投诉中
        int rows = orderMapper.update(null,
            new LambdaUpdateWrapper<ServiceOrder>()
                .set(ServiceOrder::getStatus, OrderStatus.COMPLAINT.getCode())
                .eq(ServiceOrder::getId, order.getId())
                .eq(ServiceOrder::getStatus, OrderStatus.PENDING_REVIEW.getCode())
        );
        if (rows != 1) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID,
                "投诉失败，订单状态可能已变更（可能已评价），请刷新后重试");
        }

        log.info("用户提交投诉（userId={}, orderId={}）", userId, order.getId());
        return toResponse(complaint);
    }

    /**
     * 管理员投诉列表（可按状态筛选）。
     */
    public PageResponse<ComplaintResponse> listForAdmin(long page, long size, String status) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        Page<Complaint> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Complaint> wrapper = new LambdaQueryWrapper<Complaint>()
            .eq(status != null && !status.isBlank(), Complaint::getStatus, status)
            .orderByDesc(Complaint::getCreateTime)
            .orderByDesc(Complaint::getId);
        Page<Complaint> result = complaintMapper.selectPage(pageParam, wrapper);
        List<ComplaintResponse> items = result.getRecords().stream().map(this::toResponse).toList();
        return PageResponse.<ComplaintResponse>builder()
            .records(items)
            .total(result.getTotal())
            .page(result.getCurrent())
            .size(result.getSize())
            .build();
    }

    /**
     * 管理员处理投诉（事务）。
     *
     * <p>条件更新投诉状态为已处理，同时条件更新订单状态：投诉中 → 已完成。
     * 不更新阿姨评分（规范 §7.9）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long complaintId, ComplaintHandleRequest req) {
        Complaint complaint = complaintMapper.selectById(complaintId);
        if (complaint == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "投诉不存在");
        }
        if (!"PENDING".equals(complaint.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID, "该投诉已处理，不可重复处理");
        }

        // 条件更新投诉状态
        int rows = complaintMapper.update(null,
            new LambdaUpdateWrapper<Complaint>()
                .set(Complaint::getStatus, "HANDLED")
                .set(Complaint::getHandleRemark, req.getHandleRemark())
                .set(Complaint::getHandleTime, LocalDateTime.now())
                .eq(Complaint::getId, complaintId)
                .eq(Complaint::getStatus, "PENDING")
        );
        if (rows != 1) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID, "投诉处理失败，可能已被其他管理员处理");
        }

        // 条件更新订单状态：投诉中 → 已完成
        int orderRows = orderMapper.update(null,
            new LambdaUpdateWrapper<ServiceOrder>()
                .set(ServiceOrder::getStatus, OrderStatus.COMPLETED.getCode())
                .eq(ServiceOrder::getId, complaint.getOrderId())
                .eq(ServiceOrder::getStatus, OrderStatus.COMPLAINT.getCode())
        );
        if (orderRows != 1) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID,
                "订单状态更新失败，投诉订单状态可能已变更");
        }

        log.info("管理员处理投诉（complaintId={}, orderId={}, remark={}）",
            complaintId, complaint.getOrderId(), req.getHandleRemark());
    }

    private ComplaintResponse toResponse(Complaint c) {
        return ComplaintResponse.builder()
            .id(c.getId())
            .orderId(c.getOrderId())
            .userId(c.getUserId())
            .reason(c.getReason())
            .status(c.getStatus())
            .handleRemark(c.getHandleRemark())
            .handleTime(c.getHandleTime())
            .createTime(c.getCreateTime())
            .build();
    }
}
