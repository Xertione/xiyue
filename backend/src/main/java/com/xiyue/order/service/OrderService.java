package com.xiyue.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiyue.aunt.entity.Aunt;
import com.xiyue.aunt.enums.AuntAcceptStatus;
import com.xiyue.aunt.enums.AuntAdminStatus;
import com.xiyue.aunt.mapper.AuntMapper;
import com.xiyue.common.exception.BusinessException;
import com.xiyue.common.result.PageResponse;
import com.xiyue.common.result.ResultCode;
import com.xiyue.integration.MockPaymentService;
import com.xiyue.order.dto.CreateOrderRequest;
import com.xiyue.order.dto.GrabListItem;
import com.xiyue.order.dto.OrderDetail;
import com.xiyue.order.dto.OrderListItem;
import com.xiyue.order.dto.PayResponse;
import com.xiyue.order.entity.AuntBookingSlot;
import com.xiyue.order.entity.ServiceOrder;
import com.xiyue.order.enums.OrderStatus;
import com.xiyue.order.mapper.AuntBookingSlotMapper;
import com.xiyue.order.mapper.ServiceOrderMapper;
import com.xiyue.order.util.OrderNoGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单服务（阶段 3 核心）。
 *
 * <p>职责分区：
 * <ul>
 *   <li>用户端：创建订单、模拟支付、订单列表/详情、取消（含模拟退款+档期释放）；</li>
 *   <li>阿姨端：抢单大厅、抢单（事务+唯一索引）、阿姨订单列表/详情；</li>
 *   <li>管理员端：全量订单、指派阿姨（兜底）。</li>
 * </ul>
 *
 * <p>关键设计：
 * <ul>
 *   <li>订单状态条件更新保证原子性（防并发抢单/取消）；</li>
 *   <li>档期联合唯一索引保障同一阿姨同一小时块唯一；</li>
 *   <li>抢单事务内：条件更新订单 + 插入档期，任一失败整体回滚（ADR-010）；</li>
 *   <li>待支付/待抢单不占档期，抢单/指派成功才锁档期，取消释放档期（规范 §5.3）。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ServiceOrderMapper orderMapper;
    private final AuntBookingSlotMapper slotMapper;
    private final AuntMapper auntMapper;
    private final OrderNoGenerator orderNoGenerator;
    private final MockPaymentService mockPaymentService;

    // ===== 用户端 =====

    /**
     * 创建订单（规范 §5.1、§7.7）。
     *
     * <p>校验服务时间合法后，生成订单号，保存联系人/电话/地址/金额快照，
     * 状态置为待支付(0)，aunt_id 为 null。待支付订单不占用档期。
     *
     * @param userId 当前登录用户 ID
     * @param req    创建请求
     * @return 订单详情
     */
    public OrderDetail createOrder(Long userId, CreateOrderRequest req) {
        // 校验不跨天：endHour = startHour + durationHours 必须 <= 24
        int endHour = req.getStartHour() + req.getDurationHours();
        if (endHour > 24) {
            throw new BusinessException(ResultCode.PARAM_INVALID,
                "服务时间不能跨天，结束时间不能超过 24 点");
        }

        ServiceOrder order = new ServiceOrder();
        order.setUserId(userId);
        order.setServiceDate(req.getServiceDate());
        order.setStartHour(req.getStartHour());
        order.setDurationHours(req.getDurationHours());
        order.setContactName(req.getContactName());
        order.setContactPhone(req.getContactPhone());
        order.setAddress(req.getAddress());
        order.setAmount(req.getAmount());
        order.setStatus(OrderStatus.PENDING_PAY.getCode());

        // 订单号冲突重试（同毫秒 1/900 概率，唯一索引兜底，重试保证用户体验）
        for (int attempt = 0; attempt < 3; attempt++) {
            order.setOrderNo(orderNoGenerator.generate());
            try {
                orderMapper.insert(order);
                break;
            } catch (DuplicateKeyException e) {
                if (attempt == 2) {
                    throw new BusinessException(ResultCode.BUSINESS_ERROR, "订单创建失败，请稍后重试");
                }
                log.warn("订单号冲突，重试（attempt={}）", attempt + 1);
            }
        }

        log.info("创建订单（userId={}, orderNo={}, amount={}）", userId, order.getOrderNo(), order.getAmount());
        return toDetail(order);
    }

    /**
     * 模拟支付（规范 §7.4）。
     *
     * <p>仅允许待支付订单发起；条件更新（WHERE status=待支付）保证原子性，
     * 防止与取消并发；支付成功状态置为待抢单(1)，记录支付流水号/时间/方式。
     *
     * @param userId  当前登录用户 ID
     * @param orderId 订单 ID
     * @return 支付响应
     */
    public PayResponse pay(Long userId, Long orderId) {
        ServiceOrder order = getOrderAndCheckOwnership(orderId, userId);
        if (order.getStatus() != OrderStatus.PENDING_PAY.getCode()) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID,
                "仅待支付订单可支付，当前状态: " + OrderStatus.fromCode(order.getStatus()).getMessage());
        }
        String payNo = mockPaymentService.generatePayNo();
        int rows = orderMapper.update(null,
            new LambdaUpdateWrapper<ServiceOrder>()
                .set(ServiceOrder::getStatus, OrderStatus.PENDING_GRAB.getCode())
                .set(ServiceOrder::getPayNo, payNo)
                .set(ServiceOrder::getPayTime, LocalDateTime.now())
                .set(ServiceOrder::getPayMethod, "MOCK")
                .eq(ServiceOrder::getId, orderId)
                .eq(ServiceOrder::getStatus, OrderStatus.PENDING_PAY.getCode())
        );
        if (rows != 1) {
            // 并发：订单已被支付或取消
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID,
                "订单支付失败，可能已被支付或取消，请刷新后重试");
        }
        log.info("模拟支付成功（userId={}, orderNo={}, payNo={}）", userId, order.getOrderNo(), payNo);
        return PayResponse.builder()
            .orderNo(order.getOrderNo())
            .payNo(payNo)
            .status(OrderStatus.PENDING_GRAB.getCode())
            .statusText(OrderStatus.PENDING_GRAB.getMessage())
            .build();
    }

    /**
     * 用户订单列表（分页 + 状态筛选）。
     */
    public PageResponse<OrderListItem> listForUser(Long userId, long page, long size, Integer status) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        Page<ServiceOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ServiceOrder> wrapper = new LambdaQueryWrapper<ServiceOrder>()
            .eq(ServiceOrder::getUserId, userId)
            .eq(status != null, ServiceOrder::getStatus, status)
            .orderByDesc(ServiceOrder::getCreateTime)
            .orderByDesc(ServiceOrder::getId);
        Page<ServiceOrder> result = orderMapper.selectPage(pageParam, wrapper);
        List<OrderListItem> items = result.getRecords().stream().map(this::toListItem).toList();
        return PageResponse.<OrderListItem>builder()
            .records(items)
            .total(result.getTotal())
            .page(result.getCurrent())
            .size(result.getSize())
            .build();
    }

    /**
     * 用户订单详情（校验归属）。
     */
    public OrderDetail getDetailForUser(Long userId, Long orderId) {
        ServiceOrder order = getOrderAndCheckOwnership(orderId, userId);
        return toDetail(order);
    }

    /**
     * 用户取消订单（规范 §7.6）。
     *
     * <p>规则：
     * <ul>
     *   <li>仅待支付(0)、待抢单(1)、待服务(2) 允许取消；服务中/待确认禁止取消，需联系管理员；</li>
     *   <li>使用状态条件更新（WHERE status IN(0,1,2)）防止与抢单/开始服务并发成功；</li>
     *   <li>已支付订单（待抢单/待服务）记录模拟退款状态、流水号、时间；</li>
     *   <li>待服务订单已锁档期，取消时在同一事务中删除档期记录释放。</li>
     * </ul>
     *
     * @param userId  当前登录用户 ID
     * @param orderId 订单 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long orderId) {
        ServiceOrder order = getOrderAndCheckOwnership(orderId, userId);
        int oldStatus = order.getStatus();
        if (!OrderStatus.isCancelable(oldStatus)) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID,
                "当前状态（" + OrderStatus.fromCode(oldStatus).getMessage() + "）不允许取消，服务中/待确认请联系管理员");
        }

        // 条件更新：仅当状态仍为可取消状态时才生效（防并发抢单/开始服务）
        int rows = orderMapper.update(null,
            new LambdaUpdateWrapper<ServiceOrder>()
                .set(ServiceOrder::getStatus, OrderStatus.CANCELLED.getCode())
                .set(ServiceOrder::getCancelTime, LocalDateTime.now())
                .eq(ServiceOrder::getId, orderId)
                .in(ServiceOrder::getStatus,
                    OrderStatus.PENDING_PAY.getCode(),
                    OrderStatus.PENDING_GRAB.getCode(),
                    OrderStatus.PENDING_SERVICE.getCode())
        );
        if (rows != 1) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID,
                "订单取消失败，状态可能已变更，请刷新后重试");
        }

        // 已支付订单（待抢单/待服务）记录模拟退款
        if (OrderStatus.needsRefund(oldStatus)) {
            String refundNo = mockPaymentService.generateRefundNo();
            orderMapper.update(null,
                new LambdaUpdateWrapper<ServiceOrder>()
                    .set(ServiceOrder::getRefundStatus, "REFUNDED")
                    .set(ServiceOrder::getRefundNo, refundNo)
                    .set(ServiceOrder::getRefundTime, LocalDateTime.now())
                    .eq(ServiceOrder::getId, orderId)
            );
            log.info("模拟退款（orderNo={}, refundNo={}）", order.getOrderNo(), refundNo);
        }

        // 释放档期（不依赖 oldStatus：待支付/待抢单无档期记录，删除 0 条无害；待服务有档期则释放）
        int deleted = slotMapper.delete(
            new LambdaQueryWrapper<AuntBookingSlot>()
                .eq(AuntBookingSlot::getOrderId, orderId)
        );
        if (deleted > 0) {
            log.info("释放档期（orderNo={}, 删除{}条占用记录）", order.getOrderNo(), deleted);
        }

        log.info("用户取消订单（userId={}, orderNo={}, oldStatus={}）",
            userId, order.getOrderNo(), OrderStatus.fromCode(oldStatus).getMessage());
    }

    // ===== 阿姨端 =====

    /**
     * 抢单大厅：待抢单订单列表（规范 §4 阿姨角色）。
     *
     * <p>前置校验阿姨状态：休息/下架/禁用的阿姨查看抢单大厅无意义，返回空列表。
     * 抢单操作时仍会二次校验（{@link #grab}），保证并发安全。
     */
    public PageResponse<GrabListItem> grabList(Long auntUserId, long page, long size) {
        // 校验阿姨状态：非可用/休息状态返回空列表
        Aunt aunt = auntMapper.selectOne(
            new LambdaQueryWrapper<Aunt>().eq(Aunt::getUserId, auntUserId)
        );
        if (aunt == null
            || !AuntAdminStatus.AVAILABLE.name().equals(aunt.getAdminStatus())
            || !AuntAcceptStatus.AVAILABLE.name().equals(aunt.getAcceptStatus())) {
            return PageResponse.<GrabListItem>builder()
                .records(List.of())
                .total(0L)
                .page(Math.max(page, 1))
                .size(Math.min(Math.max(size, 1), 100))
                .build();
        }
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        Page<ServiceOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ServiceOrder> wrapper = new LambdaQueryWrapper<ServiceOrder>()
            .eq(ServiceOrder::getStatus, OrderStatus.PENDING_GRAB.getCode())
            .orderByDesc(ServiceOrder::getCreateTime)
            .orderByDesc(ServiceOrder::getId);
        Page<ServiceOrder> result = orderMapper.selectPage(pageParam, wrapper);
        List<GrabListItem> items = result.getRecords().stream().map(this::toGrabListItem).toList();
        return PageResponse.<GrabListItem>builder()
            .records(items)
            .total(result.getTotal())
            .page(result.getCurrent())
            .size(result.getSize())
            .build();
    }

    /**
     * 阿姨抢单（事务 + 条件更新 + 档期唯一索引，规范 §7.3、ADR-010）。
     *
     * <p>事务步骤：
     * <ol>
     *   <li>查阿姨资料，校验管理状态=可用 + 接单状态=可抢单（休息不能抢，ADR-017）；</li>
     *   <li>查订单校验状态=待抢单；</li>
     *   <li>档期预检（该阿姨该日期各小时块未被占用）；</li>
     *   <li>条件更新订单（WHERE status=待抢单 AND aunt_id IS NULL），校验行数=1；</li>
     *   <li>循环插入档期占用记录，唯一索引冲突则抛 AUNT_SLOT_CONFLICT 触发回滚。</li>
     * </ol>
     * 任一步失败整体回滚。
     *
     * @param auntUserId 阿姨的 sys_user.id
     * @param orderId    订单 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void grab(Long auntUserId, Long orderId) {
        Aunt aunt = auntMapper.selectOne(
            new LambdaQueryWrapper<Aunt>().eq(Aunt::getUserId, auntUserId)
        );
        if (aunt == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "阿姨资料不存在，仅阿姨角色可抢单");
        }
        if (!AuntAdminStatus.AVAILABLE.name().equals(aunt.getAdminStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID,
                "阿姨当前管理状态不可抢单（已下架或禁用）");
        }
        if (!AuntAcceptStatus.AVAILABLE.name().equals(aunt.getAcceptStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID,
                "阿姨当前为休息状态，无法抢单，请先切换为可抢单");
        }

        ServiceOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (order.getStatus() != OrderStatus.PENDING_GRAB.getCode()) {
            throw new BusinessException(ResultCode.ORDER_ALREADY_GRABBED,
                "订单已被抢走或状态已变更");
        }

        // 档期预检（友好提示，最终保障靠唯一索引）
        checkSlotAvailable(aunt.getId(), order.getServiceDate(), order.getStartHour(), order.getDurationHours());

        // 阿姨姓名兜底：注册时 name 可能未填，用"阿姨{id}"兜底避免快照为 null
        String auntName = aunt.getName() != null ? aunt.getName() : "阿姨" + aunt.getId();

        // 条件更新订单归属（防并发：同一订单只能被一位阿姨抢到）
        int rows = orderMapper.update(null,
            new LambdaUpdateWrapper<ServiceOrder>()
                .set(ServiceOrder::getAuntId, aunt.getId())
                .set(ServiceOrder::getAuntName, auntName)
                .set(ServiceOrder::getAuntAvatar, aunt.getAvatar())
                .set(ServiceOrder::getStatus, OrderStatus.PENDING_SERVICE.getCode())
                .eq(ServiceOrder::getId, orderId)
                .eq(ServiceOrder::getStatus, OrderStatus.PENDING_GRAB.getCode())
                .isNull(ServiceOrder::getAuntId)
        );
        if (rows != 1) {
            throw new BusinessException(ResultCode.ORDER_ALREADY_GRABBED, "订单已被其他阿姨抢走");
        }

        // 插入档期占用记录（唯一索引冲突则回滚订单更新）
        insertSlots(aunt.getId(), orderId, order.getServiceDate(), order.getStartHour(), order.getDurationHours());
        log.info("抢单成功（auntUserId={}, auntId={}, orderNo={}）", auntUserId, aunt.getId(), order.getOrderNo());
    }

    /**
     * 阿姨订单列表（按 aunt.id 查，可按状态筛选）。
     */
    public PageResponse<OrderListItem> listForAunt(Long auntUserId, long page, long size, Integer status) {
        Aunt aunt = auntMapper.selectOne(
            new LambdaQueryWrapper<Aunt>().eq(Aunt::getUserId, auntUserId)
        );
        if (aunt == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "阿姨资料不存在，仅阿姨角色可查询");
        }
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        Page<ServiceOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ServiceOrder> wrapper = new LambdaQueryWrapper<ServiceOrder>()
            .eq(ServiceOrder::getAuntId, aunt.getId())
            .eq(status != null, ServiceOrder::getStatus, status)
            .orderByDesc(ServiceOrder::getCreateTime)
            .orderByDesc(ServiceOrder::getId);
        Page<ServiceOrder> result = orderMapper.selectPage(pageParam, wrapper);
        List<OrderListItem> items = result.getRecords().stream().map(this::toListItem).toList();
        return PageResponse.<OrderListItem>builder()
            .records(items)
            .total(result.getTotal())
            .page(result.getCurrent())
            .size(result.getSize())
            .build();
    }

    /**
     * 阿姨订单详情（校验订单 aunt_id 归属）。
     */
    public OrderDetail getDetailForAunt(Long auntUserId, Long orderId) {
        Aunt aunt = auntMapper.selectOne(
            new LambdaQueryWrapper<Aunt>().eq(Aunt::getUserId, auntUserId)
        );
        if (aunt == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "阿姨资料不存在，仅阿姨角色可查询");
        }
        ServiceOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (order.getAuntId() == null || !order.getAuntId().equals(aunt.getId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return toDetail(order);
    }

    // ===== 服务履约（阶段4）=====

    /**
     * 阿姨开始服务（待服务→服务中，规范 §5.4）。
     *
     * <p>条件更新（WHERE status=待服务 AND aunt_id=当前阿姨）防止与取消并发。
     */
    @Transactional(rollbackFor = Exception.class)
    public void start(Long auntUserId, Long orderId) {
        Aunt aunt = auntMapper.selectOne(
            new LambdaQueryWrapper<Aunt>().eq(Aunt::getUserId, auntUserId)
        );
        if (aunt == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "阿姨资料不存在，仅阿姨角色可操作");
        }
        int rows = orderMapper.update(null,
            new LambdaUpdateWrapper<ServiceOrder>()
                .set(ServiceOrder::getStatus, OrderStatus.IN_SERVICE.getCode())
                .eq(ServiceOrder::getId, orderId)
                .eq(ServiceOrder::getStatus, OrderStatus.PENDING_SERVICE.getCode())
                .eq(ServiceOrder::getAuntId, aunt.getId())
        );
        if (rows != 1) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID,
                "开始服务失败，订单状态可能已变更或非你接的单");
        }
        log.info("阿姨开始服务（auntUserId={}, orderId={}）", auntUserId, orderId);
    }

    /**
     * 阿姨提交服务完成（服务中→待确认，规范 §5.4、§9 阶段4）。
     *
     * <p>上传演示图片 URL，条件更新（WHERE status=服务中 AND aunt_id）防止并发。
     */
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long auntUserId, Long orderId, String imageUrl) {
        Aunt aunt = auntMapper.selectOne(
            new LambdaQueryWrapper<Aunt>().eq(Aunt::getUserId, auntUserId)
        );
        if (aunt == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "阿姨资料不存在，仅阿姨角色可操作");
        }
        int rows = orderMapper.update(null,
            new LambdaUpdateWrapper<ServiceOrder>()
                .set(ServiceOrder::getStatus, OrderStatus.PENDING_CONFIRM.getCode())
                .set(ServiceOrder::getCompleteImage, imageUrl)
                .eq(ServiceOrder::getId, orderId)
                .eq(ServiceOrder::getStatus, OrderStatus.IN_SERVICE.getCode())
                .eq(ServiceOrder::getAuntId, aunt.getId())
        );
        if (rows != 1) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID,
                "提交完成失败，订单状态可能已变更或非你接的单");
        }
        log.info("阿姨提交服务完成（auntUserId={}, orderId={}, imageUrl={}）", auntUserId, orderId, imageUrl);
    }

    /**
     * 用户确认服务完成（待确认→待评价，规范 §5.4）。
     *
     * <p>条件更新（WHERE status=待确认 AND user_id）防止并发。
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long userId, Long orderId) {
        int rows = orderMapper.update(null,
            new LambdaUpdateWrapper<ServiceOrder>()
                .set(ServiceOrder::getStatus, OrderStatus.PENDING_REVIEW.getCode())
                .eq(ServiceOrder::getId, orderId)
                .eq(ServiceOrder::getStatus, OrderStatus.PENDING_CONFIRM.getCode())
                .eq(ServiceOrder::getUserId, userId)
        );
        if (rows != 1) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID,
                "确认失败，订单状态可能已变更或非你的订单");
        }
        log.info("用户确认服务完成（userId={}, orderId={}）", userId, orderId);
    }

    // ===== 管理员端 =====

    /**
     * 管理员全量订单列表（可按状态筛选）。
     */
    public PageResponse<OrderListItem> listForAdmin(long page, long size, Integer status) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        Page<ServiceOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ServiceOrder> wrapper = new LambdaQueryWrapper<ServiceOrder>()
            .eq(status != null, ServiceOrder::getStatus, status)
            .orderByDesc(ServiceOrder::getCreateTime)
            .orderByDesc(ServiceOrder::getId);
        Page<ServiceOrder> result = orderMapper.selectPage(pageParam, wrapper);
        List<OrderListItem> items = result.getRecords().stream().map(this::toListItem).toList();
        return PageResponse.<OrderListItem>builder()
            .records(items)
            .total(result.getTotal())
            .page(result.getCurrent())
            .size(result.getSize())
            .build();
    }

    /**
     * 管理员指派阿姨（兜底机制，规范 §4 管理员职责、§7.3 事务）。
     *
     * <p>逻辑同抢单，但 auntId 由管理员指定，不校验接单状态（管理员强制指派兜底），
     * 仍校验管理状态=可用（下架/禁用的阿姨不应被指派）。
     *
     * @param orderId 订单 ID
     * @param auntId  被指派的阿姨 aunt.id
     */
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long orderId, Long auntId) {
        Aunt aunt = auntMapper.selectById(auntId);
        if (aunt == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "阿姨不存在");
        }
        if (!AuntAdminStatus.AVAILABLE.name().equals(aunt.getAdminStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID,
                "阿姨当前管理状态不可被指派（已下架或禁用）");
        }

        ServiceOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (order.getStatus() != OrderStatus.PENDING_GRAB.getCode()) {
            throw new BusinessException(ResultCode.ORDER_ALREADY_GRABBED,
                "订单已被抢走或状态已变更，无法指派");
        }

        checkSlotAvailable(aunt.getId(), order.getServiceDate(), order.getStartHour(), order.getDurationHours());

        // 阿姨姓名兜底：注册时 name 可能未填，用"阿姨{id}"兜底避免快照为 null
        String auntName = aunt.getName() != null ? aunt.getName() : "阿姨" + aunt.getId();

        int rows = orderMapper.update(null,
            new LambdaUpdateWrapper<ServiceOrder>()
                .set(ServiceOrder::getAuntId, aunt.getId())
                .set(ServiceOrder::getAuntName, auntName)
                .set(ServiceOrder::getAuntAvatar, aunt.getAvatar())
                .set(ServiceOrder::getStatus, OrderStatus.PENDING_SERVICE.getCode())
                .eq(ServiceOrder::getId, orderId)
                .eq(ServiceOrder::getStatus, OrderStatus.PENDING_GRAB.getCode())
                .isNull(ServiceOrder::getAuntId)
        );
        if (rows != 1) {
            throw new BusinessException(ResultCode.ORDER_ALREADY_GRABBED, "订单已被其他阿姨抢走，指派失败");
        }

        insertSlots(aunt.getId(), orderId, order.getServiceDate(), order.getStartHour(), order.getDurationHours());
        log.info("管理员指派阿姨成功（auntId={}, orderNo={}）", aunt.getId(), order.getOrderNo());
    }

    // ===== 档期辅助 =====

    /**
     * 档期预检：该阿姨在该日期的各小时块是否已被占用。
     */
    private void checkSlotAvailable(Long auntId, java.time.LocalDate serviceDate, int startHour, int durationHours) {
        List<Integer> hours = new ArrayList<>();
        for (int i = 0; i < durationHours; i++) {
            hours.add(startHour + i);
        }
        Long count = slotMapper.selectCount(
            new LambdaQueryWrapper<AuntBookingSlot>()
                .eq(AuntBookingSlot::getAuntId, auntId)
                .eq(AuntBookingSlot::getServiceDate, serviceDate)
                .in(AuntBookingSlot::getHourSlot, hours)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.AUNT_SLOT_CONFLICT,
                "该阿姨在该时段已被预约，请选择其他订单");
        }
    }

    /**
     * 循环插入档期占用记录（startHour ~ startHour+durationHours-1）。
     * 唯一索引冲突抛 AUNT_SLOT_CONFLICT，事务回滚订单更新。
     */
    private void insertSlots(Long auntId, Long orderId, java.time.LocalDate serviceDate, int startHour, int durationHours) {
        for (int i = 0; i < durationHours; i++) {
            AuntBookingSlot slot = new AuntBookingSlot();
            slot.setAuntId(auntId);
            slot.setOrderId(orderId);
            slot.setServiceDate(serviceDate);
            slot.setHourSlot(startHour + i);
            try {
                slotMapper.insert(slot);
            } catch (DuplicateKeyException e) {
                throw new BusinessException(ResultCode.AUNT_SLOT_CONFLICT,
                    "该阿姨在该时段已被预约，请选择其他订单");
            }
        }
    }

    // ===== 内部转换 =====

    private GrabListItem toGrabListItem(ServiceOrder order) {
        return GrabListItem.builder()
            .id(order.getId())
            .orderNo(order.getOrderNo())
            .serviceDate(order.getServiceDate())
            .startHour(order.getStartHour())
            .durationHours(order.getDurationHours())
            .address(order.getAddress())
            .amount(order.getAmount())
            .contactName(order.getContactName())
            .contactPhone(order.getContactPhone())
            .createTime(order.getCreateTime())
            .build();
    }

    private OrderListItem toListItem(ServiceOrder order) {
        return OrderListItem.builder()
            .id(order.getId())
            .orderNo(order.getOrderNo())
            .serviceDate(order.getServiceDate())
            .startHour(order.getStartHour())
            .durationHours(order.getDurationHours())
            .status(order.getStatus())
            .statusText(OrderStatus.fromCode(order.getStatus()).getMessage())
            .amount(order.getAmount())
            .auntName(order.getAuntName())
            .createTime(order.getCreateTime())
            .build();
    }

    private OrderDetail toDetail(ServiceOrder order) {
        return OrderDetail.builder()
            .id(order.getId())
            .orderNo(order.getOrderNo())
            .userId(order.getUserId())
            .auntId(order.getAuntId())
            .serviceDate(order.getServiceDate())
            .startHour(order.getStartHour())
            .durationHours(order.getDurationHours())
            .contactName(order.getContactName())
            .contactPhone(order.getContactPhone())
            .address(order.getAddress())
            .amount(order.getAmount())
            .auntName(order.getAuntName())
            .auntAvatar(order.getAuntAvatar())
            .status(order.getStatus())
            .statusText(OrderStatus.fromCode(order.getStatus()).getMessage())
            .payNo(order.getPayNo())
            .payTime(order.getPayTime())
            .payMethod(order.getPayMethod())
            .refundStatus(order.getRefundStatus())
            .refundNo(order.getRefundNo())
            .refundTime(order.getRefundTime())
            .cancelTime(order.getCancelTime())
            .completeImage(order.getCompleteImage())
            .createTime(order.getCreateTime())
            .updateTime(order.getUpdateTime())
            .build();
    }

    /**
     * 查订单并校验用户归属（用户端接口防越权）。
     */
    private ServiceOrder getOrderAndCheckOwnership(Long orderId, Long userId) {
        ServiceOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            // 不暴露存在性，统一提示不存在
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }
}
