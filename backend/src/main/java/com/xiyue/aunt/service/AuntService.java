package com.xiyue.aunt.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiyue.aunt.dto.*;
import com.xiyue.aunt.entity.Aunt;
import com.xiyue.aunt.enums.AuntAcceptStatus;
import com.xiyue.aunt.enums.AuntAdminStatus;
import com.xiyue.aunt.mapper.AuntMapper;
import com.xiyue.common.exception.BusinessException;
import com.xiyue.common.result.PageResponse;
import com.xiyue.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 阿姨资料服务。
 *
 * <p>职责划分：
 * <ul>
 *   <li>用户端：列表（仅 AVAILABLE，筛选+排序+分页）、详情（仅 AVAILABLE）；</li>
 *   <li>管理员端：全量列表、详情、编辑运营字段、上下架/禁用、逻辑删除；</li>
 *   <li>阿姨自己：按 user_id 更新接单状态。</li>
 * </ul>
 *
 * <p>状态分离（ADR-008）：admin_status（管理员控制）与 accept_status（阿姨自控）独立，
 * 用户端可见性只看 admin_status=AVAILABLE。
 *
 * <p>逻辑删除：aunt.deleted 字段 + MyBatis-Plus @TableLogic，查询自动过滤 deleted=1，
 * deleteById 自动改为 update deleted=1。存在历史订单禁止物理删除的检查在阶段3 service_order 表建立后补充。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuntService {

    private final AuntMapper auntMapper;

    // ===== 用户端 =====

    /**
     * 用户端阿姨列表：只显示 admin_status=AVAILABLE，支持筛选+排序+分页。
     *
     * @param page      页码（从 1 开始）
     * @param size      每页大小
     * @param minRating 最低星级（可空）
     * @param minPrice  最低价格（可空）
     * @param maxPrice  最高价格（可空）
     * @param skillTag  技能标签关键字（可空，模糊匹配）
     * @param sort      排序：price_asc 价格升序；默认/其他 按星级降序
     */
    public PageResponse<AuntListItem> listForUser(long page, long size,
                                                  BigDecimal minRating,
                                                  BigDecimal minPrice,
                                                  BigDecimal maxPrice,
                                                  String skillTag,
                                                  String sort) {
        Page<Aunt> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Aunt> wrapper = new LambdaQueryWrapper<Aunt>()
            .eq(Aunt::getAdminStatus, AuntAdminStatus.AVAILABLE.name())
            .ge(minRating != null, Aunt::getRating, minRating)
            .ge(minPrice != null, Aunt::getPrice, minPrice)
            .le(maxPrice != null, Aunt::getPrice, maxPrice)
            .like(StringUtils.hasText(skillTag), Aunt::getSkillTags, skillTag);
        if ("price_asc".equalsIgnoreCase(sort)) {
            wrapper.orderByAsc(Aunt::getPrice);
        } else {
            // 默认按星级降序
            wrapper.orderByDesc(Aunt::getRating);
        }
        Page<Aunt> result = auntMapper.selectPage(pageParam, wrapper);
        List<AuntListItem> items = result.getRecords().stream()
            .map(this::toListItem)
            .toList();
        return PageResponse.<AuntListItem>builder()
            .records(items)
            .total(result.getTotal())
            .page(result.getCurrent())
            .size(result.getSize())
            .build();
    }

    /**
     * 用户端阿姨详情：只返回 AVAILABLE 的阿姨，否则提示不存在/已下架。
     */
    public AuntDetail getDetailForUser(Long id) {
        Aunt aunt = auntMapper.selectOne(
            new LambdaQueryWrapper<Aunt>()
                .eq(Aunt::getId, id)
                .eq(Aunt::getAdminStatus, AuntAdminStatus.AVAILABLE.name())
        );
        if (aunt == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "阿姨不存在或已下架");
        }
        return toDetail(aunt);
    }

    // ===== 管理员端 =====

    /**
     * 管理员全量列表（含所有管理状态，@TableLogic 自动过滤已删除）。
     */
    public PageResponse<AuntListItem> listForAdmin(long page, long size) {
        Page<Aunt> pageParam = new Page<>(page, size);
        Page<Aunt> result = auntMapper.selectPage(pageParam,
            new LambdaQueryWrapper<Aunt>().orderByDesc(Aunt::getCreateTime));
        List<AuntListItem> items = result.getRecords().stream()
            .map(this::toListItem)
            .toList();
        return PageResponse.<AuntListItem>builder()
            .records(items)
            .total(result.getTotal())
            .page(result.getCurrent())
            .size(result.getSize())
            .build();
    }

    /**
     * 管理员阿姨详情（看所有管理状态）。
     */
    public AuntDetail getDetailForAdmin(Long id) {
        Aunt aunt = auntMapper.selectById(id);
        if (aunt == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "阿姨不存在");
        }
        return toDetail(aunt);
    }

    /**
     * 管理员编辑阿姨资料（仅运营字段：name/avatar/price/skillTags/intro）。
     * rating/serviceCount 由系统维护，admin_status/accept_status 通过独立接口更新。
     */
    public void updateByAdmin(Long id, AuntUpdateRequest req) {
        Aunt aunt = auntMapper.selectById(id);
        if (aunt == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "阿姨不存在");
        }
        if (req.getName() != null) {
            aunt.setName(req.getName());
        }
        if (req.getAvatar() != null) {
            aunt.setAvatar(req.getAvatar());
        }
        if (req.getPrice() != null) {
            aunt.setPrice(req.getPrice());
        }
        if (req.getSkillTags() != null) {
            aunt.setSkillTags(req.getSkillTags());
        }
        if (req.getIntro() != null) {
            aunt.setIntro(req.getIntro());
        }
        auntMapper.updateById(aunt);
        log.info("管理员编辑阿姨资料（id={}）", id);
    }

    /**
     * 管理员更新阿姨管理状态（上下架/禁用）。
     */
    public void updateAdminStatus(Long id, String adminStatus) {
        if (!AuntAdminStatus.isValid(adminStatus)) {
            throw new BusinessException(ResultCode.PARAM_INVALID,
                "管理状态不合法，仅允许 AVAILABLE / OFF_SHELF / DISABLED");
        }
        Aunt aunt = auntMapper.selectById(id);
        if (aunt == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "阿姨不存在");
        }
        aunt.setAdminStatus(adminStatus);
        auntMapper.updateById(aunt);
        log.info("管理员更新阿姨管理状态（id={}, status={}）", id, adminStatus);
    }

    /**
     * 管理员逻辑删除阿姨（@TableLogic 自动 update deleted=1）。
     *
     * <p>注：存在历史订单禁止物理删除的检查在阶段3 service_order 表建立后补充。
     * 当前逻辑删除不影响历史数据关联（仅标记 deleted=1，记录保留）。
     */
    public void deleteByAdmin(Long id) {
        Aunt aunt = auntMapper.selectById(id);
        if (aunt == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "阿姨不存在");
        }
        auntMapper.deleteById(id);
        log.info("管理员逻辑删除阿姨（id={}）", id);
    }

    // ===== 阿姨自己 =====

    /**
     * 阿姨自己更新接单状态（按 user_id 查 aunt 资料）。
     */
    public void updateMyAcceptStatus(Long userId, String acceptStatus) {
        if (!AuntAcceptStatus.isValid(acceptStatus)) {
            throw new BusinessException(ResultCode.PARAM_INVALID,
                "接单状态不合法，仅允许 AVAILABLE / RESTING");
        }
        Aunt aunt = auntMapper.selectOne(
            new LambdaQueryWrapper<Aunt>().eq(Aunt::getUserId, userId)
        );
        if (aunt == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "阿姨资料不存在，仅阿姨角色可操作");
        }
        aunt.setAcceptStatus(acceptStatus);
        auntMapper.updateById(aunt);
        log.info("阿姨更新接单状态（userId={}, status={}）", userId, acceptStatus);
    }

    // ===== 内部转换 =====

    private AuntListItem toListItem(Aunt aunt) {
        return AuntListItem.builder()
            .id(aunt.getId())
            .name(aunt.getName())
            .avatar(aunt.getAvatar())
            .price(aunt.getPrice())
            .rating(aunt.getRating())
            .serviceCount(aunt.getServiceCount())
            .skillTags(aunt.getSkillTags())
            .intro(aunt.getIntro())
            .build();
    }

    private AuntDetail toDetail(Aunt aunt) {
        return AuntDetail.builder()
            .id(aunt.getId())
            .name(aunt.getName())
            .avatar(aunt.getAvatar())
            .price(aunt.getPrice())
            .rating(aunt.getRating())
            .serviceCount(aunt.getServiceCount())
            .skillTags(aunt.getSkillTags())
            .intro(aunt.getIntro())
            .adminStatus(aunt.getAdminStatus())
            .acceptStatus(aunt.getAcceptStatus())
            .build();
    }
}
