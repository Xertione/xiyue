package com.xiyue.security;

import com.xiyue.common.exception.BusinessException;
import com.xiyue.common.result.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 安全上下文工具：从 SecurityContext 取当前登录用户。
 *
 * <p>供 Controller / Service 获取当前用户 ID，避免直接接触 SecurityContextHolder 细节。
 * 未登录或身份信息异常时抛出 {@link BusinessException}（401）。
 */
@Component
public class SecurityUserContext {

    /**
     * 获取当前登录用户主体。
     */
    public LoginUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof LoginUser loginUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录身份信息异常");
        }
        return loginUser;
    }

    /**
     * 获取当前登录用户 ID。
     */
    public Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }
}
