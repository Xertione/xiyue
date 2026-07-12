package com.xiyue.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建订单请求。
 *
 * <p>校验规则（规范 §5.1、§5.3）：
 * <ul>
 *   <li>serviceDate 必须是未来日期（@Future：明天及以后）；</li>
 *   <li>startHour 整点 0-23；durationHours >=1；</li>
 *   <li>endHour = startHour + durationHours 必须 <= 24（不跨天，Service 层校验）；</li>
 *   <li>contactPhone 11 位手机号；amount >= 0.01。</li>
 * </ul>
 */
@Data
public class CreateOrderRequest {

    /** 服务日期（未来日期） */
    @NotNull(message = "服务日期不能为空")
    @Future(message = "服务日期必须是未来日期")
    private LocalDate serviceDate;

    /** 开始小时（0-23 整点） */
    @NotNull(message = "开始时间不能为空")
    @Min(value = 0, message = "开始时间不能小于 0 点")
    @Max(value = 23, message = "开始时间不能大于 23 点")
    private Integer startHour;

    /** 服务时长（小时，>=1） */
    @NotNull(message = "服务时长不能为空")
    @Min(value = 1, message = "服务时长至少 1 小时")
    private Integer durationHours;

    /** 联系人姓名 */
    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    /** 联系电话（11 位手机号） */
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;

    /** 服务地址 */
    @NotBlank(message = "服务地址不能为空")
    private String address;

    /** 订单金额（价格快照，>= 0.01） */
    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额至少 0.01 元")
    private BigDecimal amount;
}
